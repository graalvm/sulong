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
package com.oracle.truffle.llvm.writer.facades;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.truffle.llvm.parser.api.model.ModelModule;
import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.visitors.FunctionVisitor;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;

public class FunctionV32Writer {

    public static String generateLLVM(ModelModule model) {
        final StringBuilder sb = new StringBuilder();

        model.accept(new ModelVisitor() {
            @Override
            public void ifVisitNotOverwritten(Object obj) {
            }

            @Override
            public void visit(GlobalAlias alias) {
                sb.append(alias.toString() + "\n\n");
            }

            @Override
            public void visit(GlobalConstant constant) {
                sb.append(constant.toString() + "\n\n");
            }

            @Override
            public void visit(GlobalVariable variable) {
                sb.append(variable.toString() + "\n\n");
            }

            @Override
            public void visit(FunctionDeclaration function) {
                sb.append("; function declaration: " + function + "\n\n"); // TODO
            }

            @Override
            public void visit(FunctionDefinition function) {
                sb.append(generateLLVM(function) + "\n\n");
            }
        });

        return sb.toString();
    }

    public static String generateLLVM(InstructionGeneratorFacade facade) {
        return generateLLVM(facade.getFunctionDefinition());
    }

    public static String generateLLVM(FunctionDefinition function) {
        final StringBuilder sb = new StringBuilder();

        sb.append(createFunctionHeader(function) + " {\n");

        function.accept(new FunctionVisitor() {
            @Override
            public void visit(InstructionBlock block) {
                sb.append("; <label>:" + block.getName().substring(1) + "\n");
                for (Instruction inst : block.getInstructions()) {
                    sb.append("    "); // spaces at instruction start
                    sb.append(inst.toString());
                    sb.append("\n");
                }
            }
        });

        sb.append("}");
        return sb.toString();
    }

    public static String createFunctionHeader(FunctionDefinition function) {
        List<String> sb = new ArrayList<>();
        sb.add("define");
        sb.add(function.getReturnType().toString());

        List<String> args = new ArrayList<>();
        for (FunctionParameter fArg : function.getParameters()) {
            args.add(fArg.getType() + " " + fArg.getName());
        }
        if (function.isVarArg()) {
            args.add("...");
        }

        sb.add(function.getName() + "(" + args.stream().collect(Collectors.joining(", ")) + ")");
        return sb.stream().collect(Collectors.joining(" "));
    }
}
