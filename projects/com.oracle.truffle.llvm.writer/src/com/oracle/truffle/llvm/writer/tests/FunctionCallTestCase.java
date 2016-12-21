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

import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.PointerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;
import com.oracle.truffle.llvm.writer.facades.ModelModuleFacade;

public class FunctionCallTestCase {

    @SuppressWarnings("unused") private final Type functionReturnType;

    public FunctionCallTestCase() {
        functionReturnType = IntegerType.INTEGER;
    }

    @Test
    public void test() {
        ModelModuleFacade model = new ModelModuleFacade();

        // Checkstyle: stop magic number name check

        Symbol str = model.createGlobalStringConstant(".str", "test\n\0");

        FunctionDeclaration printfDecl = model.createFunctionDeclaration("printf", IntegerType.INTEGER, new Type[]{new PointerType(IntegerType.BYTE)}, true);

        InstructionGeneratorFacade mainFacade = model.createFunctionDefinition("main", 1, IntegerType.INTEGER, false);
        InstructionGeneratorFacade fooFacade = model.createFunctionDefinition("foo", 1, IntegerType.INTEGER, false);

        Instruction fooRet = mainFacade.createCall(fooFacade.getFunctionDefinition(), new Symbol[]{});

        Instruction strPtr = mainFacade.createGetElementPointer(new PointerType(IntegerType.BYTE),
                        str,
                        new Symbol[]{new IntegerConstant(IntegerType.INTEGER, 0),
                                        new IntegerConstant(IntegerType.INTEGER, 0)},
                        false);
        mainFacade.createCall(printfDecl, new Symbol[]{strPtr});

        mainFacade.createReturn(fooRet);

        fooFacade.createReturn(new IntegerConstant(IntegerType.INTEGER, 123));

        // Checkstyle: resume magic number name check

        System.out.println(model);
    }

}
