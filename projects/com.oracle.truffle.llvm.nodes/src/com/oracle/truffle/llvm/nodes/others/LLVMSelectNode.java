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
package com.oracle.truffle.llvm.nodes.others;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMFunctionHandle;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

@NodeChildren({@NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class)})
public abstract class LLVMSelectNode extends LLVMExpressionNode {
    protected final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public abstract static class LLVMI1SelectNode extends LLVMSelectNode {

        @Specialization
        public boolean execute(boolean cond, boolean trueBranch, boolean elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMI8SelectNode extends LLVMSelectNode {

        @Specialization
        public byte execute(boolean cond, byte trueBranch, byte elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMI16SelectNode extends LLVMSelectNode {

        @Specialization
        public short execute(boolean cond, short trueBranch, short elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMI32SelectNode extends LLVMSelectNode {

        @Specialization
        public int execute(boolean cond, int trueBranch, int elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMI64SelectNode extends LLVMSelectNode {

        @Specialization
        public long execute(boolean cond, long trueBranch, long elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }
    }

    public abstract static class LLVMFloatSelectNode extends LLVMSelectNode {

        @Specialization
        public float execute(boolean cond, float trueBranch, float elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMDoubleSelectNode extends LLVMSelectNode {

        @Specialization
        public double execute(boolean cond, double trueBranch, double elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVM80BitFloatSelectNode extends LLVMSelectNode {

        @Specialization
        public LLVM80BitFloat execute(boolean cond, LLVM80BitFloat trueBranch, LLVM80BitFloat elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }
    }

    public abstract static class LLVMAddressSelectNode extends LLVMSelectNode {

        @Specialization
        public Object execute(boolean cond, Object trueBranch, Object elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

    }

    public abstract static class LLVMFunctionSelectNode extends LLVMSelectNode {

        @Specialization
        public LLVMFunctionDescriptor execute(boolean cond, LLVMFunctionDescriptor trueBranch, LLVMFunctionDescriptor elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }

        @Specialization
        public LLVMFunctionHandle execute(boolean cond, LLVMFunctionHandle trueBranch, LLVMFunctionDescriptor elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : LLVMFunctionHandle.createHandle(elseBranch.getFunctionPointer());
        }

        @Specialization
        public LLVMFunctionHandle execute(boolean cond, LLVMFunctionDescriptor trueBranch, LLVMFunctionHandle elseBranch) {
            return conditionProfile.profile(cond) ? LLVMFunctionHandle.createHandle(trueBranch.getFunctionPointer()) : elseBranch;
        }

        @Specialization
        public LLVMFunctionHandle execute(boolean cond, LLVMFunctionHandle trueBranch, LLVMFunctionHandle elseBranch) {
            return conditionProfile.profile(cond) ? trueBranch : elseBranch;
        }
    }

}
