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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class PhiInstruction extends ValueInstruction {

    public static final String LLVMIR_LABEL = "phi";

    private final List<Symbol> values = new ArrayList<>();

    private final List<InstructionBlock> blocks = new ArrayList<>();

    private PhiInstruction(Type type) {
        super(type);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public InstructionBlock getBlock(int index) {
        return blocks.get(index);
    }

    public int getSize() {
        return values.size();
    }

    public Symbol getValue(int index) {
        return values.get(index);
    }

    @Override
    public void replace(Symbol original, Symbol replacment) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == original) {
                values.set(i, replacment);
            }
        }
    }

    public static PhiInstruction generate(FunctionDefinition function, Type type, int[] values, int[] blocks) {
        final PhiInstruction phi = new PhiInstruction(type);
        final Symbols symbols = function.getSymbols();
        for (int i = 0; i < values.length; i++) {
            phi.values.add(symbols.getSymbol(values[i], phi));
            phi.blocks.add(function.getBlock(blocks[i]));
        }
        return phi;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // <result> = phi <ty>
        sb.append(String.format("%s = %s %s", getName(), LLVMIR_LABEL, getType()));

        // [ <val0>, <label0>], ...
        // @formatter:off
        sb.append(IntStream.range(0, getSize())
                        .mapToObj(i -> String.format(" [ %s, %s ]", values.get(i).getName(), blocks.get(i).getName()))
                        .collect(Collectors.joining(",")));
        // @formatter:on

        return sb.toString();
    }
}
