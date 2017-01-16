package com.oracle.truffle.llvm.writer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import com.oracle.truffle.llvm.writer.util.IRWriterUtil;

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

    public static class LLVMPrintVisitors {

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
