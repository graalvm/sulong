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
package com.oracle.truffle.llvm.parser.factories;

import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16AndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16AshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16LshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16OrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16ShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI16LogicalNodeFactory.LLVMI16XorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI1LogicalNodeFactory.LLVMI1AndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI1LogicalNodeFactory.LLVMI1OrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI1LogicalNodeFactory.LLVMI1XorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32AndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32AshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32LshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32OrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32ShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI32LogicalNodeFactory.LLVMI32XorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64AndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64AshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64LshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64OrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64ShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI64LogicalNodeFactory.LLVMI64XorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8AndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8AshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8LshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8OrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8ShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMI8LogicalNodeFactory.LLVMI8XorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.integer.LLVMIVarLogicalNodeFactory.LLVMIVarXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI16VectorLogicalNodeFactory.LLVMI16VectorXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI1VectorLogicalNodeFactory.LLVMI1VectorXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI32VectorLogicalNodeFactory.LLVMI32VectorXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI64VectorLogicalNodeFactory.LLVMI64VectorXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.logical.vector.LLVMI8VectorLogicalNodeFactory.LLVMI8VectorXorNodeGen;
import com.oracle.truffle.llvm.parser.instructions.LLVMLogicalInstructionKind;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.VectorType;

final class LLVMLogicalFactory {

    private LLVMLogicalFactory() {
    }

    static LLVMExpressionNode createLogicalOperation(LLVMExpressionNode left, LLVMExpressionNode right, LLVMLogicalInstructionKind type, Type llvmType) {
        return createNode(left, right, llvmType, type);
    }

    private static LLVMExpressionNode createNode(LLVMExpressionNode left, LLVMExpressionNode right, Type llvmType, LLVMLogicalInstructionKind type)
                    throws AssertionError {
        if (llvmType instanceof VectorType) {
            PrimitiveType elementType = ((VectorType) llvmType).getElementType();
            switch (elementType.getPrimitiveKind()) {
                case I1:
                    return visitLogicalI1VectorInstruction(type, left, right);
                case I8:
                    return visitLogicalI8VectorInstruction(type, left, right);
                case I16:
                    return visitLogicalI16VectorInstruction(type, left, right);
                case I32:
                    return visitLogicalI32VectorInstruction(type, left, right);
                case I64:
                    return visitLogicalI64VectorInstruction(type, left, right);
                default:
                    throw new AssertionError(llvmType);
            }
        } else if (llvmType instanceof VariableBitWidthType) {
            return visitLogicalIVarInstruction(type, left, right);
        } else if (llvmType instanceof PrimitiveType) {
            return handlePrimitive(left, right, llvmType, type);
        }
        throw new AssertionError(llvmType);
    }

    private static LLVMExpressionNode handlePrimitive(LLVMExpressionNode left, LLVMExpressionNode right, Type llvmType, LLVMLogicalInstructionKind type) throws AssertionError {
        switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
            case I1:
                return visitLogicalI1Instruction(type, left, right);
            case I8:
                return visitLogicalI8Instruction(type, left, right);
            case I16:
                return visitLogicalI16Instruction(type, left, right);
            case I32:
                return visitLogicalI32Instruction(type, left, right);
            case I64:
                return visitLogicalI64Instruction(type, left, right);
            default:
                throw new AssertionError(llvmType);
        }
    }

    private static LLVMExpressionNode visitLogicalIVarInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMIVarShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMIVarLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMIVarAshrNodeGen.create(left, right);
            case AND:
                return LLVMIVarAndNodeGen.create(left, right);
            case OR:
                return LLVMIVarOrNodeGen.create(left, right);
            case XOR:
                return LLVMIVarXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI1VectorInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI1VectorShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI1VectorLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI1VectorAshrNodeGen.create(left, right);
            case AND:
                return LLVMI1VectorAndNodeGen.create(left, right);
            case OR:
                return LLVMI1VectorOrNodeGen.create(left, right);
            case XOR:
                return LLVMI1VectorXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI16VectorInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI16VectorShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI16VectorLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI16VectorAshrNodeGen.create(left, right);
            case AND:
                return LLVMI16VectorAndNodeGen.create(left, right);
            case OR:
                return LLVMI16VectorOrNodeGen.create(left, right);
            case XOR:
                return LLVMI16VectorXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI8VectorInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI8VectorShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI8VectorLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI8VectorAshrNodeGen.create(left, right);
            case AND:
                return LLVMI8VectorAndNodeGen.create(left, right);
            case OR:
                return LLVMI8VectorOrNodeGen.create(left, right);
            case XOR:
                return LLVMI8VectorXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI32VectorInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI32VectorShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI32VectorLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI32VectorAshrNodeGen.create(left, right);
            case AND:
                return LLVMI32VectorAndNodeGen.create(left, right);
            case OR:
                return LLVMI32VectorOrNodeGen.create(left, right);
            case XOR:
                return LLVMI32VectorXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI64VectorInstruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI64VectorShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI64VectorLshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI64VectorAshrNodeGen.create(left, right);
            case AND:
                return LLVMI64VectorAndNodeGen.create(left, right);
            case OR:
                return LLVMI64VectorOrNodeGen.create(left, right);
            case XOR:
                return LLVMI64VectorXorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI1Instruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case AND:
                return LLVMI1AndNodeGen.create(left, right);
            case OR:
                return LLVMI1OrNodeGen.create(left, right);
            case XOR:
                return LLVMI1XorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI8Instruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI8ShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI8LshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI8AshrNodeGen.create(left, right);
            case AND:
                return LLVMI8AndNodeGen.create(left, right);
            case OR:
                return LLVMI8OrNodeGen.create(left, right);
            case XOR:
                return LLVMI8XorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI16Instruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI16ShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI16LshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI16AshrNodeGen.create(left, right);
            case AND:
                return LLVMI16AndNodeGen.create(left, right);
            case OR:
                return LLVMI16OrNodeGen.create(left, right);
            case XOR:
                return LLVMI16XorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI32Instruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI32ShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI32LshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI32AshrNodeGen.create(left, right);
            case AND:
                return LLVMI32AndNodeGen.create(left, right);
            case OR:
                return LLVMI32OrNodeGen.create(left, right);
            case XOR:
                return LLVMI32XorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    private static LLVMExpressionNode visitLogicalI64Instruction(LLVMLogicalInstructionKind type, LLVMExpressionNode left, LLVMExpressionNode right) {
        switch (type) {
            case SHIFT_LEFT:
                return LLVMI64ShlNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMI64LshrNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMI64AshrNodeGen.create(left, right);
            case AND:
                return LLVMI64AndNodeGen.create(left, right);
            case OR:
                return LLVMI64OrNodeGen.create(left, right);
            case XOR:
                return LLVMI64XorNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

}
