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
package com.oracle.truffle.llvm.writer;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.parser.api.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

public class ModelPrintVisitor implements ModelVisitor {

    private final LLVMPrintVersion.LLVMPrintVisitors printVisitors;

    public ModelPrintVisitor(LLVMPrintVersion.LLVMPrintVisitors printVisitors) {
        this.printVisitors = printVisitors;
    }

    @Override
    public void ifVisitNotOverwritten(Object obj) {
    }

    private static final String UNRESOLVED_FORWARD_REFERENCE = "<unresolved>";

    @Override
    public void visit(GlobalAlias alias) {
        printVisitors.print(String.format("%s = alias %s", alias.getName(), alias.getLinkage()));
        if (alias.getVisibility() != Visibility.DEFAULT) {
            printVisitors.print(String.format(" %s", alias.getVisibility()));
        }
        printVisitors.print(String.format(" %s", alias.getType()));
        printVisitors.println(String.format(" %s", alias.getValue() != null ? alias.getValue() : UNRESOLVED_FORWARD_REFERENCE));
        printVisitors.println();
    }

    @Override
    public void visit(GlobalConstant constant) {
        printVisitors.println(constant.toString());
        printVisitors.println();
    }

    @Override
    public void visit(GlobalVariable variable) {
        printVisitors.println(variable.toString());
        printVisitors.println();
    }

    @Override
    public void visit(FunctionDeclaration function) {
        Stream<String> argumentStream = Arrays.stream(function.getArgumentTypes()).map(Type::toString);
        if (function.isVarArg()) {
            argumentStream = Stream.concat(argumentStream, Stream.of("..."));
        }
        printVisitors.println(String.format("declare %s %s(%s)", function.getReturnType().toString(), function.getName(),
                        argumentStream.collect(Collectors.joining(", "))));
        printVisitors.println();
    }

    @Override
    public void visit(FunctionDefinition function) {

        Stream<String> parameterStream = function.getParameters().stream().map(f -> functionParameterToLLVMIR(f));
        if (function.isVarArg()) {
            parameterStream = Stream.concat(parameterStream, Stream.of("..."));
        }

        printVisitors.println(String.format("define %s %s(%s) {", function.getReturnType().toString(), function.getName(),
                        parameterStream.collect(Collectors.joining(", "))));

        function.accept(printVisitors.getFunctionVisitor());
        printVisitors.println("}");
        printVisitors.println();
    }

    private static String functionParameterToLLVMIR(FunctionParameter param) {
        final StringBuilder builder = new StringBuilder();
        builder.append(param.getType().toString());
        if (!ValueSymbol.UNKNOWN.equals(param.getName())) {
            builder.append(' ').append(param.getName());
        }
        return builder.toString();
    }

    @Override
    public void visit(Type type) {
        if (type instanceof StructureType && !((StructureType) type).getName().equals(ValueSymbol.UNKNOWN)) {
            StructureType actualType = (StructureType) type;
            printVisitors.println(String.format("%%%s = type %s", actualType.getName(), actualType.toDeclarationString()));
            printVisitors.println();
        }
    }

    public static String getIRString(Model model) {
        // TODO add Top-Level Structures like TargetDataLayout
        StringWriter strOut = new StringWriter();
        // TODO: LLVMPrintVersion
        LLVMPrintVersion.LLVMPrintVisitors visitors = LLVMPrintVersion.DEFAULT.createPrintVisitors(strOut);
        model.accept(visitors.getModelVisitor());
        return strOut.toString();
    }
}
