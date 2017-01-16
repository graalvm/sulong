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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public enum LLVMPrintVersion {
    LLVM_3_2(
                    ModelPrintVisitor::new,
                    FunctionPrintVisitor::new,
                    InstructionV32PrintVisitor::new,
                    ConstantPrintVisitor::new,
                    TypePrintVisitor::new),
    DEFAULT(
                    ModelPrintVisitor::new,
                    FunctionPrintVisitor::new,
                    InstructionV32PrintVisitor::new,
                    ConstantPrintVisitor::new,
                    TypePrintVisitor::new);

    @FunctionalInterface
    private interface ModelPrinter {
        ModelPrintVisitor instantiate(LLVMPrintVisitors out);
    }

    @FunctionalInterface
    private interface FunctionPrinter {
        FunctionPrintVisitor instantiate(LLVMPrintVisitors out);
    }

    @FunctionalInterface
    private interface InstructionPrinter {
        InstructionV32PrintVisitor instantiate(LLVMPrintVisitors out);
    }

    @FunctionalInterface
    private interface ConstantPrinter {
        ConstantPrintVisitor instantiate(LLVMPrintVisitors out);
    }

    @FunctionalInterface
    private interface TypePrinter {
        TypePrintVisitor instantiate(LLVMPrintVisitors out);
    }

    private final ModelPrinter modelVisitor;
    private final FunctionPrinter functionVisitor;
    private final InstructionPrinter instructionVisitor;
    private final ConstantPrinter constantVisitor;
    private final TypePrinter typeVisitor;

    LLVMPrintVersion(ModelPrinter modelVisitor, FunctionPrinter functionVisitor, InstructionPrinter instructionVisitor, ConstantPrinter constantVisitor,
                    TypePrinter typeVisitor) {
        this.modelVisitor = modelVisitor;
        this.functionVisitor = functionVisitor;
        this.instructionVisitor = instructionVisitor;
        this.constantVisitor = constantVisitor;
        this.typeVisitor = typeVisitor;
    }

    public ModelPrintVisitor createModelPrintVisitor(LLVMPrintVisitors out) {
        return modelVisitor.instantiate(out);
    }

    public FunctionPrintVisitor createFunctionPrintVisitor(LLVMPrintVisitors out) {
        return functionVisitor.instantiate(out);
    }

    public InstructionV32PrintVisitor createInstructionPrintVisitor(LLVMPrintVisitors out) {
        return instructionVisitor.instantiate(out);
    }

    public ConstantPrintVisitor createConstantPrintVisitor(LLVMPrintVisitors out) {
        return constantVisitor.instantiate(out);
    }

    public TypePrintVisitor createTypePrintVisitor(LLVMPrintVisitors out) {
        return typeVisitor.instantiate(out);
    }

    public LLVMPrintVisitors createPrintVisitors(PrintWriter out) {
        return new LLVMPrintVisitors(this, out);
    }

    public LLVMPrintVisitors createPrintVisitors(Writer out) {
        return new LLVMPrintVisitors(this, new PrintWriter(out));
    }

    public LLVMPrintVisitors createPrintVisitors(OutputStream out) {
        return new LLVMPrintVisitors(this, new PrintWriter(out));
    }

    public static final class LLVMPrintVisitors {

        private final PrintWriter out;

        private final ModelPrintVisitor modelVisitor;
        private final FunctionPrintVisitor functionVisitor;
        private final InstructionV32PrintVisitor instructionVisitor;
        private final ConstantPrintVisitor constantVisitor;
        private final TypePrintVisitor typeVisitor;

        private final IRWriterUtil irWriter;

        private LLVMPrintVisitors(LLVMPrintVersion version, PrintWriter out) {
            this.out = out;

            // TODO: create instance using this object
            this.modelVisitor = version.createModelPrintVisitor(this);
            this.functionVisitor = version.createFunctionPrintVisitor(this);
            this.instructionVisitor = version.createInstructionPrintVisitor(this);
            this.constantVisitor = version.createConstantPrintVisitor(this);
            this.typeVisitor = version.createTypePrintVisitor(this);

            this.irWriter = new IRWriterUtil(this);
        }

        public PrintWriter getPrintWriter() {
            return out;
        }

        public ModelPrintVisitor getModelVisitor() {
            return modelVisitor;
        }

        public FunctionPrintVisitor getFunctionVisitor() {
            return functionVisitor;
        }

        public InstructionV32PrintVisitor getInstructionVisitor() {
            return instructionVisitor;
        }

        public ConstantPrintVisitor getConstantVisitor() {
            return constantVisitor;
        }

        public TypePrintVisitor getTypeVisitor() {
            return typeVisitor;
        }

        public IRWriterUtil getIRWriterUtil() {
            return irWriter;
        }

        public void print(Object obj) {
            out.print(obj);
        }

        public void print(String s) {
            out.print(s);
        }

        public void println() {
            out.println();
        }

        public void println(Object obj) {
            out.println(obj);
        }

        public void println(String s) {
            out.println(s);
        }
    }
}
