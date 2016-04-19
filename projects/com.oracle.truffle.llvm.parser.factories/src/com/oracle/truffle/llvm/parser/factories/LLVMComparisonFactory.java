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

import com.oracle.truffle.llvm.nodes.base.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMAddressNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMFunctionNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVM80BitFloatNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVMDoubleNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVMFloatNode;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI16Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI1Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI32Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI64Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI8Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMIVarBitNode;
import com.oracle.truffle.llvm.nodes.impl.base.vector.LLVMI1VectorNode;
import com.oracle.truffle.llvm.nodes.impl.base.vector.LLVMI32VectorNode;
import com.oracle.truffle.llvm.nodes.impl.literals.LLVMSimpleLiteralNode.LLVMI1LiteralNode;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatOrdNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVM80BitFloatCompareNodeFactory.LLVM80BitFloatUnoNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressEqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressNeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressSgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressSgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressSleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressSltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressUgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressUgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressUleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMAddressCompareNodeFactory.LLVMAddressUltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleOrdNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMDoubleCompareNodeFactory.LLVMDoubleUnoNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatOrdNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUeqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUneNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFloatCompareNodeFactory.LLVMFloatUnoNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFunctionCompareNodeFactory.LLVMFunctionEqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMFunctionCompareNodeFactory.LLVMFunctionNeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16EqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16NeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16SgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16SgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16SleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16SltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16UgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16UgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16UleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI16CompareNodeFactory.LLVMI16UltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI1CompareNodeFactory.LLVMI1EqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI1CompareNodeFactory.LLVMI1NeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32EqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32NeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32SgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32SgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32SleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32SltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32UgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32UgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32UleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32CompareNodeFactory.LLVMI32UltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorEqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorNeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorSgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorSgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorSleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorSltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorUgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorUgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorUleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI32VectorCompareNodeFactory.LLVMI32VectorUltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64EqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64NeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64SgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64SgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64SleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64SltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64UgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64UgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64UleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI64CompareNodeFactory.LLVMI64UltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8EqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8NeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8SgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8SgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8SleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8SltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8UgeNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8UgtNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8UleNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMI8CompareNodeFactory.LLVMI8UltNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMIVarCompareNodeFactory.LLVMIVarEqNodeGen;
import com.oracle.truffle.llvm.nodes.impl.op.compare.LLVMIVarCompareNodeFactory.LLVMIVarNeqNodeGen;
import com.oracle.truffle.llvm.parser.LLVMBaseType;
import com.oracle.truffle.llvm.parser.instructions.LLVMFloatComparisonType;
import com.oracle.truffle.llvm.parser.instructions.LLVMIntegerComparisonType;

public final class LLVMComparisonFactory {

    private LLVMComparisonFactory() {
    }

    public static LLVMI1Node createIntegerComparison(LLVMExpressionNode left, LLVMExpressionNode right, LLVMBaseType llvmType, LLVMIntegerComparisonType condition) {
        switch (llvmType) {
            case I1:
                return visitI1Comparison((LLVMI1Node) left, (LLVMI1Node) right, condition);
            case I8:
                return visitI8Comparison((LLVMI8Node) left, (LLVMI8Node) right, condition);
            case I16:
                return visitI16Comparison((LLVMI16Node) left, (LLVMI16Node) right, condition);
            case I32:
                return visitI32Comparison((LLVMI32Node) left, (LLVMI32Node) right, condition);
            case I64:
                return visitI64Comparison((LLVMI64Node) left, (LLVMI64Node) right, condition);
            case I_VAR_BITWIDTH:
                return visitIVarComparison((LLVMIVarBitNode) left, (LLVMIVarBitNode) right, condition);
            case ADDRESS:
                return visitAddressComparison((LLVMAddressNode) left, (LLVMAddressNode) right, condition);
            case FUNCTION_ADDRESS:
                return visitFunctionComparison((LLVMFunctionNode) left, (LLVMFunctionNode) right, condition);
            default:
                throw new AssertionError(llvmType);
        }
    }

    public static LLVMI1VectorNode createVectorComparison(LLVMAddressNode target, LLVMExpressionNode left, LLVMExpressionNode right, LLVMBaseType llvmType, LLVMIntegerComparisonType condition) {
        switch (llvmType) {
            case I32_VECTOR:
                return visitI32VectorComparison(target, (LLVMI32VectorNode) left, (LLVMI32VectorNode) right, condition);
            default:
                throw new AssertionError(llvmType);
        }
    }

