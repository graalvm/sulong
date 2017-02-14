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
package com.oracle.truffle.llvm.parser.api.model.symbols.constants;

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.visitors.ConstantVisitor;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public final class BlockAddressConstant extends AbstractConstant {

    private FunctionDefinition function;

    private final int block;

    private BlockAddressConstant(Type type, int block) {
        super(type);
        this.block = block;
    }

    @Override
    public void accept(ConstantVisitor visitor) {
        visitor.visit(this);
    }

    public FunctionDefinition getFunction() {
        return function;
    }

    public InstructionBlock getInstructionBlock() {
        return function.getBlock(block);
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (function == original) {
            if (replacement instanceof FunctionDefinition) {
                function = (FunctionDefinition) replacement;
            } else {
                throw new IllegalStateException("Expected FunctionDefinition!");
            }
        }
    }

    public static BlockAddressConstant fromSymbols(Symbols symbols, Type type, int function, int block) {
        final BlockAddressConstant constant = new BlockAddressConstant(type, block);
        constant.function = (FunctionDefinition) symbols.getSymbol(function, constant);
        return constant;
    }
}
