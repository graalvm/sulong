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
package com.oracle.truffle.llvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.ValueType;

@ValueType
public final class LLVMFunctionHandle implements LLVMFunction {

    private final long functionIndex;
    private final boolean isSulong;

    private LLVMFunctionHandle(long functionIndex, boolean isSulong) {
        this.functionIndex = functionIndex;
        this.isSulong = isSulong;
    }

    public static LLVMFunctionHandle createSulongHandle(long value) {
        return new LLVMFunctionHandle(value, true);
    }

    public static LLVMFunctionHandle createNativeHandle(long value) {
        return new LLVMFunctionHandle(value, false);
    }

    public static LLVMFunctionHandle nullPointer() {
        return new LLVMFunctionHandle(0, true);
    }

    @Override
    public long getFunctionPointer() {
        return functionIndex;
    }

    public boolean isSulong() {
        return isSulong;
    }

    public boolean isExternNative() {
        return !isSulong;
    }

    @Override
    public boolean isNullFunction() {
        return functionIndex == 0;
    }
}
