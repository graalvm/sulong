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
package com.oracle.truffle.llvm.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.base.LLVMTerminatorNode;

// TODO remove code duplication
public abstract class LLVMSwitchNode extends LLVMTerminatorNode {

    private static final int DEFAULT_LABEL_INDEX = 0;
    private static final int CASE_LABEL_START_INDEX = 1;
    @Children final LLVMExpressionNode[] phiWriteNodes;

    public LLVMSwitchNode(int defaultLabel, int[] successors, LLVMExpressionNode[] phiWriteNodes) {
        super(getLabelArray(successors, defaultLabel));
        this.phiWriteNodes = phiWriteNodes;
    }

    protected static int[] getLabelArray(int[] successors, int defaultLabel) {
        CompilerAsserts.neverPartOfCompilation();
        int[] labels = new int[successors.length + 1];
        labels[DEFAULT_LABEL_INDEX] = defaultLabel;
        System.arraycopy(successors, 0, labels, CASE_LABEL_START_INDEX, successors.length);
        return labels;
    }

    @ExplodeLoop
    void executePhiWrites(VirtualFrame frame) {
        for (int i = 0; i < phiWriteNodes.length; i++) {
            phiWriteNodes[i].executeGeneric(frame);
        }
    }

    protected ConditionProfile[] createProfiles(int length) {
        CompilerAsserts.neverPartOfCompilation();
        ConditionProfile[] profiles = new ConditionProfile[length];
        for (int i = 0; i < profiles.length; i++) {
            profiles[i] = ConditionProfile.createCountingProfile();
        }
        return profiles;
    }

    public abstract static class LLVMI8SwitchBaseNode extends LLVMSwitchNode {

        @Child private LLVMExpressionNode cond;
        @Children private final LLVMExpressionNode[] cases;

        public LLVMI8SwitchBaseNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(defaultLabel, successors, phiWriteNodes);
            this.cond = cond;
            this.cases = cases;
        }

        @Override
        @ExplodeLoop
        public int executeGetSuccessorIndex(VirtualFrame frame) {
            int val = LLVMExpressionNode.expectI8(cond, frame);
            executePhiWrites(frame);
            for (int i = 0; i < cases.length; i++) {
                int caseValue = LLVMExpressionNode.expectI8(cases[i], frame);
                if (profile(i, val == caseValue)) {
                    return i + CASE_LABEL_START_INDEX;
                }
            }
            return DEFAULT_LABEL_INDEX;
        }

