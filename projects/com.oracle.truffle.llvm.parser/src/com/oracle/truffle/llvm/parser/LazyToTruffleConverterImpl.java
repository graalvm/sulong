/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.LLVMLivenessAnalysis.LLVMLivenessAnalysisResult;
import com.oracle.truffle.llvm.parser.LLVMPhiManager.Phi;
import com.oracle.truffle.llvm.parser.metadata.debuginfo.DebugInfoFunctionProcessor;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.Kind;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.KnownAttribute;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.functions.LazyFunctionParser;
import com.oracle.truffle.llvm.parser.nodes.LLVMSymbolReadResolver;
import com.oracle.truffle.llvm.runtime.GetStackSpaceFactory;
import com.oracle.truffle.llvm.parser.util.LLVMControlFlowGraph;
import com.oracle.truffle.llvm.parser.util.LLVMControlFlowGraph.CFGBlock;
import com.oracle.truffle.llvm.parser.util.LLVMControlFlowGraph.CFGLoop;
import com.oracle.truffle.llvm.runtime.except.LLVMUserException;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor.LazyToTruffleConverter;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceFunctionType;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.UniquesRegion;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

public class LazyToTruffleConverterImpl implements LazyToTruffleConverter {
    private final LLVMParserRuntime runtime;
    private final FunctionDefinition method;
    private final Source source;
    private final LazyFunctionParser parser;
    private final DebugInfoFunctionProcessor diProcessor;

    private static final String LOOP_SUCCESSOR_FRAME_ID = "<loop successor>";

    LazyToTruffleConverterImpl(LLVMParserRuntime runtime, FunctionDefinition method, Source source, LazyFunctionParser parser,
                    DebugInfoFunctionProcessor diProcessor) {
        this.runtime = runtime;
        this.method = method;
        this.source = source;
        this.parser = parser;
        this.diProcessor = diProcessor;
    }

