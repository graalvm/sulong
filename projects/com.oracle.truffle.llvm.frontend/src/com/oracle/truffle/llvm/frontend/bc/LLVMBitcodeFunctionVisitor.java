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

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.llvm.nodes.base.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.base.LLVMNode;
import com.oracle.truffle.llvm.nodes.base.LLVMStackFrameNuller;
import com.oracle.truffle.llvm.nodes.base.LLVMStackFrameNuller.*;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMContext;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMTerminatorNode;
import com.oracle.truffle.llvm.frontend.bc.LLVMPhiManager.Phi;
import com.oracle.truffle.llvm.runtime.LLVMOptimizationConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.man.cs.llvm.ir.model.Block;
import uk.ac.man.cs.llvm.ir.model.FunctionVisitor;
import uk.ac.man.cs.llvm.ir.model.GlobalValueSymbol;

public class LLVMBitcodeFunctionVisitor implements FunctionVisitor {

    private final LLVMBitcodeVisitor module;

    private final FrameDescriptor frame;

    private final Map<Block, List<FrameSlot>> slots;

    private final List<LLVMStackFrameNuller[]> nullers = new ArrayList<>();

    private final List<LLVMBasicBlockNode> blocks = new ArrayList<>();

    private final Map<String, LLVMExpressionNode> map = new HashMap();

    private final Map<String, Integer> labels;

    private final Map<Block, List<Phi>> phis;

    private final List<LLVMNode> block = new ArrayList<>();

    public LLVMBitcodeFunctionVisitor(LLVMBitcodeVisitor module, FrameDescriptor frame, Map<Block, List<FrameSlot>> slots, Map<String, Integer> labels, Map<Block, List<Phi>> phis) {
        this.module = module;
        this.frame = frame;
        this.slots = slots;
        this.labels = labels;
        this.phis = phis;
    }

    public void addInstruction(LLVMNode node) {
        block.add(node);
    }

    public void addTerminatingInstruction(LLVMTerminatorNode node) {
        blocks.add(new LLVMBasicBlockNode(getBlock(), node));
        block.add(node);
    }

    public LLVMNode[] getBlock() {
        return block.toArray(new LLVMNode[block.size()]);
    }

    public LLVMBasicBlockNode[] getBlocks() {
        return blocks.toArray(new LLVMBasicBlockNode[blocks.size()]);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public LLVMContext getContext() {
        return module.getContext();
    }

    public FrameDescriptor getFrame() {
        return frame;
    }

    public FrameSlot getReturnSlot() {
        return getSlot(LLVMBitcodeHelper.FUNCTION_RETURN_VALUE_FRAME_SLOT_ID);
    }

    public FrameSlot getSlot(String name) {
        return frame.findFrameSlot(name);
    }

    public FrameSlot getStackSlot() {
        return getSlot(LLVMBitcodeHelper.STACK_ADDRESS_FRAME_SLOT_ID);
    }

    public LLVMExpressionNode global(GlobalValueSymbol symbol) {
        return module.getGlobalVariable(symbol);
    }

    public Map<String, Integer> labels() {
        return labels;
    }

    public LLVMStackFrameNuller[][] getNullers() {
        return nullers.toArray(new LLVMStackFrameNuller[0][]);
    }

    public Map<Block, List<Phi>> getPhiManager() {
        return phis;
    }

    @Override
    public void visit(Block block) {
        this.block.clear();

        block.accept(new LLVMBitcodeBlockVisitor(this, block));
        nullers.add(createNullers(slots.get(block)));
    }

    private LLVMStackFrameNuller[] createNullers(List<FrameSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return new LLVMStackFrameNuller[0];
        }
        LLVMStackFrameNuller[] nodes = new LLVMStackFrameNuller[slots.size()];
        int i = 0;
        for (FrameSlot slot : slots) {
            switch (slot.getKind()) {
                case Boolean:
                    nodes[i++] = new LLVMBooleanNuller(slot);
                    break;
                case Byte:
                    nodes[i++] = new LLVMStackFrameNuller.LLVMByteNuller(slot);
                    break;
                case Int:
                    nodes[i++] = new LLVMIntNuller(slot);
                    break;
                case Long:
                    nodes[i++] = new LLVMLongNuller(slot);
                    break;
                case Float:
                    nodes[i++] = new LLVMFloatNuller(slot);
                    break;
                case Double:
                    nodes[i++] = new LLVMDoubleNull(slot);
                    break;
                case Object:
                    nodes[i++] = new LLVMObjectNuller(slot);
                    break;
                case Illegal:
                    throw new AssertionError("illegal");
                default:
                    throw new AssertionError();
            }
        }
        return nodes;
    }

    public LLVMOptimizationConfiguration getOptimizationConfiguration() {
        return module.getOptimizationConfiguration();
    }
}
