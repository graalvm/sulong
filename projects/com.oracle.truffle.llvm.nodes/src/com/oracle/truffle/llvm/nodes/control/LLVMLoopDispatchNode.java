/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.llvm.nodes.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.func.LLVMInvokeNode;
import com.oracle.truffle.llvm.runtime.except.LLVMUserException;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;

public final class LLVMLoopDispatchNode extends LLVMExpressionNode {

    private final FrameSlot exceptionValueSlot;
    private final int headerId;
    @Children private final LLVMStatementNode[] bodyNodes;
    @CompilationFinal(dimensions = 1) private final int[] indexMapping;
    @CompilationFinal(dimensions = 2) private final FrameSlot[][] beforeBlockNuller;
    @CompilationFinal(dimensions = 2) private final FrameSlot[][] afterBlockNuller;
    @CompilationFinal(dimensions = 1) private final int[] loopSuccessors;
    @CompilationFinal private final FrameSlot successorSlot;

    public LLVMLoopDispatchNode(FrameSlot exceptionValueSlot, LLVMStatementNode[] bodyNodes, FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller, int headerId, int[] indexMapping, int[] successors, FrameSlot successorSlot) {
        this.exceptionValueSlot = exceptionValueSlot;
        this.bodyNodes = bodyNodes;
        this.beforeBlockNuller = beforeBlockNuller;
        this.afterBlockNuller = afterBlockNuller;
        this.indexMapping = indexMapping;
        this.headerId = headerId;
        this.loopSuccessors = successors;
        this.successorSlot = successorSlot;
    }

    @ExplodeLoop
    private boolean isInLoop(int bci) {
        for(int i : loopSuccessors) {
            if(i == bci) {
                return false;
            }
        }
        return true;
    }

