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
package com.oracle.truffle.llvm.writer.examples;

import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.VectorType;
import com.oracle.truffle.llvm.parser.bc.util.writer.ModelToIRVisitor;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;

public class VectorBinaryOperationExample {

    public static void main(String[] args) {
        IntegerType[] testedTypes = new IntegerType[]{
                        IntegerType.BYTE,
                        IntegerType.SHORT,
                        IntegerType.INTEGER,
                        IntegerType.LONG
        };

        BinaryOperator[] testedOp = new BinaryOperator[]{
                        BinaryOperator.INT_ADD,
                        BinaryOperator.INT_SUBTRACT,
                        BinaryOperator.INT_MULTIPLY,
                        BinaryOperator.INT_UNSIGNED_DIVIDE,
                        BinaryOperator.INT_SIGNED_DIVIDE,
                        BinaryOperator.INT_UNSIGNED_REMAINDER,
                        BinaryOperator.INT_SIGNED_REMAINDER,
                        BinaryOperator.INT_SHIFT_LEFT,
                        BinaryOperator.INT_LOGICAL_SHIFT_RIGHT,
                        BinaryOperator.INT_ARITHMETIC_SHIFT_RIGHT,
                        BinaryOperator.INT_ADD,
                        BinaryOperator.INT_OR,
                        BinaryOperator.INT_XOR
        };

        for (IntegerType type : testedTypes) {
            for (BinaryOperator op : testedOp) {
                System.out.println("\n;Test: " + type + " - " + op);
                System.out.println("######################");
                createBinaryVectorTest(type, op);
                System.out.println("######################");
            }
        }
    }

    // Checkstyle: stop magic number name check
    public static void createBinaryVectorTest(IntegerType type, BinaryOperator op) {
        InstructionGeneratorFacade facade = new InstructionGeneratorFacade("main", 1, type, false);

        Instruction curSymbol;
        curSymbol = facade.createAllocate(new VectorType(type, 4));
        curSymbol = facade.createLoad(curSymbol);
        curSymbol = facade.createInsertelement(curSymbol, new IntegerConstant(type, 2), 0);
        curSymbol = facade.createInsertelement(curSymbol, new IntegerConstant(type, 3), 1);
        curSymbol = facade.createInsertelement(curSymbol, new IntegerConstant(type, 7), 2);
        curSymbol = facade.createInsertelement(curSymbol, new IntegerConstant(type, 17), 3);
        curSymbol = facade.createBinaryOperation(curSymbol, curSymbol, op);
        curSymbol = facade.createExtractelement(curSymbol, 0);
        facade.createReturn(curSymbol);

        System.out.println(ModelToIRVisitor.getIRString(facade.getModel()));
    }
    // Checkstyle: resume magic number name check

}
