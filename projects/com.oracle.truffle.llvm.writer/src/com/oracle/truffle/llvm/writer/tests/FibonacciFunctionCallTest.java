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
package com.oracle.truffle.llvm.writer.tests;

import org.junit.Test;

import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;
import com.oracle.truffle.llvm.writer.facades.ModelModuleFacade;

public class FibonacciFunctionCallTest extends TestExecutor {

    @Test
    public void test() {
        ModelModuleFacade model = new ModelModuleFacade();

        InstructionGeneratorFacade fibFacade = model.createFunctionDefinition("fibonacci", 3, IntegerType.INTEGER,
                        new Type[]{IntegerType.INTEGER}, false);
        InstructionGeneratorFacade mainFacade = model.createFunctionDefinition("main", 1, IntegerType.INTEGER, new Type[]{}, false);

        Instruction fib_res = mainFacade.createCall(fibFacade.getFunctionDefinition(),
                        new Symbol[]{new IntegerConstant(IntegerType.INTEGER, 10)});
        mainFacade.createReturn(fib_res);

        FunctionParameter param1 = fibFacade.createParameter(IntegerType.INTEGER);

        Instruction cmp1 = fibFacade.createCompare(CompareOperator.INT_SIGNED_LESS_OR_EQUAL, param1, new IntegerConstant(IntegerType.INTEGER, 1));
        fibFacade.createBranch(cmp1, 1, 2);

        fibFacade.nextBlock();
        fibFacade.createReturn(param1);

        fibFacade.nextBlock();
        Instruction fib1Pos = fibFacade.createBinaryOperation(param1, new IntegerConstant(IntegerType.INTEGER, 1), BinaryOperator.INT_SUBTRACT);
        Instruction fib1 = fibFacade.createCall(fibFacade.getFunctionDefinition(), new Symbol[]{fib1Pos});

        Instruction fib2Pos = fibFacade.createBinaryOperation(param1, new IntegerConstant(IntegerType.INTEGER, 2), BinaryOperator.INT_SUBTRACT);
        Instruction fib2 = fibFacade.createCall(fibFacade.getFunctionDefinition(), new Symbol[]{fib2Pos});

        Instruction res = fibFacade.createBinaryOperation(fib1, fib2, BinaryOperator.INT_ADD);

        fibFacade.createReturn(res);

        testModel(model, 55);
    }

}
