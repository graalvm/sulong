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
package com.oracle.truffle.llvm.parser.bc.parser.listeners.metadata;

import com.oracle.truffle.llvm.parser.bc.parser.listeners.Types;
import com.oracle.truffle.llvm.runtime.types.BigIntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.IntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.IntegerType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantPointerType;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantType;

import java.util.Iterator;
import java.util.List;

/*
 * Metadata Nodes from the type OLD_NODE are structured in a way like:
 *
 * [type, value, type, value, ...]
 */
public class MetadataArgumentParser implements Iterator<Type> {

    protected final Types types;

    protected final List<Type> symbols;

    private final long[] args;

    private final int argCount;

    private int index = 0;

    MetadataArgumentParser(Types types, List<Type> symbols, long[] args, int argCount) {
        super();
        this.types = types;
        this.symbols = symbols;
        this.args = args;
        this.argCount = argCount;
    }

    @Override
    public boolean hasNext() {
        return remaining() > 0;
    }

    @Override
    public Type next() {
        assert (hasNext());

        return get(index++);
    }

    public Type peek() {
        return get(index);
    }

    static Type typeValToType(Types types, List<Type> symbols, long typeId, long val) {
        Type typeOfArgument = types.get(typeId);

        if (typeOfArgument instanceof IntegerConstantType) {
            return symbols.get((int) val);
        } else if (typeOfArgument instanceof BigIntegerConstantType) {
            return symbols.get((int) val);
        } else if (typeOfArgument instanceof IntegerType) {
            return symbols.get((int) val);
        } else if (typeOfArgument instanceof MetaType) {
            return new MetadataConstantType(val);
        } else if (typeOfArgument instanceof PointerType) {
            return new MetadataConstantPointerType((int) val);
        } else {
            throw new AssertionError("type not suported yet: " + typeOfArgument);
        }

    }

    protected Type get(int i) {
        assert (argCount >= i * 2 + 1);

        return typeValToType(types, symbols, args[i * 2], args[(i * 2) + 1]);
    }

    public int remaining() {
        if (argCount == 0) {
            return 0;
        }

        assert (argCount >= index * 2);

        return (argCount / 2) - index;
    }

    public void rewind() {
        index = 0;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("MetadataArgumentParser [");
        for (int i = index; i < (argCount / 2); i++) {
            s.append(get(i));
            if (i < ((argCount / 2) - 1)) {
                s.append(", ");
            }
        }
        s.append(']');
        return s.toString();
    }
}
