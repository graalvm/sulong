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

import com.oracle.truffle.llvm.parser.api.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.api.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.api.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.api.model.target.ModuleID;
import com.oracle.truffle.llvm.parser.api.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

class ModelPrintVisitor implements ModelVisitor {

    protected final LLVMPrintVersion.LLVMPrintVisitors visitors;

    protected final LLVMIRPrinter.PrintTarget out;

    ModelPrintVisitor(LLVMPrintVersion.LLVMPrintVisitors visitors, LLVMIRPrinter.PrintTarget target) {
        this.visitors = visitors;
        this.out = target;
    }

    @Override
    public void ifVisitNotOverwritten(Object obj) {
    }

    static final String UNRESOLVED_FORWARD_REFERENCE = "<unresolved>";

    @Override
    public void visit(GlobalAlias alias) {
        // sulong specific toString
        out.print(String.format("%s = alias %s", alias.getName(), alias.getLinkage()));
        if (alias.getVisibility() != Visibility.DEFAULT) {
            // sulong specific toString
            out.print(String.format(" %s", alias.getVisibility().toString()));
        }

        out.print(" ");
        alias.getType().accept(visitors.getTypeVisitor());

        out.print(" ");
        final Symbol val = alias.getValue();
        if (val == null) {
            out.print(UNRESOLVED_FORWARD_REFERENCE);
        } else {
            visitors.getIRWriterUtil().printInnerSymbolValue(alias.getValue());
        }
        out.println();
    }

    @Override
    public void visit(GlobalConstant constant) {
        out.print(constant.getName());
        out.print(" = ");
        if (constant.getVisibility() != Visibility.DEFAULT) {
            out.print(constant.getVisibility().toString()); // sulong specific toString
            out.print(" ");
        }

        if (constant.getLinkage() != Linkage.EXTERNAL || constant.getValue() == null) {
            out.print(constant.getLinkage().getIrString()); // sulong specific toString
            out.print(" ");
        }

        out.print("constant ");

        ((PointerType) constant.getType()).getPointeeType().accept(visitors.getTypeVisitor());

        if (constant.getLinkage() == Linkage.EXTERNAL && constant.getValue() == null) {
            out.println();
            return;
        }

        out.print(" ");

        if (constant.getValue() != null) {

            if (constant.getValue() instanceof NullConstant) {
                out.print("zeroinitializer");

            } else if (constant.getValue() instanceof Constant) {
                ((Constant) constant.getValue()).accept(visitors.getConstantVisitor());
            } else {
                throw new AssertionError("Cannot print Global Constant with non-constant value: " + constant.getValue());
            }

        }

        if (constant.getAlign() > 1) {
            out.print(", align ");
            out.print(String.valueOf(1 << (constant.getAlign() - 1)));
        }

        out.println();
    }

    @Override
    public void visit(GlobalVariable variable) {
        out.print(variable.getName());
        out.print(" = ");
        if (variable.getVisibility() != Visibility.DEFAULT) {
            out.print(variable.getVisibility().toString()); // sulong specific toString
            out.print(" ");
        }

        if (variable.getLinkage() != Linkage.EXTERNAL || variable.getValue() == null) {
            out.print(variable.getLinkage().getIrString()); // sulong specific toString
            out.print(" ");
        }

        out.print("global ");

        ((PointerType) variable.getType()).getPointeeType().accept(visitors.getTypeVisitor());
        out.print(" ");

        if (variable.getValue() != null) {

            if (variable.getValue() instanceof NullConstant) {
                out.print("zeroinitializer");

            } else if (variable.getValue() instanceof Constant) {
                ((Constant) variable.getValue()).accept(visitors.getConstantVisitor());

            } else if (variable.getValue() instanceof ValueSymbol) {
                out.print(((ValueSymbol) variable.getValue()).getName());

            } else {
                throw new IllegalStateException("Cannot print Global with value: " + variable.getValue());
            }

        }

        if (variable.getAlign() > 1) {
            out.print(", align ");
            out.print(String.valueOf(1 << (variable.getAlign() - 1)));
        }

        out.println();
    }

    @Override
    public void visit(FunctionDeclaration function) {
        out.println();

        out.print("declare ");
        function.getReturnType().accept(visitors.getTypeVisitor());

        out.print(String.format(" %s", function.getName()));

        visitors.getTypeVisitor().printFormalArguments(function);
        out.println();
    }

    @Override
    public void visit(FunctionDefinition function) {
        out.println();

        out.print("define ");
        function.getReturnType().accept(visitors.getTypeVisitor());

        out.print(String.format(" %s", function.getName()));

        out.print("(");

        boolean firstIteration = true;
        for (FunctionParameter param : function.getParameters()) {
            if (!firstIteration) {
                out.print(", ");
            } else {
                firstIteration = false;
            }
            param.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            out.print(param.getName());
        }

        if (function.isVarArg()) {
            if (!firstIteration) {
                out.print(", ");
            }

            out.print("...");
        }

        out.print(")");

        out.println(" {");
        function.accept(visitors.getFunctionVisitor());
        out.println("}");
    }

    @Override
    public void visit(ModuleID moduleID) {
        out.println(String.format("; ModuleID = \'%s\'", moduleID.getModuleID()));
        out.println();
    }

    @Override
    public void visit(TargetDataLayout layout) {
        out.println(String.format("target datalayout = \"%s\"", layout.getDataLayout()));
        out.println();
    }

    @Override
    public void visit(Type type) {
        if (type instanceof StructureType && !((StructureType) type).getName().equals(ValueSymbol.UNKNOWN)) {
            final StructureType actualType = (StructureType) type;
            out.print(String.format("%%%s = type ", actualType.getName()));
            visitors.getTypeVisitor().printStructDeclaration(actualType);
            out.println();
        }
    }
}