    private static LLVMI1Node visitIVarComparison(LLVMIVarBitNode left, LLVMIVarBitNode right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case NOT_EQUALS:
                return LLVMIVarNeqNodeGen.create(left, right);
            case EQUALS:
                return LLVMIVarEqNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitI1Comparison(LLVMI1Node left, LLVMI1Node right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI1EqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMI1NeNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitI8Comparison(LLVMI8Node left, LLVMI8Node right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI8EqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMI8NeNodeGen.create(left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMI8UgtNodeGen.create(left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMI8UgeNodeGen.create(left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMI8UltNodeGen.create(left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMI8UleNodeGen.create(left, right);
            case SIGNED_GREATER_THAN:
                return LLVMI8SgtNodeGen.create(left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMI8SgeNodeGen.create(left, right);
            case SIGNED_LESS_THAN:
                return LLVMI8SltNodeGen.create(left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMI8SleNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitI16Comparison(LLVMI16Node left, LLVMI16Node right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI16EqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMI16NeNodeGen.create(left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMI16UgtNodeGen.create(left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMI16UgeNodeGen.create(left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMI16UltNodeGen.create(left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMI16UleNodeGen.create(left, right);
            case SIGNED_GREATER_THAN:
                return LLVMI16SgtNodeGen.create(left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMI16SgeNodeGen.create(left, right);
            case SIGNED_LESS_THAN:
                return LLVMI16SltNodeGen.create(left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMI16SleNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitI32Comparison(LLVMI32Node left, LLVMI32Node right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI32EqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMI32NeNodeGen.create(left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMI32UgtNodeGen.create(left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMI32UgeNodeGen.create(left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMI32UltNodeGen.create(left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMI32UleNodeGen.create(left, right);
            case SIGNED_GREATER_THAN:
                return LLVMI32SgtNodeGen.create(left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMI32SgeNodeGen.create(left, right);
            case SIGNED_LESS_THAN:
                return LLVMI32SltNodeGen.create(left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMI32SleNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitI64Comparison(LLVMI64Node left, LLVMI64Node right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI64EqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMI64NeNodeGen.create(left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMI64UgtNodeGen.create(left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMI64UgeNodeGen.create(left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMI64UltNodeGen.create(left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMI64UleNodeGen.create(left, right);
            case SIGNED_GREATER_THAN:
                return LLVMI64SgtNodeGen.create(left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMI64SgeNodeGen.create(left, right);
            case SIGNED_LESS_THAN:
                return LLVMI64SltNodeGen.create(left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMI64SleNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitAddressComparison(LLVMAddressNode left, LLVMAddressNode right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMAddressEqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMAddressNeNodeGen.create(left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMAddressUgtNodeGen.create(left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMAddressUgeNodeGen.create(left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMAddressUltNodeGen.create(left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMAddressUleNodeGen.create(left, right);
            case SIGNED_GREATER_THAN:
                return LLVMAddressSgtNodeGen.create(left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMAddressSgeNodeGen.create(left, right);
            case SIGNED_LESS_THAN:
                return LLVMAddressSltNodeGen.create(left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMAddressSleNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitFunctionComparison(LLVMFunctionNode left, LLVMFunctionNode right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMFunctionEqNodeGen.create(left, right);
            case NOT_EQUALS:
                return LLVMFunctionNeNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    public static LLVMI1Node createFloatComparison(LLVMExpressionNode left, LLVMExpressionNode right, LLVMBaseType llvmType, LLVMFloatComparisonType condition) {
        if (condition == LLVMFloatComparisonType.FALSE) {
            return new LLVMI1LiteralNode(false);
        } else if (condition == LLVMFloatComparisonType.TRUE) {
            return new LLVMI1LiteralNode(true);
        }
        switch (llvmType) {
            case FLOAT:
                return visitFloatComparison((LLVMFloatNode) left, (LLVMFloatNode) right, condition);
            case DOUBLE:
                return visitDoubleComparison((LLVMDoubleNode) left, (LLVMDoubleNode) right, condition);
            case X86_FP80:
                return visit80BitFloatComparison((LLVM80BitFloatNode) left, (LLVM80BitFloatNode) right, condition);
            default:
                throw new AssertionError(llvmType);
        }
    }

    private static LLVMI1Node visitFloatComparison(LLVMFloatNode left, LLVMFloatNode right, LLVMFloatComparisonType condition) {
        switch (condition) {
            case ORDERED_AND_EQUALS:
                return LLVMFloatOeqNodeGen.create(left, right);
            case ORDERED_AND_GREATER_THAN:
                return LLVMFloatOgtNodeGen.create(left, right);
            case ORDERED_AND_GREATER_EQUALS:
                return LLVMFloatOgeNodeGen.create(left, right);
            case ORDERED_AND_LESS_THAN:
                return LLVMFloatOltNodeGen.create(left, right);
            case ORDERED_AND_LESS_EQUALS:
                return LLVMFloatOleNodeGen.create(left, right);
            case ORDERED_AND_NOT_EQUALS:
                return LLVMFloatOneNodeGen.create(left, right);
            case ORDERED:
                return LLVMFloatOrdNodeGen.create(left, right);
            case UNORDERED_OR_EQUALS:
                return LLVMFloatUeqNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_THAN:
                return LLVMFloatUgtNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_EQUALS:
                return LLVMFloatUgeNodeGen.create(left, right);
            case UNORDERED_OR_LESS_THAN:
                return LLVMFloatUltNodeGen.create(left, right);
            case UNORDERED_OR_LESS_EQUALS:
                return LLVMFloatUleNodeGen.create(left, right);
            case UNORDERED_OR_NOT_EQUALS:
                return LLVMFloatUneNodeGen.create(left, right);
            case UNORDERED:
                return LLVMFloatUnoNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visitDoubleComparison(LLVMDoubleNode left, LLVMDoubleNode right, LLVMFloatComparisonType condition) {
        switch (condition) {
            case ORDERED_AND_EQUALS:
                return LLVMDoubleOeqNodeGen.create(left, right);
            case ORDERED_AND_GREATER_THAN:
                return LLVMDoubleOgtNodeGen.create(left, right);
            case ORDERED_AND_GREATER_EQUALS:
                return LLVMDoubleOgeNodeGen.create(left, right);
            case ORDERED_AND_LESS_THAN:
                return LLVMDoubleOltNodeGen.create(left, right);
            case ORDERED_AND_LESS_EQUALS:
                return LLVMDoubleOleNodeGen.create(left, right);
            case ORDERED_AND_NOT_EQUALS:
                return LLVMDoubleOneNodeGen.create(left, right);
            case ORDERED:
                return LLVMDoubleOrdNodeGen.create(left, right);
            case UNORDERED_OR_EQUALS:
                return LLVMDoubleUeqNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_THAN:
                return LLVMDoubleUgtNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_EQUALS:
                return LLVMDoubleUgeNodeGen.create(left, right);
            case UNORDERED_OR_LESS_THAN:
                return LLVMDoubleUltNodeGen.create(left, right);
            case UNORDERED_OR_LESS_EQUALS:
                return LLVMDoubleUleNodeGen.create(left, right);
            case UNORDERED_OR_NOT_EQUALS:
                return LLVMDoubleUneNodeGen.create(left, right);
            case UNORDERED:
                return LLVMDoubleUnoNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1Node visit80BitFloatComparison(LLVM80BitFloatNode left, LLVM80BitFloatNode right, LLVMFloatComparisonType condition) {
        switch (condition) {
            case ORDERED_AND_EQUALS:
                return LLVM80BitFloatOeqNodeGen.create(left, right);
            case ORDERED_AND_GREATER_THAN:
                return LLVM80BitFloatOgtNodeGen.create(left, right);
            case ORDERED_AND_GREATER_EQUALS:
                return LLVM80BitFloatOgeNodeGen.create(left, right);
            case ORDERED_AND_LESS_THAN:
                return LLVM80BitFloatOltNodeGen.create(left, right);
            case ORDERED_AND_LESS_EQUALS:
                return LLVM80BitFloatOleNodeGen.create(left, right);
            case ORDERED_AND_NOT_EQUALS:
                return LLVM80BitFloatOneNodeGen.create(left, right);
            case ORDERED:
                return LLVM80BitFloatOrdNodeGen.create(left, right);
            case UNORDERED_OR_EQUALS:
                return LLVM80BitFloatUeqNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_THAN:
                return LLVM80BitFloatUgtNodeGen.create(left, right);
            case UNORDERED_OR_GREATER_EQUALS:
                return LLVM80BitFloatUgeNodeGen.create(left, right);
            case UNORDERED_OR_LESS_THAN:
                return LLVM80BitFloatUltNodeGen.create(left, right);
            case UNORDERED_OR_LESS_EQUALS:
                return LLVM80BitFloatUleNodeGen.create(left, right);
            case UNORDERED_OR_NOT_EQUALS:
                return LLVM80BitFloatUneNodeGen.create(left, right);
            case UNORDERED:
                return LLVM80BitFloatUnoNodeGen.create(left, right);
            default:
                throw new AssertionError(condition);
        }
    }

    private static LLVMI1VectorNode visitI32VectorComparison(LLVMAddressNode target, LLVMI32VectorNode left, LLVMI32VectorNode right, LLVMIntegerComparisonType condition) {
        switch (condition) {
            case EQUALS:
                return LLVMI32VectorEqNodeGen.create(target, left, right);
            case NOT_EQUALS:
                return LLVMI32VectorNeNodeGen.create(target, left, right);
            case UNSIGNED_GREATER_THAN:
                return LLVMI32VectorUgtNodeGen.create(target, left, right);
            case UNSIGNED_GREATER_EQUALS:
                return LLVMI32VectorUgeNodeGen.create(target, left, right);
            case UNSIGNED_LESS_THAN:
                return LLVMI32VectorUltNodeGen.create(target, left, right);
            case UNSIGNED_LESS_EQUALS:
                return LLVMI32VectorUleNodeGen.create(target, left, right);
            case SIGNED_GREATER_THAN:
                return LLVMI32VectorSgtNodeGen.create(target, left, right);
            case SIGNED_GREATER_EQUALS:
                return LLVMI32VectorSgeNodeGen.create(target, left, right);
            case SIGNED_LESS_THAN:
                return LLVMI32VectorSltNodeGen.create(target, left, right);
            case SIGNED_LESS_EQUALS:
                return LLVMI32VectorSleNodeGen.create(target, left, right);
            default:
                throw new AssertionError(condition);
        }
    }
}
