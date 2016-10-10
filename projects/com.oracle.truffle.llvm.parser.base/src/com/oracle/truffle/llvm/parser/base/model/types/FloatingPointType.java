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
package com.oracle.truffle.llvm.parser.base.model.types;

import com.oracle.truffle.llvm.parser.LLVMBaseType;

public final class FloatingPointType implements Type {

    public static final int HALF_BITS = 16;
    public static final int FLOAT_BITS = 32;
    public static final int DOUBLE_BITS = 64;
    public static final int X86_FP80_BITS = 80;
    public static final int FP128_BITS = 128;
    public static final int PPC_FP128_BITS = 128;

    public static final FloatingPointType HALF = new FloatingPointType(HALF_BITS, 2, LLVMBaseType.HALF);

    public static final FloatingPointType FLOAT = new FloatingPointType(FLOAT_BITS, 4, LLVMBaseType.FLOAT);

    public static final FloatingPointType DOUBLE = new FloatingPointType(DOUBLE_BITS, 8, LLVMBaseType.DOUBLE);

    public static final FloatingPointType X86_FP80 = new FloatingPointType(X86_FP80_BITS, 16, LLVMBaseType.X86_FP80);

    public static final FloatingPointType FP128 = new FloatingPointType(FP128_BITS, 16, LLVMBaseType.F128);

    public static final FloatingPointType PPC_FP128 = new FloatingPointType(PPC_FP128_BITS, 16, LLVMBaseType.PPC_FP128);

    private final int alignment;

    private final int width;

    private final LLVMBaseType llvmBaseType;

    FloatingPointType(int width, int alignment, LLVMBaseType llvmBaseType) {
        this.alignment = alignment;
        this.width = width;
        this.llvmBaseType = llvmBaseType;
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

    @Override
    public LLVMBaseType getLLVMBaseType() {
        return llvmBaseType;
    }

    @Override
    public int getBits() {
        return width;
    }

    @Override
    public int sizeof() {
        return width / Byte.SIZE;
    }

    public int width() {
        return width;
    }

    @Override
    public String toString() {
        return String.format("f%d", width);
    }
}
