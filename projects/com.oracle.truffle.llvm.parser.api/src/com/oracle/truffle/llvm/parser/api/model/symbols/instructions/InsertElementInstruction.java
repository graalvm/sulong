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

import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class InsertElementInstruction extends ValueInstruction {

    public static final String LLVMIR_LABEL = "insertelement";

    private Symbol vector;

    private Symbol index;

    private Symbol value;

    private InsertElementInstruction(Type type) {
        super(type);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public Symbol getIndex() {
        return index;
    }

    public Symbol getValue() {
        return value;
    }

    public Symbol getVector() {
        return vector;
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (vector == original) {
            vector = replacement;
        }
        if (index == original) {
            index = replacement;
        }
        if (value == original) {
            value = replacement;
        }
    }

    public static InsertElementInstruction fromSymbols(Symbols symbols, Type type, int vector, int index, int value) {
        final InsertElementInstruction inst = new InsertElementInstruction(type);
        inst.vector = symbols.getSymbol(vector, inst);
        inst.index = symbols.getSymbol(index, inst);
        inst.value = symbols.getSymbol(value, inst);
        return inst;
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s %s, %s %s, %s %s", getName(), LLVMIR_LABEL,
                        vector.getType(), vector.getName(),
                        value.getType(), value.getName(),
                        index.getType(), index.getName());
    }
}
