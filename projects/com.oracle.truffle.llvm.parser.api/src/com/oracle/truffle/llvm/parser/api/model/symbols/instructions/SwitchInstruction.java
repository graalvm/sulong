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
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class SwitchInstruction implements VoidInstruction, TerminatingInstruction {

    public static final String LLVMIR_LABEL = "switch";

    private Symbol condition;

    private final InstructionBlock defaultBlock;

    private final Symbol[] values;

    private final InstructionBlock[] blocks;

    private SwitchInstruction(InstructionBlock defaultBlock, int numCases) {
        this.defaultBlock = defaultBlock;
        this.values = new Symbol[numCases];
        this.blocks = new InstructionBlock[numCases];
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public InstructionBlock getCaseBlock(int index) {
        return blocks[index];
    }

    public int getCaseCount() {
        return values.length;
    }

    public Symbol getCaseValue(int index) {
        return values[index];
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
        for (int i = 0; i < values.length; i++) {
            if (values[i] == original) {
                values[i] = replacement;
            }
        }
    }

    public static SwitchInstruction generate(FunctionDefinition function, int condition, int defaultBlock, int[] caseValues, int[] caseBlocks) {
        final SwitchInstruction inst = new SwitchInstruction(function.getBlock(defaultBlock), caseBlocks.length);

        final Symbols symbols = function.getSymbols();
        inst.condition = symbols.getSymbol(condition, inst);
        for (int i = 0; i < caseBlocks.length; i++) {
            inst.values[i] = symbols.getSymbol(caseValues[i], inst);
            inst.blocks[i] = function.getBlock(caseBlocks[i]);
        }

        return inst;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // switch <intty> <value>, label <defaultdest>
        sb.append(String.format("%s %s %s, label %s", LLVMIR_LABEL,
                        condition.getType(), condition.getName(),
                        defaultBlock.getName()));

        // [ <intty> <val>, label <dest> ... ]
        // @formatter:off
        sb.append(String.format(" [%s ]",
                        IntStream.range(0, getCaseCount())
                        .mapToObj(i -> String.format(" %s %s, label %s",
                                        values[i].getType(), values[i].getName(),
                                        blocks[i].getName()))
                        .collect(Collectors.joining("\n          ")))); // TODO: same space indentation
        // @formatter:on

        return sb.toString();
    }
}
