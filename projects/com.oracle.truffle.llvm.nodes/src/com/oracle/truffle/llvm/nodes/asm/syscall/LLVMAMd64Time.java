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
package com.oracle.truffle.llvm.nodes.asm.syscall;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;

public class LLVMAMd64Time {
    // @formatter:off;
    public static final int CLOCK_REALTIME                = 0;
    public static final int CLOCK_MONOTONIC               = 1;
    public static final int CLOCK_PROCESS_CPUTIME_ID      = 2;
    public static final int CLOCK_THREAD_CPUTIME_ID       = 3;
    public static final int CLOCK_MONOTONIC_RAW           = 4;
    public static final int CLOCK_REALTIME_COARSE         = 5;
    public static final int CLOCK_MONOTONIC_COARSE        = 6;
    public static final int CLOCK_BOOTTIME                = 7;
    public static final int CLOCK_REALTIME_ALARM          = 8;
    public static final int CLOCK_BOOTTIME_ALARM          = 9;
    public static final int CLOCK_SGI_CYCLE               = 10;     /* Hardware specific */
    public static final int CLOCK_TAI                     = 11;
    // @formatter:on

    @TruffleBoundary
    public static int clockGetTime(int clkId, LLVMAddress timespec) {
        long s;
        long ns;
        switch (clkId) {
            case CLOCK_REALTIME: {
                long t = System.currentTimeMillis();
                s = t / 1000;
                ns = (t % 1000) * 1000000;
                break;
            }
            default:
                return -LLVMAMD64Error.EINVAL;
        }
        LLVMAddress ptr = timespec;
        LLVMMemory.putI64(ptr, s);
        ptr = ptr.increment(8);
        LLVMMemory.putI64(ptr, ns);
        return 0;
    }
}
