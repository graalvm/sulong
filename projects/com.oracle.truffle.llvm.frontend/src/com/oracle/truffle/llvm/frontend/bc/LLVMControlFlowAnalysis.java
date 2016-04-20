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
package com.oracle.truffle.llvm.frontend.bc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uk.ac.man.cs.llvm.ir.model.*;
import uk.ac.man.cs.llvm.ir.model.elements.*;
import uk.ac.man.cs.llvm.ir.types.Type;

public final class LLVMControlFlowAnalysis {

    public static LLVMControlFlowAnalysis generate(Model model) {
        LLVMControlFlowVisitor visitor = new LLVMControlFlowVisitor();

        model.accept(visitor);

        return new LLVMControlFlowAnalysis(visitor.dependencies());
    }

    private final Map<String, LLVMControlFlow> dependencies;

    public LLVMControlFlowAnalysis(Map<String, LLVMControlFlow> dependencies) {
        this.dependencies = dependencies;
    }

    public LLVMControlFlow dependencies(String method) {
        return dependencies.get(method);
    }

    public static final class LLVMControlFlow {

        private final Map<Block, Set<Block>> predecessors;

        private final Map<Block, Set<Block>> successors;

        public LLVMControlFlow(Map<Block, Set<Block>> predecessors, Map<Block, Set<Block>> successors) {
            this.predecessors = predecessors;
            this.successors = successors;
        }

        public Set<Block> predecessor(Block block) {
            Set<Block> set = predecessors.get(block);
            return set == null ? Collections.EMPTY_SET : set;
        }

        public Set<Block> successor(Block block) {
            Set<Block> set = successors.get(block);
            return set == null ? Collections.EMPTY_SET : set;
        }
    }

    private static class LLVMControlFlowVisitor implements ModelVisitor {

        private final Map<String, LLVMControlFlow> dependencies = new HashMap<>();

        public LLVMControlFlowVisitor() {
        }

        public Map<String, LLVMControlFlow> dependencies() {
            return dependencies;
        }

        @Override
        public void visit(GlobalConstant constant) {
        }

        @Override
        public void visit(GlobalVariable variable) {
        }

        @Override
        public void visit(FunctionDeclaration method) {
        }

        @Override
        public void visit(FunctionDefinition method) {
            LLVMControlFlowFunctionVisitor successors = new LLVMControlFlowFunctionVisitor();

            method.accept(successors);

            dependencies.put(method.getName(), new LLVMControlFlow(successors.predecessors(), successors.successors()));
        }

        @Override
        public void visit(Type type) {
        }
    }

    private static class LLVMControlFlowFunctionVisitor implements FunctionVisitor, BlockVisitor {

        private final Map<Block, Set<Block>> predecessors = new HashMap<>();

        private final Map<Block, Set<Block>> successors = new HashMap<>();

        private Set<Block> workspace;
        
        public LLVMControlFlowFunctionVisitor() {
        }

        public Map<Block, Set<Block>> predecessors() {
            if (predecessors.isEmpty()) {
                for (Block blk : successors.keySet()) {
                    for (Block successor : successors.get(blk)) {
                        Set<Block> temp = predecessors.get(successor);
                        if (temp == null) {
                            temp = new HashSet<>();
                            predecessors.put(successor, temp);
                        }
                        temp.add(blk);
                    }
                }
            }
            return predecessors;
        }

        public Map<Block, Set<Block>> successors() {
            return successors;
        }

        @Override
        public void visit(AllocateInstruction ai) {
        }

        @Override
        public void visit(BinaryOperationInstruction boi) {
        }

        @Override
        public void visit(Block block) {
            workspace = new HashSet<>();
            successors.put(block, workspace);
            block.accept(this);
        }

        @Override
        public void visit(BranchInstruction branch) {
            workspace.add(branch.getSuccessor());
        }

        @Override
        public void visit(CallInstruction ci) {
        }

        @Override
        public void visit(CastInstruction ci) {
        }

        @Override
        public void visit(CompareInstruction ci) {
        }

        @Override
        public void visit(ConditionalBranchInstruction branch) {
            workspace.add(branch.getTrueSuccessor());
            workspace.add(branch.getFalseSuccessor());
        }

        @Override
        public void visit(ExtractElementInstruction eei) {
        }

        @Override
        public void visit(ExtractValueInstruction evi) {
        }

        @Override
        public void visit(GetElementPointerInstruction gepi) {
        }

        @Override
        public void visit(IndirectBranchInstruction branch) {
            for (int i = 0; i < branch.getSuccessorCount(); i++) {
                workspace.add(branch.getSuccessor(i));
            }
        }

        @Override
        public void visit(InsertElementInstruction iei) {
        }

        @Override
        public void visit(InsertValueInstruction ivi) {
        }

        @Override
        public void visit(LoadInstruction li) {
        }

        @Override
        public void visit(PhiInstruction pi) {
        }

        @Override
        public void visit(ReturnInstruction ri) {
        }

        @Override
        public void visit(SelectInstruction si) {
        }

        @Override
        public void visit(ShuffleVectorInstruction svi) {
        }

        @Override
        public void visit(StoreInstruction si) {
        }

        @Override
        public void visit(SwitchInstruction si) {
            workspace.add(si.getDefaultBlock());
            for (int i = 0; i < si.getCaseCount(); i++) {
                workspace.add(si.getCaseBlock(i));
            }
        }

        @Override
        public void visit(UnreachableInstruction ui) {
        }

        @Override
        public void visit(VoidCallInstruction vci) {
        }
    }
}
