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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

public class ModelPrintVisitor implements ModelVisitor {

    private final PrintWriter out;

    private final FunctionPrintVisitor functionVisitor;

    public ModelPrintVisitor(PrintWriter out) {
        this.out = out;
        this.functionVisitor = new FunctionPrintVisitor(out);
    }

    public ModelPrintVisitor(Writer out) {
        this(new PrintWriter(out));
    }

    public ModelPrintVisitor(OutputStream out) {
        this(new PrintWriter(out));
    }

    @Override
    public void ifVisitNotOverwritten(Object obj) {
    }

    @Override
    public void visit(GlobalAlias alias) {
        out.println(alias.toString());
        out.println();
    }

    @Override
    public void visit(GlobalConstant constant) {
        out.println(constant.toString());
        out.println();
    }

    @Override
    public void visit(GlobalVariable variable) {
        out.println(variable.toString());
        out.println();
    }

    @Override
    public void visit(FunctionDeclaration function) {
        out.println(function.toString());
        out.println();
    }

    @Override
    public void visit(FunctionDefinition function) {
        out.println(String.format("%s {", function.toString()));
        function.accept(functionVisitor);
        out.println("}");
        out.println();
    }

    @Override
    public void visit(Type type) {
        if (type instanceof StructureType && !((StructureType) type).getName().equals(ValueSymbol.UNKNOWN)) {
            StructureType actualType = (StructureType) type;
            out.println(String.format("%%%s = type %s", actualType.getName(), actualType.toDeclarationString()));
            out.println();
        }
    }

    public static String getIRString(Model model) {
        // TODO add Top-Level Structures like TargetDataLayout
        StringWriter strOut = new StringWriter();
        final ModelPrintVisitor visitor = new ModelPrintVisitor(strOut);
        model.accept(visitor);
        return strOut.toString();
    }
}
