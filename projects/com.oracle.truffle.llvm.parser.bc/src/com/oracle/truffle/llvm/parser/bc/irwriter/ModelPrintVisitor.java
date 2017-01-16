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
package com.oracle.truffle.llvm.parser.bc.irwriter;

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
import com.oracle.truffle.llvm.parser.api.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

public final class ModelPrintVisitor implements ModelVisitor {

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
    }

    private static int getTrueAlignment(int align) {
        return 1 << (align - 1);
    }

    @Override
    public void visit(GlobalConstant constant) {
        printVisitors.print(constant.getName());
        printVisitors.print(" = ");
        printVisitors.print(constant.getLinkage().getIrString());
        printVisitors.print(" constant ");

        // TODO use TypeVisitor
        printVisitors.print(((PointerType) constant.getType()).getPointeeType().toString());
        printVisitors.print(" ");

        if (constant.getValue() == null) {
            printVisitors.print("zeroinitializer");

        } else {
            // TODO use visitor
            printVisitors.print(constant.getValue().toString());
        }

        printVisitors.print(", align ");
        printVisitors.println(String.valueOf(getTrueAlignment(constant.getAlign())));
    }

    @Override
    public void visit(GlobalVariable variable) {
        printVisitors.print(variable.getName());
        printVisitors.print(" = ");
        printVisitors.print(variable.getLinkage().getIrString());
        printVisitors.print(" global ");

        // TODO use TypeVisitor
        printVisitors.print(((PointerType) variable.getType()).getPointeeType().toString());
        printVisitors.print(" ");

        if (variable.getValue() == null) {
            printVisitors.print("zeroinitializer");

        } else {
            // TODO use visitor
            printVisitors.print(variable.getValue().toString());
        }

        printVisitors.print(", align ");
        printVisitors.println(String.valueOf(getTrueAlignment(variable.getAlign())));
    }

    @Override
    public void visit(FunctionDeclaration function) {
        printVisitors.println();
        Stream<String> argumentStream = Arrays.stream(function.getArgumentTypes()).map(Type::toString);
        if (function.isVarArg()) {
            argumentStream = Stream.concat(argumentStream, Stream.of("..."));
        }
        printVisitors.println(String.format("declare %s %s(%s)", function.getReturnType().toString(), function.getName(),
                        argumentStream.collect(Collectors.joining(", "))));
    }

    @Override
    public void visit(FunctionDefinition function) {
        printVisitors.println();
        Stream<String> parameterStream = function.getParameters().stream().map(f -> functionParameterToLLVMIR(f));
        if (function.isVarArg()) {
            parameterStream = Stream.concat(parameterStream, Stream.of("..."));
        }

        printVisitors.println(String.format("define %s %s(%s) {", function.getReturnType().toString(), function.getName(),
                        parameterStream.collect(Collectors.joining(", "))));

        function.accept(printVisitors.getFunctionVisitor());
        printVisitors.println("}");
    }

    @Override
    public void visit(TargetDataLayout layout) {
        final String layoutString = layout.getDataLayout();
        printVisitors.println(String.format("target datalayout = \"%s\"", layoutString));
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
        if ("3.2".equals(LLVMOptions.ENGINE.llvmVersion())) {
            return getIRString(model, LLVMPrintVersion.LLVM_3_2);
        } else {
            LLVMLogger.info(String.format("No explicit LLVMIR-Printer for version %s, falling back to 3.2!", LLVMOptions.ENGINE.llvmVersion()));
            return getIRString(model, LLVMPrintVersion.LLVM_3_2);
        }
    }

    public static String getIRString(Model model, LLVMPrintVersion printVersion) {
        // TODO add Top-Level Structures like TargetDataLayout
        final StringWriter strOut = new StringWriter();
        final LLVMPrintVersion.LLVMPrintVisitors visitors = printVersion.createPrintVisitors(strOut);
        model.accept(visitors.getModelVisitor());
        return strOut.toString();
    }
}
