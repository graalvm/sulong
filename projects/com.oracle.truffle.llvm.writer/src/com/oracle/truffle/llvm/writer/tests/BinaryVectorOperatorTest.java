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
package com.oracle.truffle.llvm.writer.tests;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.VectorType;
import com.oracle.truffle.llvm.parser.bc.util.writer.ModelToIRVisitor;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;

@SuppressWarnings("unused")
@RunWith(Parameterized.class)
public class BinaryVectorOperatorTest {

    private final IntegerType type;
    private final BinaryOperator operator;

    public BinaryVectorOperatorTest(IntegerType type, BinaryOperator operator) {
        this.type = type;
        this.operator = operator;
    }

    @Parameters(name = "{index}: BinaryVectorOperator[type={0}, operator={1}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (IntegerType type : IntegerType.values()) {
            for (BinaryOperator operator : BinaryOperator.values()) {
                if (operator.isFloatingPoint()) {
                    continue;
                }
                parameters.add(new Object[]{type, operator});
            }
        }

        return parameters;
    }

    private static final long VECTOR1_1 = 111191111L; // prim
    private static final long VECTOR1_2 = 792606555396976L; // even

    private static final long VECTOR2_1 = 200560490131L; // prim
    private static final long VECTOR2_2 = 1442968193L; // prim

    // Checkstyle: stop magic number name check
    @Test
    public void test() {
        long maxValue = type.getBits() < 64 ? 1L << type.getBits() : Long.MAX_VALUE;

        long vector11 = VECTOR1_1 % maxValue;
        long vector12 = VECTOR1_2 % maxValue;
        long vector21 = VECTOR2_1 % maxValue;
        long vector22 = VECTOR2_2 % maxValue;

        long result1 = 0, result2 = 0;

        // TODO: signed/unsigned?
        switch (operator) {
            case INT_ADD:
                result1 = (vector11 + vector21) % maxValue;
                result2 = (vector12 + vector22) % maxValue;
                break;
            case INT_SUBTRACT:
                result1 = (vector11 - vector21) % maxValue;
                result2 = (vector12 - vector22) % maxValue;
                break;
            case INT_MULTIPLY:
                result1 = (vector11 * vector21) % maxValue;
                result2 = (vector12 * vector22) % maxValue;
                break;
            case INT_UNSIGNED_DIVIDE:
                result1 = (vector11 / vector21) % maxValue;
                result2 = (vector12 / vector22) % maxValue;
                break;
            case INT_SIGNED_DIVIDE:
                result1 = (vector11 / vector21) % maxValue;
                result2 = (vector12 / vector22) % maxValue;
                break;
            case INT_UNSIGNED_REMAINDER:
                result1 = (vector11 % vector21) % maxValue;
                result2 = (vector12 % vector22) % maxValue;
                break;
            case INT_SIGNED_REMAINDER:
                result1 = (vector11 % vector21) % maxValue;
                result2 = (vector12 % vector22) % maxValue;
                break;
            case INT_SHIFT_LEFT:
                result1 = (vector11 << vector21) % maxValue;
                result2 = (vector12 << vector22) % maxValue;
                break;
            case INT_LOGICAL_SHIFT_RIGHT:
                result1 = (vector11 >> vector21) % maxValue;
                result2 = (vector12 >> vector22) % maxValue;
                break;
            case INT_ARITHMETIC_SHIFT_RIGHT:
                result1 = (vector11 >>> vector21) % maxValue;
                result2 = (vector12 >>> vector22) % maxValue;
                break;
            case INT_AND:
                result1 = (vector11 & vector21) % maxValue;
                result2 = (vector12 & vector22) % maxValue;
                break;
            case INT_OR:
                result1 = (vector11 | vector21) % maxValue;
                result2 = (vector12 | vector22) % maxValue;
                break;
            case INT_XOR:
                result1 = (vector11 ^ vector21) % maxValue;
                result2 = (vector12 ^ vector22) % maxValue;
                break;
            default:
                fail("unexpected operator");
                break;
        }

        InstructionGeneratorFacade facade = new InstructionGeneratorFacade("main", 1, IntegerType.BOOLEAN, false);

        Instruction vec1 = facade.createAllocate(new VectorType(type, 2)); // TODO: wrong align
        Instruction vec2 = facade.createAllocate(new VectorType(type, 2));
        Instruction vec_res = facade.createAllocate(new VectorType(type, 2));

        vec1 = facade.createLoad(vec1);
        vec1 = facade.createInsertElement(vec1, new IntegerConstant(type, vector11), 0);
        vec1 = facade.createInsertElement(vec1, new IntegerConstant(type, vector12), 1);

        vec2 = facade.createLoad(vec2);
        vec2 = facade.createInsertElement(vec2, new IntegerConstant(type, vector21), 0);
        vec2 = facade.createInsertElement(vec2, new IntegerConstant(type, vector22), 1);

        Instruction retVec = facade.createBinaryOperation(vec1, vec2, operator);

        // // TODO: this version doesn't work yet, so we evaluate the result in another way
        // vec_res = facade.createLoad(vec_res);
        // vec_res = facade.createInsertElement(vec_res, new IntegerConstant(type, result1), 0);
        // vec_res = facade.createInsertElement(vec_res, new IntegerConstant(type, result2), 1);
        //
        // retVec = facade.createCompare(CompareOperator.INT_NOT_EQUAL, vec1, vec2);
        //
        // Instruction retVec1 = facade.createExtractElement(retVec, 0);
        // Instruction retVec2 = facade.createExtractElement(retVec, 1);

        Instruction retVec1 = facade.createExtractElement(retVec, 0);
        Instruction retVec2 = facade.createExtractElement(retVec, 1);

        retVec1 = facade.createCompare(CompareOperator.INT_NOT_EQUAL, retVec1, new IntegerConstant(type, result1));
        retVec2 = facade.createCompare(CompareOperator.INT_NOT_EQUAL, retVec2, new IntegerConstant(type, result2));

        Instruction ret = facade.createBinaryOperation(retVec1, retVec2, BinaryOperator.INT_OR);
        facade.createReturn(ret); // 0=OK, 1=ERROR
        // Checkstyle: resume magic number name check

        System.out.println(ModelToIRVisitor.getIRString(facade.getModel()));
    }
}
