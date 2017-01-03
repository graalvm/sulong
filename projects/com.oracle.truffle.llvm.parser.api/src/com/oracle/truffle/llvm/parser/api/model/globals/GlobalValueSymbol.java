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
package com.oracle.truffle.llvm.parser.api.model.globals;

import com.oracle.truffle.llvm.parser.api.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.api.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.symbols.ValueSymbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.api.model.types.PointerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.ModelVisitor;

public abstract class GlobalValueSymbol implements ValueSymbol {

    private final Type type;

    private final int initialiser;

    private final int align;

    private String name = ValueSymbol.UNKNOWN;

    private Symbol value = null;

    private final Linkage linkage;

    private final Visibility visibility;

    GlobalValueSymbol(Type type, int initialiser, int align, Linkage linkage, Visibility visibility) {
        this.type = type;
        this.initialiser = initialiser;
        this.align = align;
        this.linkage = linkage;
        this.visibility = visibility;
    }

    public abstract void accept(ModelVisitor visitor);

    @Override
    public int getAlign() {
        return align;
    }

    public int getInitialiser() {
        return initialiser;
    }

    public abstract String getIRKeyword();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    public Linkage getLinkage() {
        return linkage;
    }

    public boolean isStatic() {
        return linkage == Linkage.INTERNAL || linkage == Linkage.PRIVATE;
    }

    public boolean isExtern() {
        return linkage == Linkage.EXTERNAL || linkage == Linkage.EXTERN_WEAK;
    }

    public Symbol getValue() {
        return value;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void initialise(Symbols symbols) {
        if (getInitialiser() > 0) {
            value = symbols.getSymbol(getInitialiser() - 1);
        }
    }

    @Override
    public void setName(String name) {
        this.name = "@" + name;
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();

        builder.append(getName()).append(" = ");

        // TODO threadlocal model

        builder.append(getLinkage().getIrString()).append(' ');

        // TODO calling convention

        builder.append(getIRKeyword()).append(' ');

        builder.append(((PointerType) getType()).getPointeeType().toString());

        if (getValue() != null && getValue() instanceof NullConstant) {
            builder.append(" zeroinitializer");
        } else if (getValue() != null) {
            builder.append(' ').append(getValue().toString());
        }

        if (getAlign() > 0) {
            builder.append(String.format(", align %d", align));
        }

        return builder.toString();
    }
}