        abstract boolean profile(int i, boolean value);

    }

    public static class LLVMI8SwitchNode extends LLVMI8SwitchBaseNode {

        public LLVMI8SwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
        }

        @Override
        boolean profile(int i, boolean value) {
            return value;
        }

    }

    public static class LLVMI8ProfilingSwitchNode extends LLVMI8SwitchBaseNode {

        @CompilationFinal(dimensions = 1) private final ConditionProfile[] profiles;

        public LLVMI8ProfilingSwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
            profiles = createProfiles(cases.length);
        }

        @Override
        boolean profile(int i, boolean value) {
            return profiles[i].profile(value);
        }

    }

    public abstract static class LLVMI16SwitchBaseNode extends LLVMSwitchNode {

        @Child private LLVMExpressionNode cond;
        @Children private final LLVMExpressionNode[] cases;

        public LLVMI16SwitchBaseNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(defaultLabel, successors, phiWriteNodes);
            this.cond = cond;
            this.cases = cases;
        }

        @Override
        @ExplodeLoop
        public int executeGetSuccessorIndex(VirtualFrame frame) {
            short val = LLVMExpressionNode.expectI16(cond, frame);
            executePhiWrites(frame);
            for (short i = 0; i < cases.length; i++) {
                short caseValue = LLVMExpressionNode.expectI16(cases[i], frame);
                if (profile(i, val == caseValue)) {
                    return i + CASE_LABEL_START_INDEX;
                }
            }
            return DEFAULT_LABEL_INDEX;
        }

        abstract boolean profile(int i, boolean value);

    }

    public static class LLVMI16SwitchNode extends LLVMI16SwitchBaseNode {

        public LLVMI16SwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
        }

        @Override
        boolean profile(int i, boolean value) {
            return value;
        }

    }

    public static class LLVMI16ProfilingSwitchNode extends LLVMI16SwitchBaseNode {

        @CompilationFinal(dimensions = 1) private final ConditionProfile[] profiles;

        public LLVMI16ProfilingSwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
            profiles = createProfiles(cases.length);
        }

        @Override
        boolean profile(int i, boolean value) {
            return profiles[i].profile(value);
        }

    }

    public abstract static class LLVMI32SwitchBaseNode extends LLVMSwitchNode {

        @Child private LLVMExpressionNode cond;
        @Children private final LLVMExpressionNode[] cases;

        public LLVMI32SwitchBaseNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(defaultLabel, successors, phiWriteNodes);
            this.cond = cond;
            this.cases = cases;
        }

        @Override
        @ExplodeLoop
        public int executeGetSuccessorIndex(VirtualFrame frame) {
            int val = LLVMExpressionNode.expectI32(cond, frame);
            executePhiWrites(frame);
            for (int i = 0; i < cases.length; i++) {
                int caseValue = LLVMExpressionNode.expectI32(cases[i], frame);
                if (profile(i, val == caseValue)) {
                    return i + CASE_LABEL_START_INDEX;
                }
            }
            return DEFAULT_LABEL_INDEX;
        }

        abstract boolean profile(int i, boolean value);

    }

    public static class LLVMI32ProfilingSwitchNode extends LLVMI32SwitchBaseNode {

        @CompilationFinal(dimensions = 1) private final ConditionProfile[] profiles;

        public LLVMI32ProfilingSwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
            profiles = createProfiles(cases.length);
        }

        @Override
        boolean profile(int i, boolean value) {
            return profiles[i].profile(value);
        }

    }

    public static class LLVMI32SwitchNode extends LLVMI32SwitchBaseNode {

        @CompilationFinal(dimensions = 1) private ConditionProfile[] profiles;

        public LLVMI32SwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
            profiles = createProfiles(cases.length);
        }

        @Override
        boolean profile(int i, boolean value) {
            return value;
        }

    }

    public abstract static class LLVMI64SwitchBaseNode extends LLVMSwitchNode {

        @Child private LLVMExpressionNode cond;
        @Children private final LLVMExpressionNode[] cases;

        public LLVMI64SwitchBaseNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(defaultLabel, successors, phiWriteNodes);
            this.cond = cond;
            this.cases = cases;
        }

        @Override
        @ExplodeLoop
        public int executeGetSuccessorIndex(VirtualFrame frame) {
            long val = LLVMExpressionNode.expectI64(cond, frame);
            executePhiWrites(frame);
            for (int i = 0; i < cases.length; i++) {
                long caseValue = LLVMExpressionNode.expectI64(cases[i], frame);
                if (profile(i, val == caseValue)) {
                    return i + CASE_LABEL_START_INDEX;
                }
            }
            return DEFAULT_LABEL_INDEX;
        }

        abstract boolean profile(int i, boolean value);

    }

    public static class LLVMI64ProfilingSwitchNode extends LLVMI64SwitchBaseNode {

        @CompilationFinal(dimensions = 1) private final ConditionProfile[] profiles;

        public LLVMI64ProfilingSwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
            profiles = createProfiles(cases.length);
        }

        @Override
        boolean profile(int i, boolean value) {
            return profiles[i].profile(value);
        }

    }

    public static class LLVMI64SwitchNode extends LLVMI64SwitchBaseNode {

        public LLVMI64SwitchNode(LLVMExpressionNode cond, LLVMExpressionNode[] cases, int[] successors, int defaultLabel, LLVMExpressionNode[] phiWriteNodes) {
            super(cond, cases, successors, defaultLabel, phiWriteNodes);
        }

        @Override
        boolean profile(int i, boolean value) {
            return value;
        }

    }

}
