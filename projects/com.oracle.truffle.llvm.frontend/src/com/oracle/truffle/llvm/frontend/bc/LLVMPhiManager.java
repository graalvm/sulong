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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.man.cs.llvm.ir.model.*;
import uk.ac.man.cs.llvm.ir.model.elements.*;
import uk.ac.man.cs.llvm.ir.types.Type;

public final class LLVMPhiManager implements ModelVisitor  {

    public static LLVMPhiManager generate(Model model) {
        LLVMPhiManager visitor = new LLVMPhiManager();

        model.accept(visitor);

        return visitor;
    }

    private final Map<String, Map<Block, List<Phi>>> edges = new HashMap();

    private LLVMPhiManager() {
    }

    public Map<Block, List<Phi>> getPhiMap(String method) {
        Map<Block, List<Phi>> references = edges.get(method);
        if (references == null) {
            return Collections.emptyMap();
        } else {
            return references;
        }
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
        LLVMPhiManagerFunctionVisitor visitor = new LLVMPhiManagerFunctionVisitor();

        method.accept(visitor);

        edges.put(method.getName(), visitor.getEdges());
    }

    @Override
    public void visit(Type type) {
    }

    private static class LLVMPhiManagerFunctionVisitor implements FunctionVisitor, InstructionVisitor {

        private final Map<Block, List<Phi>> edges = new HashMap<>();

        private Block block = null;

        public LLVMPhiManagerFunctionVisitor() {
        }

        public Map<Block, List<Phi>> getEdges() {
            return edges;
        }

        @Override
        public void visit(Block block) {
            this.block = block;
            block.accept(this);
        }

        @Override
        public void visit(AllocateInstruction ai) {
        }

        @Override
        public void visit(BinaryOperationInstruction boi) {
        }

        @Override
        public void visit(BranchInstruction bi) {
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
        public void visit(ConditionalBranchInstruction cbi) {
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
        public void visit(IndirectBranchInstruction ibi) {
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
        public void visit(PhiInstruction phi) {
            for (int i = 0; i < phi.getSize(); i++) {
                Block blk = phi.getBlock(i);
                List<Phi> references = edges.get(blk);
                if (references == null) {
                    references = new ArrayList<>();
                    edges.put(blk, references);
                }
                references.add(new Phi(block, phi, phi.getValue(i)));
            }
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
        }

        @Override
        public void visit(SwitchOldInstruction si) {
        }

        @Override
        public void visit(UnreachableInstruction ui) {
        }

        @Override
        public void visit(VoidCallInstruction vci) {
        }
    }

    public static final class Phi {

        private final Block block;

        private final ValueSymbol phi;

        private final Symbol value;

        public Phi(Block block, ValueSymbol phi, Symbol value) {
            this.block = block;
            this.phi = phi;
            this.value = value;
        }

        public Block getBlock() {
            return block;
        }

        public ValueSymbol getPhiValue() {
            return phi;
        }

        public Symbol getValue() {
            return value;
        }
    }
}
