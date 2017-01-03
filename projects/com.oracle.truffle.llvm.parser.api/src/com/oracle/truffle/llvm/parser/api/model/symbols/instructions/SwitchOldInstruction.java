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
package com.oracle.truffle.llvm.parser.api.model.symbols.instructions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class SwitchOldInstruction implements VoidInstruction, TerminatingInstruction {

    private static final String LLVMIR_LABEL = "switch";

    private Symbol condition;

    private final InstructionBlock defaultBlock;

    private final long[] cases;

    private final InstructionBlock[] blocks;

    private SwitchOldInstruction(InstructionBlock defaultBlock, long[] cases, InstructionBlock[] blocks) {
        this.defaultBlock = defaultBlock;
        this.cases = cases;
        this.blocks = blocks;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public InstructionBlock getCaseBlock(int index) {
        return blocks[index];
    }

    public int getCaseCount() {
        return cases.length;
    }

    public long getCaseValue(int index) {
        return cases[index];
    }

    public Symbol getCondition() {
        return condition;
    }

    public InstructionBlock getDefaultBlock() {
        return defaultBlock;
    }

    @Override
    public List<InstructionBlock> getSuccessors() {
        final List<InstructionBlock> successors = new ArrayList<>(blocks.length + 1);
        Collections.addAll(successors, blocks);
        successors.add(defaultBlock);
        return successors;
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (condition == original) {
            condition = replacement;
        }
    }

    public static SwitchOldInstruction generate(FunctionDefinition function, int condition, int defaultBlock, long[] cases, int[] targetBlocks) {
        final InstructionBlock[] blocks = new InstructionBlock[targetBlocks.length];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = function.getBlock(targetBlocks[i]);
        }

        final SwitchOldInstruction inst = new SwitchOldInstruction(function.getBlock(defaultBlock), cases, blocks);
        inst.condition = function.getSymbols().getSymbol(condition, inst);
        return inst;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        // switch <intty> <value>, label <defaultdest>
        sb.append(LLVMIR_LABEL).append(' ');
        final String conditionType = condition.getType().toString();
        sb.append(conditionType).append(' ');
        sb.append(condition.getName());
        sb.append(", label ").append(defaultBlock.getName());

        // [ <intty> <val>, label <dest> ... ]
        // @formatter:off
        sb.append(" [");
        final String indent = buildIndent(sb.length());
        sb.append(IntStream.range(0, getCaseCount()).mapToObj(i -> {
            final long val = cases[i];
            final Symbol blk = blocks[i];
            return String.format("%s %d, label %s", conditionType, val, blk.getName());
        }).collect(Collectors.joining(indent)));
        // @formatter:on
        sb.append("]");
        return sb.toString();
    }

    private static String buildIndent(int length) {
        final StringBuilder builder = new StringBuilder(length + 1);
        builder.append('\n');
        for (int i = 0; i < length; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
