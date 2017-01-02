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

import com.oracle.truffle.llvm.parser.api.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class CompareInstruction extends ValueInstruction {

    public static final String LLVMIR_LABEL = "icmp";
    public static final String LLVMIR_LABEL_FP = "fcmp";

    private final CompareOperator operator;

    private Symbol lhs;

    private Symbol rhs;

    private CompareInstruction(Type type, CompareOperator operator) {
        super(type);
        this.operator = operator;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public Symbol getLHS() {
        return lhs;
    }

    public CompareOperator getOperator() {
        return operator;
    }

    public Symbol getRHS() {
        return rhs;
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (lhs == original) {
            lhs = replacement;
        }
        if (rhs == original) {
            rhs = replacement;
        }
    }

    public static CompareInstruction fromSymbols(Symbols symbols, Type type, int opcode, int lhs, int rhs) {
        final CompareInstruction cmpInst = new CompareInstruction(type, CompareOperator.decode(opcode));
        cmpInst.lhs = symbols.getSymbol(lhs, cmpInst);
        cmpInst.rhs = symbols.getSymbol(rhs, cmpInst);
        return cmpInst;
    }

    @Override
    public String toString() {
        if (operator.isFloatingPoint()) {
            // <result> = fcmp <cond> <ty> <op1>, <op2>
            return String.format("%s = %s %s %s %s, %s", getName(), LLVMIR_LABEL_FP, operator, lhs.getType(),
                            lhs.getName(), rhs.getName());
        } else {
            // <result> = icmp <cond> <ty> <op1>, <op2>
            return String.format("%s = %s %s %s %s, %s", getName(), LLVMIR_LABEL, operator, lhs.getType(),
                            lhs.getName(), rhs.getName());
        }
    }
}
