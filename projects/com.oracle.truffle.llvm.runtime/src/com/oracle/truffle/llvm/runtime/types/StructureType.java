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
package com.oracle.truffle.llvm.runtime.types;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

public final class StructureType extends AggregateType {

    private final String name;
    private final boolean isPacked;
    private final Type[] types;

    public StructureType(String name, boolean isPacked, Type[] types) {
        this.name = name;
        this.isPacked = isPacked;
        this.types = types;
    }

    public StructureType(boolean isPacked, Type[] types) {
        this(LLVMIdentifier.UNKNOWN, isPacked, types);
    }

    public Type[] getElementTypes() {
        return types;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getBitSize() {
        if (isPacked) {
            return Arrays.stream(types).mapToInt(Type::getBitSize).sum();
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("TargetDataLayout is necessary to compute Padding information!");
        }
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getNumberOfElements() {
        return types.length;
    }

    @Override
    public Type getElementType(long index) {
        assert index == (int) index;
        return types[(int) index];
    }

    @Override
    public int getAlignment(DataSpecConverter targetDataLayout) {
        return isPacked ? 1 : getLargestAlignment(targetDataLayout);
    }

    @Override
    public int getSize(DataSpecConverter targetDataLayout) {
        int sumByte = 0;
        for (final Type elementType : types) {
            if (!isPacked) {
                sumByte += Type.getPadding(sumByte, elementType, targetDataLayout);
            }
            sumByte += elementType.getSize(targetDataLayout);
        }

        int padding = 0;
        if (!isPacked && sumByte != 0) {
            padding = Type.getPadding(sumByte, getAlignment(targetDataLayout));
        }

        return sumByte + padding;
    }

    @Override
    public Type shallowCopy() {
        final StructureType copy = new StructureType(name, isPacked, types);
        copy.setSourceType(getSourceType());
        return copy;
    }

    @Override
    public long getOffsetOf(long index, DataSpecConverter targetDataLayout) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            final Type elementType = types[i];
            if (!isPacked) {
                offset += Type.getPadding(offset, elementType, targetDataLayout);
            }
            offset += elementType.getSize(targetDataLayout);
        }
        if (!isPacked && getSize(targetDataLayout) > offset) {
            assert index == (int) index;
            offset += Type.getPadding(offset, types[(int) index], targetDataLayout);
        }
        return offset;
    }

    private int getLargestAlignment(DataSpecConverter targetDataLayout) {
        int largestAlignment = 0;
        for (final Type elementType : types) {
            largestAlignment = Math.max(largestAlignment, elementType.getAlignment(targetDataLayout));
        }
        return largestAlignment;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    @TruffleBoundary
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isPacked ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(types);
        return result;
    }

    @Override
    @TruffleBoundary
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StructureType other = (StructureType) obj;
        if (isPacked != other.isPacked) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (!Arrays.equals(types, other.types)) {
            return false;
        }
        return true;
    }
}
