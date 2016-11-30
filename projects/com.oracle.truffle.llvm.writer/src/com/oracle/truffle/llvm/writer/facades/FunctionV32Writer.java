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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.truffle.llvm.parser.base.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.base.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.base.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.base.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.base.model.visitors.FunctionVisitor;
import com.oracle.truffle.llvm.parser.base.model.visitors.InstructionVisitorAdapter;

public class FunctionV32Writer {

    public static String generateLLVM(InstructionGeneratorFacade facade) {
        return generateLLVM(facade.getFunctionDefinition());
    }

    public static String generateLLVM(FunctionDefinition function) {
        final StringBuilder sb = new StringBuilder();
        sb.append(createFunctionHeader(function) + " {\n");

        function.accept(new FunctionVisitor() {
            InstructionToLLVMIRVisitor instructionVisitor = new InstructionToLLVMIRVisitor(sb);

            @Override
            public void visit(InstructionBlock block) {
                block.accept(instructionVisitor);
                sb.append("; block_end: " + block.getBlockIndex() + "\n");
            }
        });

        sb.append("}");
        return sb.toString();
    }

    public static String createFunctionHeader(FunctionDefinition function) {
        List<String> sb = new ArrayList<>();
        sb.add("define");
        sb.add(function.getReturnType().toString());
        assert function.getParameters().isEmpty(); // TODO: not implemented yet
        assert function.isVarArg() == false; // TODO: not implemented yet
        sb.add(function.getName() + "()");
        return sb.stream().collect(Collectors.joining(" "));
    }

    public static class InstructionToLLVMIRVisitor implements InstructionVisitorAdapter {
        private final StringBuilder llvmir;
        final Map<Instruction, String> labels = new HashMap<>();

        public InstructionToLLVMIRVisitor(StringBuilder llvmir) {
            this.llvmir = llvmir;
        }

        private void addInstructionString(String str) {
            llvmir.append("    "); // spaces at start
            llvmir.append(str);
            llvmir.append("\n");
        }

        private String newLabel(Instruction curInstr) {
            String returnVar = "%" + (labels.size() + 1);
            labels.put(curInstr, returnVar);
            return returnVar;
        }

        private String createSymbolLabel(Symbol s) {
            if (s instanceof Constant) {
                return s.getType() + " " + s.toString(); // TODO: needs check
            } else {
                return s.getType() + " " + labels.get(s);
            }
        }

        @Override
        public void visit(AllocateInstruction allocate) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(allocate) + " =");
            sb.add("alloca");
            sb.add(allocate.getPointeeType() + ",");
            sb.add("align " + allocate.getAlign());
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(BinaryOperationInstruction operation) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(operation) + " =");
            sb.add(operation.getOperator().getName());
            sb.add(operation.getType().toString());
            sb.add(labels.get(operation.getLHS()) + ",");
            sb.add(labels.get(operation.getRHS()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(ExtractElementInstruction extract) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(extract) + " =");
            sb.add("extractelement");
            sb.add(createSymbolLabel(extract.getVector()) + ",");
            sb.add(createSymbolLabel(extract.getIndex()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(InsertElementInstruction insert) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(insert) + " =");
            sb.add("insertelement");
            sb.add(createSymbolLabel(insert.getVector()) + ",");
            sb.add(createSymbolLabel(insert.getValue()) + ",");
            sb.add(createSymbolLabel(insert.getIndex()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(LoadInstruction load) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(load) + " =");
            sb.add("load");
            sb.add(createSymbolLabel(load.getSource()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(ReturnInstruction ret) {
            List<String> sb = new ArrayList<>();
            sb.add("ret");
            sb.add(createSymbolLabel(ret.getValue()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }
    }
}
