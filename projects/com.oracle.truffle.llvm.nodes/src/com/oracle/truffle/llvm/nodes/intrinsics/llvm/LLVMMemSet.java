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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public abstract class LLVMMemSet extends LLVMBuiltin {

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class),
                    @NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMMemSetI64 extends LLVMMemSet {

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress address, byte value, long length, int align, boolean isVolatile) {
            memset(address, value, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable address, byte value, long length, int align, boolean isVolatile) {
            memset(address.getNativeLocation(), value, length);
            return null;
        }
    }

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class),
                    @NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMMemSetI32 extends LLVMMemSet {

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMAddress address, byte value, int length, int align, boolean isVolatile) {
            memset(address, value, length);
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object executeVoid(LLVMGlobalVariable address, byte value, int length, int align, boolean isVolatile) {
            memset(address.getNativeLocation(), value, length);
            return null;
        }
    }

    protected static final long MAX_JAVA_LEN = 256;

    @CompilationFinal private boolean inJava = true;

    protected void memset(LLVMAddress address, byte value, long length) {
        if (inJava) {
            if (length <= MAX_JAVA_LEN) {
                long current = address.getVal();
                long end = current + length;
                if (end - current >= 8) {
                    long v16 = ((long) value) << 8 | ((long) value & 0xFF);
                    long v32 = v16 << 16 | v16;
                    long v64 = v32 << 32 | v32;

                    do {
                        LLVMMemory.putI64(LLVMAddress.fromLong(current), v64);
                        current += 8;
                    } while (end - current >= 8);
                }

                while (current < end) {
                    LLVMMemory.putI8(LLVMAddress.fromLong(current), value);
                    current++;
                }
                return;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inJava = false;
            }
        }

        LLVMMemory.memset(address, length, value);
    }

}
