/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.frontend.bc;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.nodes.base.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.base.LLVMNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMAddressNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMContext;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMLanguage;
import com.oracle.truffle.llvm.nodes.impl.func.LLVMCallNode;
import com.oracle.truffle.llvm.nodes.impl.func.LLVMFunctionStartNode;
import com.oracle.truffle.llvm.nodes.impl.intrinsics.c.LLVMFreeFactory;
import com.oracle.truffle.llvm.nodes.impl.intrinsics.llvm.LLVMMemCopyFactory.LLVMMemI32CopyFactory;
import com.oracle.truffle.llvm.nodes.impl.literals.LLVMSimpleLiteralNode.LLVMAddressLiteralNode;
import com.oracle.truffle.llvm.nodes.impl.literals.LLVMSimpleLiteralNode.LLVMI1LiteralNode;
import com.oracle.truffle.llvm.nodes.impl.literals.LLVMSimpleLiteralNode.LLVMI32LiteralNode;
import com.oracle.truffle.llvm.nodes.impl.others.LLVMStaticInitsBlockNode;
import com.oracle.truffle.llvm.parser.LLVMBaseType;
import com.oracle.truffle.llvm.parser.LLVMParserResult;
import com.oracle.truffle.llvm.parser.factories.LLVMBlockFactory;
import com.oracle.truffle.llvm.parser.factories.LLVMFrameReadWriteFactory;
import com.oracle.truffle.llvm.parser.factories.LLVMFunctionFactory;
import com.oracle.truffle.llvm.parser.factories.LLVMMemoryReadWriteFactory;
import com.oracle.truffle.llvm.parser.factories.LLVMRootNodeFactory;
import com.oracle.truffle.llvm.runtime.LLVMOptimizationConfiguration;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor.LLVMRuntimeType;
import com.oracle.truffle.llvm.types.memory.LLVMHeap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.man.cs.llvm.ir.LLVMParser;
import uk.ac.man.cs.llvm.ir.model.*;
import uk.ac.man.cs.llvm.ir.module.ModuleVersion;
import uk.ac.man.cs.llvm.ir.types.PointerType;
import uk.ac.man.cs.llvm.ir.types.Type;

public class LLVMBitcodeVisitor implements ModelVisitor {

