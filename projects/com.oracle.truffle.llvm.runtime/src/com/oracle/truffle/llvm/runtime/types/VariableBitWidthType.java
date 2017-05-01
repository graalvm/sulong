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

public final class VariableBitWidthType extends Type {

    private final int bitWidth;
    private final Object constant;

    public VariableBitWidthType(int bitWidth) {
        this(bitWidth, null);
    }

    VariableBitWidthType(int bitWidth, Object constant) {
        this.bitWidth = bitWidth;
        this.constant = constant;
    }

    public Object getConstant() {
        return constant;
    }

    public boolean isConstant() {
        return constant != null;
    }

    @Override
    public int getBitSize() {
        return bitWidth;
    }

    @Override
    public int hashCode(Set<Type> visited) {
        final int prime = 31;
        int result = 1;
        result = prime * result + bitWidth;
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
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
        VariableBitWidthType other = (VariableBitWidthType) obj;
        if (bitWidth != other.bitWidth) {
            return false;
        }
        if (constant == null) {
            if (other.constant != null) {
                return false;
            }
        } else if (!constant.equals(other.constant)) {
            return false;
        }
        return true;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getAlignment(DataSpecConverter targetDataLayout) {
        if (targetDataLayout != null) {
            return targetDataLayout.getBitAlignment(this) / Byte.SIZE;

        } else if (bitWidth <= Byte.SIZE) {
            return Byte.BYTES;

        } else if (bitWidth <= Short.SIZE) {
            return Short.BYTES;

        } else if (bitWidth <= Integer.SIZE) {
            return Integer.BYTES;

        } else {
            return Long.BYTES;
        }
    }

    @Override
    public int getSize(DataSpecConverter targetDataLayout) {
        return targetDataLayout.getSize(this);
    }

}
