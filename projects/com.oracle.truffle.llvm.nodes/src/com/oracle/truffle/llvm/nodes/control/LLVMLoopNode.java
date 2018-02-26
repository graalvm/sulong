/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.llvm.nodes.control;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.llvm.nodes.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.base.LLVMFrameNullerUtil;
import com.oracle.truffle.llvm.nodes.func.LLVMResumeNode;
import com.oracle.truffle.llvm.nodes.others.LLVMUnreachableNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public class LLVMLoopNode extends LLVMExpressionNode{

    @Child private LoopNode loop;

    private int loopHeaderId;

    private LLVMLoopNode(LLVMExpressionNode[] blocks, FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller) {    //TODO more specific types
        loop = Truffle.getRuntime().createLoopNode(new LLVMRepeatingNode(blocks, beforeBlockNuller, afterBlockNuller));
        this.loopHeaderId = (blocks[0] instanceof LLVMBasicBlockNode) ? ((LLVMBasicBlockNode)blocks[0]).getBlockId() : ((LLVMLoopNode)blocks[0]).getLoopHeaderId();
    }

    public int getLoopHeaderId() {
        return loopHeaderId;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        throw new UnsupportedOperationException("Must not be called.");
    }

    public int executeLoop(VirtualFrame frame) {
        try {
            loop.executeLoop(frame);
        }catch(LLVMBreakException e) {
            return e.getNextBlock();
        }
        CompilerAsserts.neverPartOfCompilation();
        throw new IllegalStateException("must not reach here"); // control flow by exception
    }

    private static class LLVMRepeatingNode extends Node implements RepeatingNode {

        @Children private final LLVMExpressionNode[] nodes;

        @CompilationFinal(dimensions = 2) private final FrameSlot[][] beforeBlockNuller;
        @CompilationFinal(dimensions = 2) private final FrameSlot[][] afterBlockNuller;

        private final int[] nodeIds;

        public LLVMRepeatingNode(LLVMExpressionNode[] basicBlocks, FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller) {
            this.nodes = Arrays.copyOf(basicBlocks, basicBlocks.length);
            this.beforeBlockNuller = beforeBlockNuller;
            this.afterBlockNuller = afterBlockNuller;

            this.nodeIds = new int[nodes.length];
            for(int i = 0; i < nodeIds.length; i++) {
                LLVMExpressionNode node = nodes[i];

                if (node instanceof LLVMBasicBlockNode) {
                    nodeIds[i] = ((LLVMBasicBlockNode)node).getBlockId();
                }else {
                    assert(node instanceof LLVMLoopNode);
                    nodeIds[i] = ((LLVMLoopNode)node).getLoopHeaderId();
                }
            }
        }

        @Override
        @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
        public boolean executeRepeating(VirtualFrame frame) {
            int nextBlock;
            LLVMExpressionNode header = nodes[0];
            nextBlock = executeNode(header, frame);
            if(isInLoop(nextBlock)) {  // enter loop?
                // execute blocks until loop body is repeated (check successor block)
                LLVMExpressionNode block = nodes[getIndexOfNode(nextBlock)];
                do {
                    nextBlock = executeNode(block, frame);
                    if (!isInLoop(nextBlock)) {
                        throw new LLVMBreakException(nextBlock);
                    }
                    block = nodes[getIndexOfNode(nextBlock)];
                } while (getBlockId(block) != getBlockId(header));
                return true;
            }
//            return false;
            throw new LLVMBreakException(nextBlock);    // control flow by exception ...
        }

        private int executeNode(LLVMExpressionNode node, VirtualFrame frame) {
            if(node instanceof LLVMBasicBlockNode) {
                ((LLVMBasicBlockNode)node).executeStatements(frame);
                return getSuccessor(frame, (LLVMBasicBlockNode)node, beforeBlockNuller, afterBlockNuller);
            }else {
                assert(node instanceof LLVMLoopNode);
                return ((LLVMLoopNode)node).executeLoop(frame);
            }
        }

        private static int getBlockId(LLVMExpressionNode node) {
            if (node instanceof LLVMBasicBlockNode) {
                return ((LLVMBasicBlockNode)node).getBlockId();
            }else {
                assert(node instanceof LLVMLoopNode);
                return ((LLVMLoopNode)node).getLoopHeaderId();
            }
        }

        private boolean isInLoop(int block) {
            // TODO inefficient
            for(int i = 0; i < nodeIds.length; i++) {
                if(nodeIds[i] == block) return true;
            }
            return false;
        }

        private int getIndexOfNode(int nodeId) {
            // TODO inefficient: store information in datastructure (hashmap) for faster lookup
            for (int i = 0; i < nodeIds.length; i++) {
                if (nodeIds[i] == nodeId) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static int getSuccessor(VirtualFrame frame, LLVMBasicBlockNode bb, FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller) {
        int successor;
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
                executePhis(frame, conditionalBranchNode, LLVMConditionalBranchNode.TRUE_SUCCESSOR);
                nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
                successor = conditionalBranchNode.getTrueSuccessor();
                nullDeadSlots(frame, successor, beforeBlockNuller);
                return successor;
            } else {
                if (CompilerDirectives.inInterpreter()) {
                    bb.increaseBranchProbability(LLVMConditionalBranchNode.FALSE_SUCCESSOR);
                }
                executePhis(frame, conditionalBranchNode, LLVMConditionalBranchNode.FALSE_SUCCESSOR);
                nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
                successor = conditionalBranchNode.getFalseSuccessor();
                nullDeadSlots(frame, successor, beforeBlockNuller);
                return successor;
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
                    executePhis(frame, switchNode, i);
                    nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
                    successor = successors[i];
                    nullDeadSlots(frame, successor, beforeBlockNuller);
                    return successor;
                }
            }

            int i = successors.length - 1;
            if (CompilerDirectives.inInterpreter()) {
                bb.increaseBranchProbability(i);
            }
            executePhis(frame, switchNode, i);
            nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
            successor = successors[i];
            nullDeadSlots(frame, successor, beforeBlockNuller);
            return successor;
        } else if (controlFlowNode instanceof LLVMIndirectBranchNode) {
            LLVMIndirectBranchNode indirectBranchNode = (LLVMIndirectBranchNode) controlFlowNode;
            int[] successors = indirectBranchNode.getSuccessors();
            int successorBasicBlockIndex = indirectBranchNode.executeCondition(frame);
            for (int i = 0; i < successors.length - 1; i++) {
                if (CompilerDirectives.injectBranchProbability(bb.getBranchProbability(i), successors[i] == successorBasicBlockIndex)) {
                    if (CompilerDirectives.inInterpreter()) {
                        bb.increaseBranchProbability(i);
                    }
                    executePhis(frame, indirectBranchNode, i);
                    nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
                    successor = successors[i];
                    nullDeadSlots(frame, successor, beforeBlockNuller);
                    return successor;
                }
            }

            int i = successors.length - 1;
            assert successorBasicBlockIndex == successors[i];
            if (CompilerDirectives.inInterpreter()) {
                bb.increaseBranchProbability(i);
            }
            executePhis(frame, indirectBranchNode, i);
            nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
            successor = successors[i];
            nullDeadSlots(frame, successor, beforeBlockNuller);
            return successor;
        } else if (controlFlowNode instanceof LLVMBrUnconditionalNode) {
            LLVMBrUnconditionalNode unconditionalNode = (LLVMBrUnconditionalNode) controlFlowNode;
            unconditionalNode.execute(frame); // required for instrumentation
            executePhis(frame, unconditionalNode, 0);
            nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
            successor = unconditionalNode.getSuccessor();
            nullDeadSlots(frame, successor, beforeBlockNuller);
            return successor;
//        } else if (controlFlowNode instanceof LLVMInvokeNode) {
//            LLVMInvokeNode invokeNode = (LLVMInvokeNode) controlFlowNode;
//            try {
//                invokeNode.execute(frame);
//                executePhis(frame, invokeNode, LLVMInvokeNode.NORMAL_SUCCESSOR);
//                nullDeadSlots(frame, successor, afterBlockNuller);
//                successor = invokeNode.getNormalSuccessor();
//                nullDeadSlots(frame, successor, beforeBlockNuller);
//                return successor;
//            } catch (LLVMException e) {
//                frame.setObject(exceptionValueSlot, e);
//                executePhis(frame, invokeNode, LLVMInvokeNode.UNWIND_SUCCESSOR);
//                nullDeadSlots(frame, successor, afterBlockNuller);
//                successor = invokeNode.getUnwindSuccessor();
//                nullDeadSlots(frame, successor, beforeBlockNuller);
//                return successor;
//            }
//        } else if (controlFlowNode instanceof LLVMRetNode) {
//            LLVMRetNode retNode = (LLVMRetNode) controlFlowNode;
//            returnValue = retNode.execute(frame);
//            assert noPhisNecessary(retNode);
//            nullDeadSlots(frame, successor, afterBlockNuller);
//            successor = retNode.getSuccessor();
//            continue outer;
        } else if (controlFlowNode instanceof LLVMResumeNode) {
            LLVMResumeNode resumeNode = (LLVMResumeNode) controlFlowNode;
            assert noPhisNecessary(resumeNode);
            nullDeadSlots(frame, bb.getBlockId(), afterBlockNuller);
            resumeNode.execute(frame);
            CompilerAsserts.neverPartOfCompilation();
            throw new IllegalStateException("must not reach here");
        } else if (controlFlowNode instanceof LLVMUnreachableNode) {
            LLVMUnreachableNode unreachableNode = (LLVMUnreachableNode) controlFlowNode;
            assert noPhisNecessary(unreachableNode);
            unreachableNode.execute();
            CompilerAsserts.neverPartOfCompilation();
            throw new IllegalStateException("must not reach here");
        } else {
            CompilerAsserts.neverPartOfCompilation();
            throw new UnsupportedOperationException("unexpected controlFlowNode type: " + controlFlowNode);
        }
    }

    @ExplodeLoop
    private static void executePhis(VirtualFrame frame, LLVMControlFlowNode controlFlowNode, int successorIndex) {
        LLVMExpressionNode phi = controlFlowNode.getPhiNode(successorIndex);
        if (phi != null) {
            phi.executeGeneric(frame);
        }
    }

    @ExplodeLoop
    private static void nullDeadSlots(VirtualFrame frame, int bci, FrameSlot[][] blockNullers) {
        FrameSlot[] frameSlotsToNull = blockNullers[bci];
        if (frameSlotsToNull != null) {
            for (int i = 0; i < frameSlotsToNull.length; i++) {
                LLVMFrameNullerUtil.nullFrameSlot(frame, frameSlotsToNull[i]);
            }
        }
    }

    private static boolean noPhisNecessary(LLVMControlFlowNode controlFlowNode) {
        return controlFlowNode.getSuccessorCount() == 0 || controlFlowNode.getSuccessorCount() == 1 && controlFlowNode.getPhiNode(0) == null;
    }

    public static LLVMLoopNode create(LLVMExpressionNode[] basicBlocks,FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller) {
        return new LLVMLoopNode(basicBlocks, beforeBlockNuller, afterBlockNuller);
    }
}