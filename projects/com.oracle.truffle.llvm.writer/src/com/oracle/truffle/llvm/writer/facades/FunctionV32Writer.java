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

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ConditionalBranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.api.model.visitors.FunctionVisitor;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitorAdapter;

public class FunctionV32Writer {

    public static String generateLLVM(InstructionGeneratorFacade facade) {
        return generateLLVM(facade.getFunctionDefinition());
    }

    public static String generateLLVM(FunctionDefinition function) {
        final StringBuilder sb = new StringBuilder();
        final Map<Symbol, String> labels = new HashMap<>();

        sb.append(createFunctionHeader(function, labels) + " {\n");

        function.accept(new FunctionVisitor() {
            InstructionToLLVMIRVisitor instructionVisitor = new InstructionToLLVMIRVisitor(sb, labels);

            @Override
            public void visit(InstructionBlock block) {
                sb.append(createBranchLabel(block) + ":\n");
                block.accept(instructionVisitor);
            }
        });

        sb.append("}");
        return sb.toString();
    }

    public static String createFunctionHeader(FunctionDefinition function, Map<Symbol, String> labels) {
        List<String> sb = new ArrayList<>();
        sb.add("define");
        sb.add(function.getReturnType().toString());

        List<String> args = new ArrayList<>();
        for (FunctionParameter fArg : function.getParameters()) {
            args.add(fArg.getType() + " " + addLabelToSymbolsMap(labels, fArg));
        }
        if (function.isVarArg()) {
            args.add("...");
        }

        sb.add(function.getName() + "(" + args.stream().collect(Collectors.joining(", ")) + ")");
        return sb.stream().collect(Collectors.joining(" "));
    }

    private static String addLabelToSymbolsMap(Map<Symbol, String> labels, Symbol curSymbol) {
        // we simply use named variables, so we don't have to worry about enumeration
        String returnVar = "%sym_" + (labels.size() + 1);
        labels.put(curSymbol, returnVar);
        return returnVar;
    }

    private static String createBranchLabel(InstructionBlock block) {
        // we simply use named blocks, so we don't have to worry about enumeration
        return "block_" + block.getBlockIndex();
    }

    public static class InstructionToLLVMIRVisitor implements InstructionVisitorAdapter {
        private final StringBuilder llvmir;
        final Map<Symbol, String> labels;

        public InstructionToLLVMIRVisitor(StringBuilder llvmir, Map<Symbol, String> labels) {
            this.llvmir = llvmir;
            this.labels = labels;
        }

        private void addInstructionString(String str) {
            llvmir.append("    "); // spaces at start
            llvmir.append(str);
            llvmir.append("\n");
        }

        private String newLabel(Symbol curInstr) {
            return addLabelToSymbolsMap(labels, curInstr);
        }

        private String createSymbolLabel(Symbol s) {
            return s.getType() + " " + createSymbolLabelWithoutType(s);
        }

        private String createSymbolLabelWithoutType(Symbol s) {
            if (s instanceof Constant) {
                return s.toString(); // TODO: needs check
            } else {
                return labels.get(s);
            }
        }

        private static String createSymbolLabel(InstructionBlock s) {
            return "label %" + createBranchLabel(s);
        }

        @Override
        public void visit(AllocateInstruction allocate) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(allocate) + " =");
            sb.add(AllocateInstruction.LLVMIR_LABEL);
            sb.add(allocate.getPointeeType() + ",");
            sb.add("align " + allocate.getAlign());
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(BinaryOperationInstruction operation) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(operation) + " =");
            sb.add(operation.getOperator().toString());
            sb.add(operation.getType().toString());
            sb.add(createSymbolLabelWithoutType(operation.getLHS()) + ",");
            sb.add(createSymbolLabelWithoutType(operation.getRHS()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(BranchInstruction branch) {
            List<String> sb = new ArrayList<>();
            sb.add(BranchInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(branch.getSuccessor()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(ConditionalBranchInstruction branch) {
            List<String> sb = new ArrayList<>();
            sb.add(ConditionalBranchInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(branch.getCondition()) + ",");
            sb.add(createSymbolLabel(branch.getTrueSuccessor()) + ",");
            sb.add(createSymbolLabel(branch.getFalseSuccessor()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(ExtractElementInstruction extract) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(extract) + " =");
            sb.add(ExtractElementInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(extract.getVector()) + ",");
            sb.add(createSymbolLabel(extract.getIndex()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(InsertElementInstruction insert) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(insert) + " =");
            sb.add(InsertElementInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(insert.getVector()) + ",");
            sb.add(createSymbolLabel(insert.getValue()) + ",");
            sb.add(createSymbolLabel(insert.getIndex()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(LoadInstruction load) {
            List<String> sb = new ArrayList<>();
            sb.add(newLabel(load) + " =");
            sb.add(LoadInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(load.getSource()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }

        @Override
        public void visit(ReturnInstruction ret) {
            List<String> sb = new ArrayList<>();
            sb.add(ReturnInstruction.LLVMIR_LABEL);
            sb.add(createSymbolLabel(ret.getValue()));
            addInstructionString(sb.stream().collect(Collectors.joining(" ")));
        }
    }
}
