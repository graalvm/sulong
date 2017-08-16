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
package com.oracle.truffle.llvm.nodes.asm.support;

/*
 * This functionality is in the standard class library since Java 9:
 * https://bugs.openjdk.java.net/browse/JDK-5100935
 */
public class LongMultiplication {
    private static final long MASK32 = 0xFFFFFFFFL;
    private static final int SHIFT32 = 32;

    // based on http://www.hackersdelight.org/hdcodetxt/mulhu.c.txt
    public static long multiplyHigh(long u, long v) {
        long u0 = u & MASK32;
        long u1 = u >> SHIFT32;
        long v0 = v & MASK32;
        long v1 = v >> SHIFT32;
        long w0 = u0 * v0;
        long t = u1 * v0 + (w0 >>> SHIFT32);
        long w1 = t & MASK32;
        long w2 = t >> SHIFT32;
        w1 += u0 * v1;
        return u1 * v1 + w2 + (w1 >> SHIFT32);
    }

    public static long multiplyHighUnsigned(long x, long y) {
        long high = multiplyHigh(x, y);
        return high + (((x < 0) ? y : 0) + ((y < 0) ? x : 0));
    }
}
