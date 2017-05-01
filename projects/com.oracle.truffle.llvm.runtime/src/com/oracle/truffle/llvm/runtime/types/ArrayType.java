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

import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

import java.util.Set;

public final class ArrayType extends AggregateType {

    @Override
    public int hashCode(Set<Type> visited) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elementType == null) ? 0 : elementType.hashCodeSafe(visited));
        result = prime * result + length;
        return result;
    }

    @Override
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
        ArrayType other = (ArrayType) obj;
        if (elementType == null) {
            if (other.elementType != null) {
                return false;
            }
        } else if (!elementType.equals(other.elementType)) {
            return false;
        }
        if (length != other.length) {
            return false;
        }
        return true;
    }

    private static final int NO_LENGTH = -1;

    private final Type elementType;
    private final int length;

    public ArrayType(Type type, int length) {
        this.elementType = type;
        this.length = length;
    }

    public ArrayType(Type type) {
        this(type, NO_LENGTH);
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getBitSize() {
        return getElementType().getBitSize() * getNumberOfElements();
    }

    public Type getElementType() {
        return elementType;
    }

    public boolean hasLength() {
        return length != NO_LENGTH;
    }

    @Override
    public int getNumberOfElements() {
        if (length == NO_LENGTH) {
            throw new UnsupportedOperationException();
        }
        return length;
    }

    @Override
    public Type getElementType(int index) {
        return elementType;
    }

    @Override
    public int getAlignment(DataSpecConverter targetDataLayout) {
        return getElementType().getAlignment(targetDataLayout);
    }

    @Override
    public int getSize(DataSpecConverter targetDataLayout) {
        return getElementType().getSize(targetDataLayout) * length;
    }

    @Override
    public int getOffsetOf(int index, DataSpecConverter targetDataLayout) {
        return getElementType().getSize(targetDataLayout) * index;
    }

    @Override
    public String toString() {
        return String.format("[%d x %s]", getNumberOfElements(), getElementType());
    }

}
