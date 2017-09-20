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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.llvm.nodes.asm.support.LLVMAMD64String;
import com.oracle.truffle.llvm.runtime.LLVMAddress;

public class LLVMAMD64Info {
    public static final String sysname;
    public static final String release;
    public static final String machine;

    static {
        sysname = System.getProperty("os.name");
        release = System.getProperty("os.version");
        String arch = System.getProperty("os.arch");
        if (arch.equals("amd64")) {
            arch = "x86_64";
        }
        machine = arch;
    }

    private static String readFile(String name, String fallback) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            Path path = Paths.get(name);
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path)).trim();
            }
        } catch (Exception e) {
        }
        return fallback;
    }

    @TruffleBoundary
    public static String getHostname() {
        String hostname = readFile("/proc/sys/kernel/hostname", null);
        if (hostname != null) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @TruffleBoundary
    public static String getVersion() {
        return readFile("/proc/sys/kernel/version", null);
    }

    @TruffleBoundary
    public static String getDomainName() {
        return readFile("/proc/sys/kernel/domainname", null);
    }

    public static long uname(LLVMAddress name) {
        LLVMAddress ptr = name;
        LLVMAMD64String.strcpy(ptr, sysname);
        ptr = ptr.increment(65);
        LLVMAMD64String.strcpy(ptr, getHostname());
        ptr = ptr.increment(65);
        LLVMAMD64String.strcpy(ptr, release);
        ptr = ptr.increment(65);
        LLVMAMD64String.strcpy(ptr, getVersion());
        ptr = ptr.increment(65);
        LLVMAMD64String.strcpy(ptr, machine);
        ptr = ptr.increment(65);
        LLVMAMD64String.strcpy(ptr, getDomainName());
        return 0;
    }

    @TruffleBoundary
    private static LLVMAMD64ProcessStat getstat() {
        String stat = readFile("/proc/self/stat", null);
        if (stat == null) {
            return null;
        } else {
            return new LLVMAMD64ProcessStat(stat);
        }
    }

    @TruffleBoundary
    public static long getpid() {
        LLVMAMD64ProcessStat stat = getstat();
        if (stat != null) {
            return stat.getPid();
        }
        String info = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(info.split("@")[0]);
    }

    @TruffleBoundary
    public static long getppid() {
        LLVMAMD64ProcessStat stat = getstat();
        if (stat != null) {
            return stat.getPpid();
        } else {
            return 1; // fallback: init
        }
    }
}
