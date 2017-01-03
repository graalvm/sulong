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

import org.junit.Test;

import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.FunctionType;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.PointerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;
import com.oracle.truffle.llvm.writer.facades.ModelModuleFacade;

public class FunctionPointerCallTest extends TestExecutor {

    private final IntegerType functionType = IntegerType.INTEGER;

    @Test
    public void test() {
        ModelModuleFacade model = new ModelModuleFacade();

        // Checkstyle: stop magic number name check

        BinaryOperator[] operators = new BinaryOperator[]{BinaryOperator.INT_ADD,
                        BinaryOperator.INT_SUBTRACT,
                        BinaryOperator.INT_MULTIPLY,
                        BinaryOperator.INT_SIGNED_DIVIDE};

        InstructionGeneratorFacade[] functions = new InstructionGeneratorFacade[operators.length];

        FunctionType callType = new FunctionType(functionType, new Type[]{functionType, functionType}, false);

        for (int i = 0; i < functions.length; i++) {
            functions[i] = model.createFunctionDefinition("foo_" + operators[i].toString(), 1, callType);
            Symbol lhs = functions[i].createParameter(functionType);
            Symbol rhs = functions[i].createParameter(functionType);
            Symbol result = functions[i].createBinaryOperation(lhs, rhs, operators[i]);
            functions[i].createReturn(result);
            functions[i].exitFunction();
        }

        InstructionGeneratorFacade mainFacade = model.createFunctionDefinition("main", 1, IntegerType.INTEGER, new Type[]{}, false);

        Instruction fp = mainFacade.createAllocate(new PointerType(callType));
        mainFacade.createStore(fp, functions[0].getFunctionDefinition(), 8);

        fp = mainFacade.createLoad(fp);

        Instruction fooRet = mainFacade.createCall(fp, new Symbol[]{new IntegerConstant(functionType, 0), new IntegerConstant(functionType, 0)});

        mainFacade.createReturn(fooRet);
        mainFacade.exitFunction();

        // Checkstyle: resume magic number name check

        testModel(model, 0);
    }

}
