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
package com.oracle.truffle.llvm.parser.base.model.symbols.constants.integer;

import com.oracle.truffle.llvm.parser.base.model.symbols.constants.AbstractConstant;
import com.oracle.truffle.llvm.parser.base.model.types.IntegerType;

public final class IntegerConstant extends AbstractConstant {

    private final long value;

    public IntegerConstant(IntegerType type, long value) {
        super(type);
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (((IntegerType) getType()).getBitCount() == 1) {
            return value == 0 ? "false" : "true";
        }
        return String.valueOf(value);
    }

    public static IntegerConstant fromDatum(IntegerType type, long datum) {
        // Sign extend for everything except i1 (boolean)
        final int bits = type.getBitCount();
        long d = datum;
        if (bits > 1 && bits < Long.SIZE) {
            d = extendSign(bits, d);
        }

        return new IntegerConstant(type, d);
    }

    private static long extendSign(int bits, long value) {
        long v = value;
        long mask = (~((1L << (bits)) - 1)) >> 1;
        if ((v & mask) != 0) {
            v |= mask;
        }
        return v;
    }
}