    public static LLVMParserResult getMain(Source source, LLVMContext context, LLVMOptimizationConfiguration configuration) {
         Model model = new Model();

        new LLVMParser(model).parse(ModuleVersion.LLVM_3_4, source.getPath());

        LLVMPhiManager phis = LLVMPhiManager.generate(model);

        LLVMFrameDescriptors lifetimes = LLVMFrameDescriptors.generate(model);

        LLVMLabelList labels = LLVMLabelList.generate(model);

        LLVMBitcodeVisitor module = new LLVMBitcodeVisitor(context, configuration, lifetimes, labels, phis);

        model.accept(module);

        LLVMFunctionDescriptor mainFunction = module.getFunction("@main");
        FrameDescriptor frame = module.frames.getDescriptor();

        FrameSlot stack = frame.findFrameSlot(LLVMBitcodeHelper.STACK_ADDRESS_FRAME_SLOT_ID);

        LLVMNode[] globals = module.getGobalVariables(stack).toArray(new LLVMNode[0]);

        RootNode staticInits = new LLVMStaticInitsBlockNode(globals, frame, context, stack);
        RootCallTarget staticInitsTarget = Truffle.getRuntime().createCallTarget(staticInits);
        LLVMNode[] deallocs = module.getDeallocations();
        RootNode staticDestructors = new LLVMStaticInitsBlockNode(deallocs, frame, context, stack);
        RootCallTarget staticDestructorsTarget = Truffle.getRuntime().createCallTarget(staticDestructors);
        if (mainFunction == null) {
            return new LLVMBitcodeParserResult(Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(stack)), staticInitsTarget, staticDestructorsTarget, module.getFunctions());
        }
        RootCallTarget mainCallTarget = module.getFunctions().get(mainFunction);
        RootNode globalFunction = LLVMRootNodeFactory.createGlobalRootNode(context, stack, frame, mainCallTarget, context.getMainArguments(), source, mainFunction.getParameterTypes());
        RootCallTarget globalFunctionRoot = Truffle.getRuntime().createCallTarget(globalFunction);
        RootNode globalRootNode = LLVMFunctionFactory.createGlobalRootNodeWrapping(globalFunctionRoot, mainFunction.getReturnType());
        RootCallTarget wrappedCallTarget = Truffle.getRuntime().createCallTarget(globalRootNode);
        return new LLVMBitcodeParserResult(wrappedCallTarget, staticInitsTarget, staticDestructorsTarget, module.getFunctions());
    }

    private final LLVMContext context;

    private final LLVMOptimizationConfiguration optimizationConfiguration;

    private final LLVMFrameDescriptors frames;

    private final LLVMLabelList labels;

    private final LLVMPhiManager phis;

    private final List<LLVMNode> deallocations = new ArrayList<>();

    private final Map<LLVMFunctionDescriptor, RootCallTarget> functions = new HashMap<>();

    private final Map<GlobalValueSymbol, LLVMAddressNode> variables = new HashMap<>();

    public LLVMBitcodeVisitor(LLVMContext context, LLVMOptimizationConfiguration optimizationConfiguration, LLVMFrameDescriptors frames, LLVMLabelList labels, LLVMPhiManager phis) {
        this.context = context;
        this.optimizationConfiguration = optimizationConfiguration;
        this.frames = frames;
        this.labels = labels;
        this.phis = phis;
    }

    private LLVMExpressionNode createFunction(FunctionDefinition method) {
        String name = method.getName();

        LLVMBitcodeFunctionVisitor visitor = new LLVMBitcodeFunctionVisitor(
                this,
                frames.getDescriptor(name),
                frames.getSlots(name),
                labels.labels(name),
                phis.getPhiMap(name));

        method.accept(visitor);

        return LLVMBlockFactory.createFunctionBlock(
                visitor.getReturnSlot(),
                visitor.getBlocks(),
                visitor.getNullers());
    }

    private List<LLVMNode> createParameters(FrameDescriptor frame, List<FunctionParameter> parameters) {
        List<LLVMNode> parameterNodes = new ArrayList<>();

        LLVMExpressionNode stack = LLVMFunctionFactory.createFunctionArgNode(0, LLVMBaseType.ADDRESS);
        parameterNodes.add(LLVMFrameReadWriteFactory.createFrameWrite(LLVMBaseType.ADDRESS, stack, frame.findFrameSlot(LLVMBitcodeHelper.STACK_ADDRESS_FRAME_SLOT_ID)));

        int argIndex = LLVMCallNode.ARG_START_INDEX;
//        if (resolve(functionHeader.getRettype()).isStruct()) {
//            LLVMExpressionNode functionRetParNode = LLVMFunctionFactory.createFunctionArgNode(argIndex, paramType)e(argIndex++, LLVMBaseType.STRUCT);
//            LLVMNode retValue = createAssignment((String) retSlot.getIdentifier(), functionRetParNode, functionHeader.getRettype());
//            formalParamInits.add(retValue);
//        }
        for (FunctionParameter parameter : parameters) {
            LLVMBaseType llvmtype = LLVMBitcodeHelper.toBaseType(parameter.getType());
            LLVMExpressionNode parameterNode = LLVMFunctionFactory.createFunctionArgNode(argIndex++, llvmtype);
            FrameSlot slot = frame.findFrameSlot(parameter.getName());
            parameterNodes.add(LLVMFrameReadWriteFactory.createFrameWrite(llvmtype, parameterNode, slot));
        }
        return parameterNodes;
    }

    private LLVMNode createVariable(GlobalValueSymbol global, FrameSlot stack) {
        if (global == null || global.getValue() == null) {
            return null;
        } else {
            LLVMExpressionNode constant = LLVMBitcodeHelper.toConstantNode(global.getValue(), global.getAlign(), this::getGlobalVariable, context, stack);
            if (constant != null) {
                Type type = ((PointerType) global.getType()).getPointeeType();
                LLVMBaseType baseType = LLVMBitcodeHelper.toBaseType(type);
                int size = LLVMBitcodeHelper.getSize(type, global.getAlign());

                LLVMAddressLiteralNode globalVarAddress = (LLVMAddressLiteralNode) getGlobalVariable(global);

                if (size == 0) {
                    return null;
                } else {
                    LLVMNode store;
                    if (baseType == LLVMBaseType.ARRAY || baseType == LLVMBaseType.STRUCT) {
                        store = LLVMMemI32CopyFactory.create(globalVarAddress, (LLVMAddressNode) constant, new LLVMI32LiteralNode(size), new LLVMI32LiteralNode(0), new LLVMI1LiteralNode(false));
                    } else {
                        Type t = global.getValue().getType();
                        store = LLVMMemoryReadWriteFactory.createStore(globalVarAddress, constant, LLVMBitcodeHelper.toBaseType(t), LLVMBitcodeHelper.getSize(t, 0));
                    }
                    return store;
                }
            } else {
                return null;
            }
        }
    }

    public LLVMContext getContext() {
        return context;
    }

    public LLVMNode[] getDeallocations() {
        return deallocations.toArray(new LLVMNode[deallocations.size()]);
    }

    public LLVMFunctionDescriptor getFunction(String name) {
        for (LLVMFunctionDescriptor function : functions.keySet()) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }

    public Map<LLVMFunctionDescriptor, RootCallTarget> getFunctions() {
        return functions;
    }

    public LLVMExpressionNode getGlobalVariable(GlobalValueSymbol global) {
        LLVMAddressNode address = variables.get(global);
        if (address == null) {
            Type type = ((PointerType) global.getType()).getPointeeType();

            address = new LLVMAddressLiteralNode(LLVMHeap.allocateMemory(LLVMBitcodeHelper.getSize(type, global.getAlign())));
            deallocations.add(LLVMFreeFactory.create(address));
            variables.put(global, address);
        }
        return address;
    }

    public LLVMOptimizationConfiguration getOptimizationConfiguration() {
        return optimizationConfiguration;
    }

    public List<LLVMNode> getGobalVariables(FrameSlot stack) {
        List<LLVMNode> globals = new ArrayList<>();
        for (GlobalValueSymbol global : variables.keySet()) {
            LLVMNode store = createVariable(global, stack);
            if (store != null) {
                globals.add(store);
            }
        }
        return globals;
    }

    @Override
    public void visit(GlobalConstant constant) {
        variables.put(constant, null);
    }

    @Override
    public void visit(GlobalVariable variable) {
        variables.put(variable, null);
    }

    @Override
    public void visit(FunctionDeclaration method) {
    }

    @Override
    public void visit(FunctionDefinition method) {
        FrameDescriptor frame = frames.getDescriptor(method.getName());

        List<LLVMNode> parameters = createParameters(frame, method.getParameters());

        LLVMExpressionNode body = createFunction(method);

        LLVMNode[] beforeFunction = parameters.toArray(new LLVMNode[parameters.size()]);
        LLVMNode[] afterFunction = new LLVMNode[0];

        LLVMFunctionStartNode rootNode = new LLVMFunctionStartNode(body, beforeFunction, afterFunction, frame, method.getName());
        LLVMRuntimeType llvmReturnType = LLVMBitcodeHelper.toRuntimeType(method.base());
        LLVMRuntimeType[] llvmParamTypes = LLVMBitcodeHelper.toRuntimeTypes(method.args());
        LLVMFunctionDescriptor function = context.getFunctionRegistry().createFunctionDescriptor(method.getName(), llvmReturnType, llvmParamTypes, method.isVarArg());
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        functions.put(function, callTarget);
    }

    @Override
    public void visit(Type type) {
    }
}
