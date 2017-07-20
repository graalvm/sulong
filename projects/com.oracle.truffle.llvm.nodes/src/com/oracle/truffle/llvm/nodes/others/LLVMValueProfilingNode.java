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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ByteValueProfile;
import com.oracle.truffle.api.profiles.DoubleValueProfile;
import com.oracle.truffle.api.profiles.FloatValueProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMFunction;
import com.oracle.truffle.llvm.runtime.LLVMFunctionHandle;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.profiling.BooleanValueProfile;
import com.oracle.truffle.llvm.runtime.profiling.ShortValueProfile;

@NodeChild
public abstract class LLVMValueProfilingNode extends LLVMExpressionNode {

    public abstract Object executeWithTarget(Object value);

    public abstract static class LLVMI1ProfiledValueNode extends LLVMValueProfilingNode {

        private final BooleanValueProfile profile = BooleanValueProfile.create();

        @Specialization
        public boolean executeI1(boolean value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMI8ProfiledValueNode extends LLVMValueProfilingNode {

        private final ByteValueProfile profile = ByteValueProfile.createIdentityProfile();

        @Specialization
        public byte executeI8(byte value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMI16ProfiledValueNode extends LLVMValueProfilingNode {

        private final ShortValueProfile profile = ShortValueProfile.create();

        @Specialization
        public short executeI1(short value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMI32ProfiledValueNode extends LLVMValueProfilingNode {

        private final IntValueProfile profile = IntValueProfile.createIdentityProfile();

        @Specialization
        public int executeI32(int value) {
            return profile.profile(value);
        }
    }

    public abstract static class LLVMI64ProfiledValueNode extends LLVMValueProfilingNode {

        private final LongValueProfile profile = LongValueProfile.createIdentityProfile();

        @Specialization
        public long executeI64(long value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMFloatProfiledValueNode extends LLVMValueProfilingNode {

        private final FloatValueProfile profile = FloatValueProfile.createRawIdentityProfile();

        @Specialization
        public float executeFloat(float value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMDoubleProfiledValueNode extends LLVMValueProfilingNode {

        private final DoubleValueProfile profile = DoubleValueProfile.createRawIdentityProfile();

        @Specialization
        public double executeFloat(double value) {
            return profile.profile(value);
        }

    }

    public abstract static class LLVMAddressProfiledValueNode extends LLVMValueProfilingNode {

        @Specialization
        public LLVMAddress executeAddress(LLVMAddress value, @Cached("createIdentityProfile()") LongValueProfile profile) {
            return LLVMAddress.fromLong(profile.profile(value.getVal()));
        }

        @Specialization
        public LLVMFunction executeFunction(LLVMFunctionHandle value, @Cached("createIdentityProfile()") LongValueProfile profile) {
            return getContext().createHandle(profile.profile(value.getFunctionPointer()));
        }

        @Specialization
        public Object noCache(Object value) {
            return value;
        }

    }

}