    @Override
    @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
    public Object executeGeneric(VirtualFrame frame) {
        // helper variable to not return at first encounter of headerId
        boolean firstIteration = true;

        CompilerAsserts.compilationConstant(bodyNodes.length);
        int basicBlockIndex = headerId;

        // do-while loop fails at PE
        outer: while (isInLoop(basicBlockIndex) && (basicBlockIndex != headerId || firstIteration)) {
            CompilerAsserts.partialEvaluationConstant(basicBlockIndex);

            firstIteration = false;

            if(bodyNodes[indexMapping[basicBlockIndex]] instanceof LLVMLoopNode) {
                LLVMLoopNode loop = (LLVMLoopNode) bodyNodes[indexMapping[basicBlockIndex]];
                loop.execute(frame);
                int successorBasicBlockIndex = FrameUtil.getIntSafe(frame, successorSlot);

                int[] successors = loop.getSuccessors();
                for(int i = 0; i < successors.length-1; i++) {
                    if(successorBasicBlockIndex == successors[i]) {
                        basicBlockIndex = successors[i];
                        continue outer;
                    }
                }

                int i = successors.length - 1;
                assert successors[i] == successorBasicBlockIndex : "Could not find loop successor";
                basicBlockIndex = successors[i];

                continue outer;
            }

            assert(bodyNodes[indexMapping[basicBlockIndex]] instanceof LLVMBasicBlockNode);
            LLVMBasicBlockNode bb = (LLVMBasicBlockNode) bodyNodes[indexMapping[basicBlockIndex]];

            // execute all statements
            bb.execute(frame);

            // execute control flow node, write phis, null stack frame slots, and dispatch to
            // the correct successor block
            LLVMControlFlowNode controlFlowNode = bb.termInstruction;
            if (controlFlowNode instanceof LLVMConditionalBranchNode) {
                LLVMConditionalBranchNode conditionalBranchNode = (LLVMConditionalBranchNode) controlFlowNode;
                boolean condition = conditionalBranchNode.executeCondition(frame);
                if (CompilerDirectives.injectBranchProbability(bb.getBranchProbability(LLVMConditionalBranchNode.TRUE_SUCCESSOR), condition)) {
                    if (CompilerDirectives.inInterpreter()) {
                        bb.increaseBranchProbability(LLVMConditionalBranchNode.TRUE_SUCCESSOR);
                    }
                    LLVMDispatchBasicBlockNode.executePhis(frame, conditionalBranchNode, LLVMConditionalBranchNode.TRUE_SUCCESSOR);
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                    basicBlockIndex = conditionalBranchNode.getTrueSuccessor();
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                    continue outer;
                } else {
                    if (CompilerDirectives.inInterpreter()) {
                        bb.increaseBranchProbability(LLVMConditionalBranchNode.FALSE_SUCCESSOR);
                    }
                    LLVMDispatchBasicBlockNode.executePhis(frame, conditionalBranchNode, LLVMConditionalBranchNode.FALSE_SUCCESSOR);
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                    basicBlockIndex = conditionalBranchNode.getFalseSuccessor();
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                    continue outer;
                }
            } else if (controlFlowNode instanceof LLVMSwitchNode) {
                LLVMSwitchNode switchNode = (LLVMSwitchNode) controlFlowNode;
                Object condition = switchNode.executeCondition(frame);
                int[] successors = switchNode.getSuccessors();
                for (int i = 0; i < successors.length - 1; i++) {
                    Object caseValue = switchNode.getCase(i).executeGeneric(frame);
                    assert caseValue.getClass() == condition.getClass() : "must be the same type - otherwise equals might wrongly return false";
                    if (CompilerDirectives.injectBranchProbability(bb.getBranchProbability(i), condition.equals(caseValue))) {
                        if (CompilerDirectives.inInterpreter()) {
                            bb.increaseBranchProbability(i);
                        }
                        LLVMDispatchBasicBlockNode.executePhis(frame, switchNode, i);
                        LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                        basicBlockIndex = successors[i];
                        LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                        continue outer;
                    }
                }

                int i = successors.length - 1;
                if (CompilerDirectives.inInterpreter()) {
                    bb.increaseBranchProbability(i);
                }
                LLVMDispatchBasicBlockNode.executePhis(frame, switchNode, i);
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                basicBlockIndex = successors[i];
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                continue outer;
            } else if (controlFlowNode instanceof LLVMIndirectBranchNode) {
                // TODO (chaeubl): we need a different approach here - this is awfully
                // inefficient (see GR-3664)
                LLVMIndirectBranchNode indirectBranchNode = (LLVMIndirectBranchNode) controlFlowNode;
                int[] successors = indirectBranchNode.getSuccessors();
                int successorBasicBlockIndex = indirectBranchNode.executeCondition(frame);
                for (int i = 0; i < successors.length - 1; i++) {
                    if (CompilerDirectives.injectBranchProbability(bb.getBranchProbability(i), successors[i] == successorBasicBlockIndex)) {
                        if (CompilerDirectives.inInterpreter()) {
                            bb.increaseBranchProbability(i);
                        }
                        LLVMDispatchBasicBlockNode.executePhis(frame, indirectBranchNode, i);
                        LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                        basicBlockIndex = successors[i];
                        LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                        continue outer;
                    }
                }

                int i = successors.length - 1;
                assert successorBasicBlockIndex == successors[i];
                if (CompilerDirectives.inInterpreter()) {
                    bb.increaseBranchProbability(i);
                }
                LLVMDispatchBasicBlockNode.executePhis(frame, indirectBranchNode, i);
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                basicBlockIndex = successors[i];
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                continue outer;
            } else if (controlFlowNode instanceof LLVMBrUnconditionalNode) {
                LLVMBrUnconditionalNode unconditionalNode = (LLVMBrUnconditionalNode) controlFlowNode;
                unconditionalNode.execute(frame); // required for instrumentation
                LLVMDispatchBasicBlockNode.executePhis(frame, unconditionalNode, 0);
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                basicBlockIndex = unconditionalNode.getSuccessor();
                LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                continue outer;
            } else if (controlFlowNode instanceof LLVMInvokeNode) {
                LLVMInvokeNode invokeNode = (LLVMInvokeNode) controlFlowNode;
                try {
                    invokeNode.execute(frame);
                    LLVMDispatchBasicBlockNode.executePhis(frame, invokeNode, LLVMInvokeNode.NORMAL_SUCCESSOR);
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                    basicBlockIndex = invokeNode.getNormalSuccessor();
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                    continue outer;
                } catch (LLVMUserException e) {
                    frame.setObject(exceptionValueSlot, e);
                    LLVMDispatchBasicBlockNode.executePhis(frame, invokeNode, LLVMInvokeNode.UNWIND_SUCCESSOR);
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, afterBlockNuller);
                    basicBlockIndex = invokeNode.getUnwindSuccessor();
                    LLVMDispatchBasicBlockNode.nullDeadSlots(frame, basicBlockIndex, beforeBlockNuller);
                    continue outer;
                }
            } else {    // some control flow nodes should be never part of a loop
                CompilerAsserts.neverPartOfCompilation();
                throw new UnsupportedOperationException("unexpected controlFlowNode type: " + controlFlowNode);
            }
        }

        if(basicBlockIndex != headerId) {
            frame.setInt(successorSlot, basicBlockIndex);
            return false;
        }

        return true;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.RootTag.class;
    }
}
