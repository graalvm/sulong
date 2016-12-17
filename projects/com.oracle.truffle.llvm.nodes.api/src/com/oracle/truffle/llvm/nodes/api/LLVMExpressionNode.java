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
package com.oracle.truffle.llvm.nodes.api;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.types.LLVMIVarBit;
import com.oracle.truffle.llvm.types.LLVMTruffleAddress;
import com.oracle.truffle.llvm.types.LLVMTruffleObject;
import com.oracle.truffle.llvm.types.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.types.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.types.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.types.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI8Vector;

/**
 * An expression node is a node that returns a result, e.g., a local variable read, or an addition
 * operation.
 */
@TypeSystemReference(LLVMTypes.class)
public abstract class LLVMExpressionNode extends LLVMNode {

    public static final int DOUBLE_SIZE_IN_BYTES = 8;
    public static final int FLOAT_SIZE_IN_BYTES = 4;

    public static final int I16_SIZE_IN_BYTES = 2;
    public static final int I16_MASK = 0xffff;

    public static final int I32_SIZE_IN_BYTES = 4;
    public static final long I32_MASK = 0xffffffffL;

    public static final int I64_SIZE_IN_BYTES = 8;

    public static final int I8_MASK = 0xff;

    public static final int ADDRESS_SIZE_IN_BYTES = 8;

    public abstract Object executeGeneric(VirtualFrame frame);

    public LLVM80BitFloat executeLLVM80BitFloat(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVM80BitFloat(executeGeneric(frame));
    }

    public LLVMAddress executeLLVMAddress(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMAddress(executeGeneric(frame));
    }

    public LLVMTruffleAddress executeLLVMTruffleAddress(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMTruffleAddress(executeGeneric(frame));
    }

    public LLVMTruffleObject executeLLVMTruffleObject(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMTruffleObject(executeGeneric(frame));
    }

    public TruffleObject executeTruffleObject(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectTruffleObject(executeGeneric(frame));
    }

    public byte[] executeByteArray(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectByteArray(executeGeneric(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectDouble(executeGeneric(frame));
    }

    public float executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectFloat(executeGeneric(frame));
    }

    public short executeI16(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectShort(executeGeneric(frame));
    }

    public boolean executeI1(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectBoolean(executeGeneric(frame));
    }

    public int executeI32(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectInteger(executeGeneric(frame));
    }

    public long executeI64(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLong(executeGeneric(frame));
    }

    public LLVMIVarBit executeLLVMIVarBit(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMIVarBit(executeGeneric(frame));
    }

    public byte executeI8(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectByte(executeGeneric(frame));
    }

    public LLVMI8Vector executeLLVMI8Vector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMI8Vector(executeGeneric(frame));
    }

    public LLVMI64Vector executeLLVMI64Vector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMI64Vector(executeGeneric(frame));
    }

    public LLVMI32Vector executeLLVMI32Vector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMI32Vector(executeGeneric(frame));
    }

    public LLVMI1Vector executeLLVMI1Vector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMI1Vector(executeGeneric(frame));
    }

    public LLVMI16Vector executeLLVMI16Vector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMI16Vector(executeGeneric(frame));
    }

    public LLVMFloatVector executeLLVMFloatVector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMFloatVector(executeGeneric(frame));
    }

    public LLVMDoubleVector executeLLVMDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMDoubleVector(executeGeneric(frame));
    }

    public LLVMFunctionDescriptor executeLLVMFunctionDescriptor(VirtualFrame frame) throws UnexpectedResultException {
        return LLVMTypesGen.expectLLVMFunctionDescriptor(executeGeneric(frame));
    }

    // static executes
    public static LLVM80BitFloat expectLLVM80BitFloat(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVM80BitFloat(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMAddress expectLLVMAddress(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMAddress(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMTruffleAddress expectLLVMTruffleAddress(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMTruffleAddress(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMTruffleObject expectLLVMTruffleObject(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMTruffleObject(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static TruffleObject expectTruffleObject(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeTruffleObject(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static byte[] expectByteArray(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeByteArray(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static double expectDouble(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeDouble(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static float expectFloat(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeFloat(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static short expectI16(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeI16(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static boolean expectI1(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeI1(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static int expectI32(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeI32(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static long expectI64(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeI64(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMIVarBit expectLLVMIVarBit(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMIVarBit(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static byte expectI8(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeI8(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMI8Vector expectLLVMI8Vector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMI8Vector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMI64Vector expectLLVMI64Vector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMI64Vector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMI32Vector expectLLVMI32Vector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMI32Vector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMI1Vector expectLLVMI1Vector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMI1Vector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMI16Vector expectLLVMI16Vector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMI16Vector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMFloatVector expectLLVMFloatVector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMFloatVector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMDoubleVector expectLLVMDoubleVector(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMDoubleVector(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    public static LLVMFunctionDescriptor expectLLVMFunctionDescriptor(LLVMExpressionNode node, VirtualFrame frame) {
        try {
            return node.executeLLVMFunctionDescriptor(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError(e);
        }
    }

    protected boolean isLLVMAddress(Object object) {
        return object instanceof LLVMAddress;
    }

    public String getSourceDescription() {
        return null;
    }
}
