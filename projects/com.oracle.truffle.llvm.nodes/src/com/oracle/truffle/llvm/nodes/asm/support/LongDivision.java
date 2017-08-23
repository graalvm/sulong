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
package com.oracle.truffle.llvm.nodes.asm.support;

public class LongDivision {
    public static class Result {
        public final long quotient;
        public final long remainder;

        Result(long quotient, long remainder) {
            this.quotient = quotient;
            this.remainder = remainder;
        }

        @Override
        public String toString() {
            return "Result[q=" + quotient + ",r=" + remainder + "]";
        }
    }

    // based on http://www.hackersdelight.org/hdcodetxt/divls.c.txt
    public static Result divs128by64(long a1, long a0, long b) {
        long q;
        long r;
        long u0 = a0;
        long u1 = a1;
        long v = b;

        if (a0 == 0 && a1 > 0) {
            return new Result(a0 / b, a0 % b);
        }

        long uneg = u1 >> 63;     // -1 if u < 0.
        if (u1 < 0) {             // Compute the absolute
            u0 = -u0;             // value of the dividend u.
            long borrow = (u0 != 0) ? 1 : 0;
            u1 = -u1 - borrow;
        }

        long vneg = v >> 63;      // -1 if v < 0.
        v = (v ^ vneg) - vneg;    // Absolute value of v.

        if (Long.compareUnsigned(u1, v) >= 0) { // overflow
            r = 0x8000000000000000L;
            q = 0x8000000000000000L;
            return new Result(q, r);
        }

        Result resultUnsigned = divu128by64(u1, u0, v);
        q = resultUnsigned.quotient;
        r = resultUnsigned.remainder;

        long diff = uneg ^ vneg; // Negate q if signs of
        q = (q ^ diff) - diff;   // u and v differed.
        if (uneg != 0) {
            r = -r;
        }

        if ((diff ^ q) < 0 && q != 0) { // If overflow, set remainder
            r = 0x8000000000000000L; // to an impossible value, and return the
            q = 0x8000000000000000L; // largest possible neg. quotient.
        }

        return new Result(q, r);
    }

    // https://www.codeproject.com/Tips/785014/UInt-Division-Modulus
    public static Result divu128by64(long u1, long u0, long d) {
        long b = 1L << 32;
        long un32;
        long un10;
        long v = d;

        long s = Long.numberOfLeadingZeros(v);
        v <<= s;
        long vn1 = v >>> 32;
        long vn0 = v & 0xffffffffL;

        if (s > 0) {
            un32 = (u1 << s) | (u0 >>> (64 - s));
            un10 = u0 << s;
        } else {
            un32 = u1;
            un10 = u0;
        }

        long un1 = un10 >>> 32;
        long un0 = un10 & 0xffffffffL;

        long q1 = Long.divideUnsigned(un32, vn1);
        long rhat = Long.remainderUnsigned(un32, vn1);

        long left = q1 * vn0;
        long right = (rhat << 32) + un1;
        while ((Long.compareUnsigned(q1, b) >= 0) || (Long.compareUnsigned(left, right) > 0)) {
            --q1;
            rhat += vn1;
            if (Long.compareUnsigned(rhat, b) < 0) {
                left -= vn0;
                right = (rhat << 32) | un1;
            } else {
                break;
            }
        }

        long un21 = (un32 << 32) + (un1 - (q1 * v));

        long q0 = Long.divideUnsigned(un21, vn1);
        rhat = Long.remainderUnsigned(un21, vn1);

        left = q0 * vn0;
        right = (rhat << 32) | un0;
        while ((Long.compareUnsigned(q0, b) >= 0) || (Long.compareUnsigned(left, right) > 0)) {
            --q0;
            rhat += vn1;
            if (rhat < b) {
                left -= vn0;
                right = (rhat << 32) | un0;
            } else {
                break;
            }
        }

        long r = ((un21 << 32) + (un0 - (q0 * v))) >>> s;
        long q = (q1 << 32) | q0;

        return new Result(q, r);
    }
}
