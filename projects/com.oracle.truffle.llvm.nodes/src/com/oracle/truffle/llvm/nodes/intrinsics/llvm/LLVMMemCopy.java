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
package com.oracle.truffle.llvm.nodes.intrinsics.llvm;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.memory.LLVMNativeFunctions.MemCopyNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public abstract class LLVMMemCopy {

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class, value = "destination"), @NodeChild(type = LLVMExpressionNode.class, value = "source"),
                    @NodeChild(type = LLVMExpressionNode.class, value = "length"),
                    @NodeChild(type = LLVMExpressionNode.class, value = "align"), @NodeChild(type = LLVMExpressionNode.class, value = "isVolatile")})
    public abstract static class LLVMMemI32Copy extends LLVMBuiltin {

        @Child private MemCopyNode memCopy;

        private MemCopyNode getMemCopy() {
            if (memCopy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                memCopy = insert(getContext().getNativeFunctions().createMemCopyNode());
            }
            return memCopy;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress target, LLVMAddress source, int length, int align, boolean isVolatile) {
            getMemCopy().execute(target, source, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable target, LLVMAddress source, int length, int align, boolean isVolatile) {
            getMemCopy().execute(target.getNativeLocation(), source, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress target, LLVMGlobalVariable source, int length, int align, boolean isVolatile) {
            getMemCopy().execute(target, source.getNativeLocation(), length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable target, LLVMGlobalVariable source, int length, int align, boolean isVolatile) {
            getMemCopy().execute(target.getNativeLocation(), source.getNativeLocation(), length);
            return null;
        }

    }

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class, value = "destination"), @NodeChild(type = LLVMExpressionNode.class, value = "source"),
                    @NodeChild(type = LLVMExpressionNode.class, value = "length"),
                    @NodeChild(type = LLVMExpressionNode.class, value = "align"), @NodeChild(type = LLVMExpressionNode.class, value = "isVolatile")})
    public abstract static class LLVMMemI64Copy extends LLVMBuiltin {

        @Child private MemCopyNode memCopy;

        private MemCopyNode getMemCopy() {
            if (memCopy == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                memCopy = insert(getContext().getNativeFunctions().createMemCopyNode());
            }
            return memCopy;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress target, LLVMAddress source, long length, int align, boolean isVolatile) {
            getMemCopy().execute(target, source, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable target, LLVMAddress source, long length, int align, boolean isVolatile) {
            getMemCopy().execute(target.getNativeLocation(), source, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress target, LLVMGlobalVariable source, long length, int align, boolean isVolatile) {
            getMemCopy().execute(target, source.getNativeLocation(), length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable target, LLVMGlobalVariable source, long length, int align, boolean isVolatile) {
            getMemCopy().execute(target.getNativeLocation(), source.getNativeLocation(), length);
            return null;
        }

    }

}
