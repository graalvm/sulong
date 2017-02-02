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

enum LLVMPrintVersion {
    LLVM_3_2(
                    ModelPrintVisitor::new,
                    FunctionPrintVisitor::new,
                    InstructionPrintVisitor::new,
                    ConstantPrintVisitor::new,
                    TypePrintVisitor::new),
    LLVM_3_8(
                    ModelPrintVisitorV38::new,
                    FunctionPrintVisitor::new,
                    InstructionPrintVisitorV38::new,
                    ConstantPrintVisitorV38::new,
                    TypePrintVisitor::new),
    DEFAULT(
                    ModelPrintVisitor::new,
                    FunctionPrintVisitor::new,
                    InstructionPrintVisitor::new,
                    ConstantPrintVisitor::new,
                    TypePrintVisitor::new);

    @FunctionalInterface
    private interface ModelPrinter {
        ModelPrintVisitor instantiate(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target);
    }

    @FunctionalInterface
    private interface FunctionPrinter {
        FunctionPrintVisitor instantiate(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target);
    }

    @FunctionalInterface
    private interface InstructionPrinter {
        InstructionPrintVisitor instantiate(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target);
    }

    @FunctionalInterface
    private interface ConstantPrinter {
        ConstantPrintVisitor instantiate(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target);
    }

    @FunctionalInterface
    private interface TypePrinter {
        TypePrintVisitor instantiate(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target);
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

    private ModelPrintVisitor createModelPrintVisitor(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target) {
        return modelVisitor.instantiate(out, target);
    }

    private FunctionPrintVisitor createFunctionPrintVisitor(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target) {
        return functionVisitor.instantiate(out, target);
    }

    private InstructionPrintVisitor createInstructionPrintVisitor(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target) {
        return instructionVisitor.instantiate(out, target);
    }

    private ConstantPrintVisitor createConstantPrintVisitor(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target) {
        return constantVisitor.instantiate(out, target);
    }

    private TypePrintVisitor createTypePrintVisitor(LLVMPrintVisitors out, LLVMIRPrinter.PrintTarget target) {
        return typeVisitor.instantiate(out, target);
    }

    LLVMPrintVisitors createPrintVisitors(LLVMIRPrinter.PrintTarget out) {
        return new LLVMPrintVisitors(this, out);
    }

    static final class LLVMPrintVisitors {

        private final ModelPrintVisitor modelVisitor;
        private final FunctionPrintVisitor functionVisitor;
        private final InstructionPrintVisitor instructionVisitor;
        private final ConstantPrintVisitor constantVisitor;
        private final TypePrintVisitor typeVisitor;

        private final IRWriterUtil irWriter;

        private LLVMPrintVisitors(LLVMPrintVersion version, LLVMIRPrinter.PrintTarget target) {
            this.modelVisitor = version.createModelPrintVisitor(this, target);
            this.functionVisitor = version.createFunctionPrintVisitor(this, target);
            this.instructionVisitor = version.createInstructionPrintVisitor(this, target);
            this.constantVisitor = version.createConstantPrintVisitor(this, target);
            this.typeVisitor = version.createTypePrintVisitor(this, target);
            this.irWriter = new IRWriterUtil(this, target);
        }

        ModelPrintVisitor getModelVisitor() {
            return modelVisitor;
        }

        FunctionPrintVisitor getFunctionVisitor() {
            return functionVisitor;
        }

        InstructionPrintVisitor getInstructionVisitor() {
            return instructionVisitor;
        }

        ConstantPrintVisitor getConstantVisitor() {
            return constantVisitor;
        }

        TypePrintVisitor getTypeVisitor() {
            return typeVisitor;
        }

        IRWriterUtil getIRWriterUtil() {
            return irWriter;
        }
    }
}
