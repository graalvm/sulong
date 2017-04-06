/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.memory;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMDataEscapeNode;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMDataEscapeNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMBoxedPrimitive;
import com.oracle.truffle.llvm.runtime.LLVMGlobalVariableDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;

@NodeChildren({@NodeChild(type = LLVMExpressionNode.class, value = "ptrNode")})
public abstract class LLVMCompareExchangeNode extends LLVMExpressionNode {

    @Child protected Node foreignWrite = Message.WRITE.createNode();
    @Child protected LLVMDataEscapeNode dataEscape = LLVMDataEscapeNodeGen.create();

    protected void doForeignAccess(LLVMTruffleObject addr, int stride, Object value) {
        try {
            ForeignAccess.sendWrite(foreignWrite, addr.getObject(), (int) (addr.getOffset() / stride), dataEscape.executeWithTarget(value));
        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void doForeignAccess(TruffleObject addr, Object value) {
        try {
            ForeignAccess.sendWrite(foreignWrite, addr, 0, dataEscape.executeWithTarget(value));
        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    @NodeChildren(value = {@NodeChild(type = LLVMExpressionNode.class, value = "cmpResult"), @NodeChild(type = LLVMExpressionNode.class, value = "oldValue"),
                    @NodeChild(type = LLVMExpressionNode.class, value = "replacementValue")})
    public abstract static class LLVMI8CmpxchgNode extends LLVMCompareExchangeNode {

        @Specialization
        public Object execute(LLVMGlobalVariableDescriptor address, boolean doReplace, byte oldVal, byte newVal) {
            if (doReplace) {
                LLVMMemory.putI8(address.getNativeAddress(), newVal);
            }
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, boolean doReplace, byte oldVal, byte newVal) {
            if (doReplace) {
                LLVMMemory.putI8(address, newVal);
            }
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, boolean doReplace, byte oldVal, byte newVal) {
            if (doReplace) {
                doForeignAccess(address, 1, newVal);
            }
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, boolean doReplace, byte oldVal, byte newVal) {
            if (address.getValue() instanceof Long) {
                if (doReplace) {
                    LLVMMemory.putI8(LLVMAddress.fromLong((long) address.getValue()), newVal);
                }
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, boolean doReplace, byte oldVal, byte newVal) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I8), doReplace, oldVal, newVal);
            return null;
        }
    }

}
