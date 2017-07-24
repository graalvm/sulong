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
package com.oracle.truffle.llvm.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.nodes.cast.LLVMToI64Node.LLVMToI64BitNode;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMBoxedPrimitive;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMFunctionHandle;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariableAccess;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM.ForeignToLLVMType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;

@NodeChild(value = "fromNode", type = LLVMExpressionNode.class)
public abstract class LLVMToI8Node extends LLVMExpressionNode {

    @Specialization
    public byte executeI8(LLVMFunctionDescriptor from) {
        return (byte) from.getFunctionPointer();
    }

    @Specialization
    public byte executeI8(LLVMFunctionHandle from) {
        return (byte) from.getFunctionPointer();
    }

    @Specialization
    public byte executeLLVMAddress(LLVMGlobalVariable from, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        return (byte) globalAccess.getNativeLocation(from).getVal();
    }

    @Specialization
    public byte executeLLVMTruffleObject(LLVMTruffleObject from) {
        return (byte) (executeTruffleObject(from.getObject()) + from.getOffset());
    }

    @Child private Node isNull = Message.IS_NULL.createNode();
    @Child private Node isBoxed = Message.IS_BOXED.createNode();
    @Child private Node unbox = Message.UNBOX.createNode();
    @Child private ForeignToLLVM convert = ForeignToLLVM.create(ForeignToLLVMType.I8);

    @Specialization(guards = "notLLVM(from)")
    public byte executeTruffleObject(TruffleObject from) {
        if (ForeignAccess.sendIsNull(isNull, from)) {
            return 0;
        } else if (ForeignAccess.sendIsBoxed(isBoxed, from)) {
            try {
                return (byte) convert.executeWithTarget(ForeignAccess.sendUnbox(unbox, from));
            } catch (UnsupportedMessageException e) {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(e);
            }
        }
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException("Not convertable");
    }

    @Specialization
    public byte executeLLVMBoxedPrimitive(LLVMBoxedPrimitive from) {
        return (byte) convert.executeWithTarget(from.getValue());
    }

    public abstract static class LLVMToI8NoZeroExtNode extends LLVMToI8Node {

        @Specialization
        public byte executeI8(boolean from) {
            return from ? (byte) -1 : 0;
        }

        @Specialization
        public byte executeI8(short from) {
            return (byte) from;
        }

        @Specialization
        public byte executeI8(int from) {
            return (byte) from;
        }

        @Specialization
        public byte executeI8(long from) {
            return (byte) from;
        }

        @Specialization
        public byte executeI8(LLVMIVarBit from) {
            return from.getByteValue();
        }

        @Specialization
        public byte executeI8(float from) {
            return (byte) from;
        }

        @Specialization
        public byte executeI8(double from) {
            return (byte) from;
        }

        @Specialization
        public byte executeI8(LLVM80BitFloat from) {
            return from.getByteValue();
        }

        @Specialization
        public byte executeI8(LLVMAddress from) {
            return (byte) from.getVal();
        }

        @Specialization
        public byte executeI8(byte from) {
            return from;
        }

    }

    public abstract static class LLVMToI8ZeroExtNode extends LLVMToI8Node {

        @Specialization
        public byte executeI8(boolean from) {
            return (byte) (from ? 1 : 0);
        }

        @Specialization
        public byte executeI8(byte from) {
            return from;
        }
    }

    public abstract static class LLVMToI8BitNode extends LLVMToI8Node {

        @Specialization
        public byte executeI8(byte from) {
            return from;
        }

        @Specialization
        public byte executeI1Vector(LLVMI1Vector from) {
            return (byte) LLVMToI64BitNode.castI1Vector(from, Byte.SIZE);
        }

        @Specialization
        public byte executeI8Vector(LLVMI8Vector from) {
            if (from.getLength() != 1) {
                CompilerDirectives.transferToInterpreter();
                throw new AssertionError("invalid vector size!");
            }
            return from.getValue(0);
        }
    }

}
