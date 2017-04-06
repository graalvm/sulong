/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.model.symbols.instructions;

import com.oracle.truffle.llvm.parser.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.model.visitors.InstructionVisitor;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public final class CompareExchangeInstruction extends ValueInstruction {

    private Symbol ptr;
    private Symbol cmp;
    private Symbol replace;

    private final AtomicOrdering successOrdering;
    private final AtomicOrdering failureOrdering;
    private final SynchronizationScope synchronizationScope;

    private final boolean isWeak;
    private final boolean isVolatile;

    private CompareExchangeInstruction(Type type, AtomicOrdering successOrdering, AtomicOrdering failureOrdering, SynchronizationScope synchronizationScope, boolean isWeak, boolean isVolatile) {
        super(type);
        this.successOrdering = successOrdering;
        this.failureOrdering = failureOrdering;
        this.synchronizationScope = synchronizationScope;
        this.isWeak = isWeak;
        this.isVolatile = isVolatile;
    }

    public Symbol getPtr() {
        return ptr;
    }

    public Symbol getCmp() {
        return cmp;
    }

    public Symbol getReplace() {
        return replace;
    }

    public AtomicOrdering getSuccessOrdering() {
        return successOrdering;
    }

    public AtomicOrdering getFailureOrdering() {
        return failureOrdering;
    }

    public SynchronizationScope getSynchronizationScope() {
        return synchronizationScope;
    }

    public boolean isWeak() {
        return isWeak;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (original == ptr) {
            ptr = replacement;
        }
        if (original == cmp) {
            cmp = replacement;
        }
        if (original == replace) {
            replace = replacement;
        }
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    public static CompareExchangeInstruction fromSymbols(Symbols symbols, Type type, int ptr, int cmp, int replace, boolean isVolatile, long successOrderingId, long synchronizationScopeId,
                    long failureOrderingId, boolean isWeak) {
        final AtomicOrdering successOrdering = AtomicOrdering.decode(successOrderingId);
        final SynchronizationScope synchronizationScope = SynchronizationScope.decode(synchronizationScopeId);
        final AtomicOrdering failureOrdering = AtomicOrdering.getOrStrongestFailureOrdering(failureOrderingId, successOrdering);

        final CompareExchangeInstruction cmpxchg = new CompareExchangeInstruction(type, successOrdering, failureOrdering, synchronizationScope, isWeak, isVolatile);
        cmpxchg.ptr = symbols.getSymbol(ptr, cmpxchg);
        cmpxchg.cmp = symbols.getSymbol(cmp, cmpxchg);
        cmpxchg.replace = symbols.getSymbol(replace, cmpxchg);
        return cmpxchg;
    }
}