    @Override
    public RootCallTarget convert() {
        CompilerAsserts.neverPartOfCompilation();

        // parse the function block
        parser.parse(diProcessor, source, runtime);

        // prepare the phis
        final Map<InstructionBlock, List<Phi>> phis = LLVMPhiManager.getPhis(method);

        // setup the frameDescriptor
        final FrameDescriptor frame = StackManager.createFrame(method);

        // setup the uniquesRegion
        UniquesRegion uniquesRegion = new UniquesRegion();
        GetStackSpaceFactory getStackSpaceFactory = GetStackSpaceFactory.createGetUniqueStackSpaceFactory(uniquesRegion);

        LLVMLivenessAnalysisResult liveness = LLVMLivenessAnalysis.computeLiveness(frame, runtime.getContext(), phis, method);
        LLVMSymbolReadResolver symbols = new LLVMSymbolReadResolver(runtime, frame, getStackSpaceFactory);

        List<FrameSlot> notNullable = new ArrayList<>();

        LLVMRuntimeDebugInformation dbgInfoHandler = new LLVMRuntimeDebugInformation(frame, runtime.getContext(), notNullable, symbols);
        dbgInfoHandler.registerStaticDebugSymbols(method);

        LLVMBitcodeFunctionVisitor visitor = new LLVMBitcodeFunctionVisitor(runtime.getContext(), runtime.getLibrary(), frame, uniquesRegion, phis, method.getParameters().size(), symbols, method,
                        liveness, notNullable, dbgInfoHandler);
        method.accept(visitor);

        LLVMControlFlowGraph cfg = new LLVMControlFlowGraph(method.getBlocks().toArray(FunctionDefinition.EMPTY));
        cfg.build();

        FrameSlot[][] nullableBeforeBlock = getNullableFrameSlots(frame, liveness.getNullableBeforeBlock(), notNullable);
        FrameSlot[][] nullableAfterBlock = getNullableFrameSlots(frame, liveness.getNullableAfterBlock(), notNullable);
        LLVMSourceLocation location = method.getLexicalScope();

        List<LLVMStatementNode> copyArgumentsToFrame = copyArgumentsToFrame(frame);
        LLVMStatementNode[] copyArgumentsToFrameArray = copyArgumentsToFrame.toArray(new LLVMStatementNode[copyArgumentsToFrame.size()]);

        List<LLVMStatementNode> nodes = visitor.getBlocks();
        FrameSlot loopSuccessorSlot = null;

        if (cfg.isReducible() && cfg.getCFGLoops().size() > 0) {
            loopSuccessorSlot = frame.addFrameSlot(LOOP_SUCCESSOR_FRAME_ID, FrameSlotKind.Int);

            nodes = resolveLoops(nodes, cfg, frame, nullableBeforeBlock, nullableAfterBlock, loopSuccessorSlot);
        }

        LLVMExpressionNode body = runtime.getContext().getNodeFactory().createFunctionBlockNode(frame.findFrameSlot(LLVMUserException.FRAME_SLOT_ID), nodes, uniquesRegion.build(), nullableBeforeBlock,
                        nullableAfterBlock, location, copyArgumentsToFrameArray, loopSuccessorSlot);

        RootNode rootNode = runtime.getContext().getNodeFactory().createFunctionStartNode(body, frame, method.getName(), method.getSourceName(),
                        method.getParameters().size(), source, location);

        method.onAfterParse();

        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    private List<LLVMStatementNode> resolveLoops(List<LLVMStatementNode> nodes, LLVMControlFlowGraph cfg, FrameDescriptor frame, FrameSlot[][] nullableBeforeBlock,
                    FrameSlot[][] nullableAfterBlock, FrameSlot loopSuccessorSlot) {

        List<LLVMStatementNode> resolvedNodes = new ArrayList<>(nodes);

        for (CFGLoop loop : cfg.getCFGLoops()) {

            int headerId = loop.getHeader().id;

            int[] indexMapping = new int[resolvedNodes.size()];
            Arrays.fill(indexMapping, -1);

            List<LLVMStatementNode> bodyNodes = new ArrayList<>();

            // add header to body nodes
            bodyNodes.add(resolvedNodes.get(headerId));
            indexMapping[headerId] = 0;

            // add body nodes
            int i = 1;
            for (CFGBlock block : loop.getBody()) {
                bodyNodes.add(resolvedNodes.get(block.id));
                indexMapping[block.id] = i++;
            }

            int[] loopSuccessors = loop.getSuccessorIDs();

            LLVMExpressionNode loopBody = runtime.getContext().getNodeFactory().createLoopDispatchNode(frame.findFrameSlot(LLVMUserException.FRAME_SLOT_ID), Collections.unmodifiableList(bodyNodes),
                            nullableBeforeBlock, nullableAfterBlock, headerId, indexMapping, loopSuccessors, loopSuccessorSlot);

            LLVMControlFlowNode loopNode = runtime.getContext().getNodeFactory().createLoop(loopBody, loopSuccessors);

            // replace header block with loop node
            resolvedNodes.set(headerId, runtime.getContext().getNodeFactory().createBasicBlockNode(new LLVMStatementNode[0], loopNode, headerId, ("loopAt" + headerId)));

            // remove inner loops to reduce number of nodes
            for (CFGLoop innerLoop : loop.getInnerLoops()) {
                resolvedNodes.set(innerLoop.getHeader().id, null);
            }
        }

        return resolvedNodes;
    }

    @Override
    public LLVMSourceFunctionType getSourceType() {
        return method.getSourceFunction().getSourceType();
    }

    private static FrameSlot[][] getNullableFrameSlots(FrameDescriptor frame, BitSet[] nullablePerBlock, List<FrameSlot> notNullable) {
        List<? extends FrameSlot> frameSlots = frame.getSlots();
        FrameSlot[][] result = new FrameSlot[nullablePerBlock.length][];

        for (int i = 0; i < nullablePerBlock.length; i++) {
            BitSet nullable = nullablePerBlock[i];
            int bitIndex = -1;

            ArrayList<FrameSlot> nullableSlots = new ArrayList<>();
            while ((bitIndex = nullable.nextSetBit(bitIndex + 1)) >= 0) {
                FrameSlot frameSlot = frameSlots.get(bitIndex);
                if (!notNullable.contains(frameSlot)) {
                    nullableSlots.add(frameSlot);
                }
            }
            if (nullableSlots.size() > 0) {
                result[i] = nullableSlots.toArray(new FrameSlot[nullableSlots.size()]);
            } else {
                assert result[i] == null;
            }
        }
        return result;
    }

    private List<LLVMStatementNode> copyArgumentsToFrame(FrameDescriptor frame) {
        List<FunctionParameter> parameters = method.getParameters();
        List<LLVMStatementNode> formalParamInits = new ArrayList<>();
        LLVMExpressionNode stackPointerNode = runtime.getContext().getNodeFactory().createFunctionArgNode(0, PrimitiveType.I64);
        formalParamInits.add(runtime.getContext().getNodeFactory().createFrameWrite(PointerType.VOID, stackPointerNode, frame.findFrameSlot(LLVMStack.FRAME_ID), null));

        int argIndex = 1;
        if (method.getType().getReturnType() instanceof StructureType) {
            argIndex++;
        }
        for (FunctionParameter parameter : parameters) {
            LLVMExpressionNode parameterNode = runtime.getContext().getNodeFactory().createFunctionArgNode(argIndex++, parameter.getType());
            FrameSlot slot = frame.findFrameSlot(parameter.getName());
            if (isStructByValue(parameter)) {
                Type type = ((PointerType) parameter.getType()).getPointeeType();
                formalParamInits.add(
                                runtime.getContext().getNodeFactory().createFrameWrite(parameter.getType(),
                                                runtime.getContext().getNodeFactory().createCopyStructByValue(type, GetStackSpaceFactory.createAllocaFactory(), parameterNode), slot, null));
            } else {
                formalParamInits.add(runtime.getContext().getNodeFactory().createFrameWrite(parameter.getType(), parameterNode, slot, null));
            }
        }
        return formalParamInits;
    }

    private static boolean isStructByValue(FunctionParameter parameter) {
        if (parameter.getType() instanceof PointerType && parameter.getParameterAttribute() != null) {
            for (Attribute a : parameter.getParameterAttribute().getAttributes()) {
                if (a instanceof KnownAttribute && ((KnownAttribute) a).getAttr() == Kind.BYVAL) {
                    return true;
                }
            }
        }
        return false;
    }
}
