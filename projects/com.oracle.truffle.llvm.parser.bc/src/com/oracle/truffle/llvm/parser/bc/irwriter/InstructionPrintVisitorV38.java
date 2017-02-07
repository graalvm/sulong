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

import com.oracle.truffle.llvm.parser.api.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.api.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Call;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

final class InstructionPrintVisitorV38 extends InstructionPrintVisitor {

    InstructionPrintVisitorV38(LLVMPrintVersion.LLVMPrintVisitors visitors, LLVMIRPrinter.PrintTarget target) {
        super(visitors, target);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        out.print(INDENTATION);
        // <result> = getelementptr
        out.print(String.format("%s = %s ", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER));

        // [inbounds]
        if (gep.isInbounds()) {
            out.print(LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS);
            out.print(" ");
        }

        ((PointerType) gep.getBasePointer().getType()).getPointeeType().accept(visitors.getTypeVisitor());
        out.print(", ");

        // <pty>* <ptrval>
        gep.getBasePointer().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(gep.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : gep.getIndices()) {
            out.print(", ");
            sym.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(sym);
        }

        out.println();
    }

    @Override
    public void visit(LoadInstruction load) {
        out.print(INDENTATION);
        out.print(String.format("%s = %s", load.getName(), LLVMIR_LABEL_LOAD));

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            out.print(" ");
            out.print(LLVMIR_LABEL_ATOMIC);
        }

        if (load.isVolatile()) {
            out.print(" ");
            out.print(LLVMIR_LABEL_VOLATILE);
        }

        if (load.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            out.print(" ");
            load.getType().accept(visitors.getTypeVisitor());
            out.print(",");
        }

        out.print(" ");
        load.getSource().getType().accept(visitors.getTypeVisitor());

        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(load.getSource());

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                out.print(" ");
                out.print(LLVMIR_LABEL_SINGLETHREAD);
            }

            out.print(" ");
            out.print(load.getAtomicOrdering().toString()); // sulong specific toString
        }

        if (load.getAlign() != 0) {
            out.print(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (load.getAlign() - 1)));
        }

        out.println();
    }

    @Override
    protected void printFunctionCall(Call call) {
        out.print(LLVMIR_LABEL_CALL);
        out.print(" ");
        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            final FunctionType decl = (FunctionType) call.getCallTarget();

            decl.getReturnType().accept(visitors.getTypeVisitor());

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {

                out.print(" ");
                visitors.getTypeVisitor().printFormalArguments(decl);
            }
            out.print(String.format(" %s", decl.getName()));

        } else if (call.getCallTarget() instanceof LoadInstruction) {
            Type targetType = ((LoadInstruction) call.getCallTarget()).getSource().getType();
            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }

            if (targetType instanceof FunctionType) {
                ((FunctionType) targetType).getReturnType().accept(visitors.getTypeVisitor());
                out.print(" ");
                visitors.getIRWriterUtil().printInnerSymbolValue(call.getCallTarget());

            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }

        } else if (call.getCallTarget() instanceof FunctionParameter) {
            call.getCallTarget().getType().accept(visitors.getTypeVisitor());
            out.print(String.format(" %s ", ((FunctionParameter) call.getCallTarget()).getName()));

        } else if (call.getCallTarget() instanceof InlineAsmConstant) {
            ((InlineAsmConstant) call.getCallTarget()).accept(visitors.getConstantVisitor());

        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        printActualArgs(call);
    }
}
