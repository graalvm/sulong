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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.llvm.nodes.asm.support.LLVMAMD64String;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMExitException;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

@NodeChildren({@NodeChild("rax"), @NodeChild("rdi"), @NodeChild("rsi"), @NodeChild("rdx"), @NodeChild("r10"), @NodeChild("r8"), @NodeChild("r9")})
public abstract class LLVMAMD64SyscallNode extends LLVMExpressionNode {
    private final LongValueProfile profile = LongValueProfile.createIdentityProfile();

    protected static void exit(int code) {
        throw new LLVMExitException(code);
    }

    @Specialization
    protected long executeI64(long rax, LLVMAddress rdi, long rsi, long rdx, long r10, long r8, long r9) {
        switch ((int) profile.profile(rax)) {
            case LLVMAMD64Syscall.SYS_open:
                return LLVMAMD64File.open(LLVMAMD64String.cstr(rdi), (int) rsi, (int) rdx);
            case LLVMAMD64Syscall.SYS_mmap:
                return LLVMAMD64Memory.mmap(rdi, rsi, (int) rdx, (int) r10, (int) r8, r9);
            case LLVMAMD64Syscall.SYS_brk:
                return LLVMAMD64Memory.brk(rdi);
            case LLVMAMD64Syscall.SYS_uname:
                return LLVMAMD64Info.uname(rdi);
            case LLVMAMD64Syscall.SYS_getcwd: {
                String cwd = LLVMAMD64Path.getcwd();
                if (cwd.length() >= rsi) {
                    return -LLVMAMD64Error.ERANGE;
                } else {
                    LLVMAMD64String.strcpy(cwd, rdi);
                    return cwd.length() + 1;
                }
            }
            default:
                // return -LLVMAMD64Error.ENOSYS;
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("unknown syscall " + rax);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected long executeI64(long rax, long rdi, LLVMAddress rsi, long rdx, long r10, long r8, long r9) {
        switch ((int) profile.profile(rax)) {
            case LLVMAMD64Syscall.SYS_read: {
                // byte[] buf = new byte[(int) rdx];
                // int result = LLVMAMD64File.read((int) rdi, buf);
                // if (result > 0) {
                // LLVMAMD64String.memcpy(buf, rsi, result);
                // }
                // return result;
                return LLVMAMD64File.read((int) rdi, rsi, (int) rdx);
            }
            case LLVMAMD64Syscall.SYS_write:
                // return LLVMAMD64File.write((int) rdi, LLVMAMD64String.memcpy(rsi, (int) rdx));
                return LLVMAMD64File.write((int) rdi, rsi, (int) rdx);
            case LLVMAMD64Syscall.SYS_fstat:
                return -LLVMAMD64Error.ENOSYS;
            case LLVMAMD64Syscall.SYS_readv:
                return LLVMAMD64File.readv((int) rdi, rsi, rdx);
            case LLVMAMD64Syscall.SYS_writev:
                return LLVMAMD64File.writev((int) rdi, rsi, rdx);
            case LLVMAMD64Syscall.SYS_clock_gettime:
                return LLVMAMd64Time.clockGetTime((int) rdi, rsi);
            default:
                // return -LLVMAMD64Error.ENOSYS;
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("unknown syscall " + rax);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected long executeI64(long rax, LLVMAddress rdi, LLVMAddress rsi, long rdx, long r10, long r8, long r9) {
        switch ((int) profile.profile(rax)) {
            case LLVMAMD64Syscall.SYS_stat:
                return LLVMAMD64File.stat(rdi, rsi);
            case LLVMAMD64Syscall.SYS_lstat:
                return -LLVMAMD64Error.ENOSYS;
            default:
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("unknown syscall " + rax);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected long executeI64(long rax, long rdi, LLVMAddress rsi, LLVMAddress rdx, long r10, long r8, long r9) {
        switch ((int) profile.profile(rax)) {
            case LLVMAMD64Syscall.SYS_rt_sigaction:
            case LLVMAMD64Syscall.SYS_rt_sigprocmask:
                return -LLVMAMD64Error.ENOSYS;
            default:
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("unknown syscall " + rax);
        }
    }

    @Specialization
    protected long executeI64(long rax, long rdi, long rsi, long rdx, long r10, long r8, long r9) {
        switch ((int) profile.profile(rax)) {
            case LLVMAMD64Syscall.SYS_close:
                return LLVMAMD64File.close((int) rdi);
            case LLVMAMD64Syscall.SYS_lseek:
                return LLVMAMD64File.lseek((int) rdi, rsi, (int) rdx);
            case LLVMAMD64Syscall.SYS_ioctl:
                return LLVMAMD64File.ioctl((int) rdi, (int) rsi, rdx);
            case LLVMAMD64Syscall.SYS_dup2:
                return LLVMAMD64File.dup2((int) rdi, (int) rsi);
            case LLVMAMD64Syscall.SYS_exit_group:
                // TODO: implement difference to SYS_exit
            case LLVMAMD64Syscall.SYS_exit:
                exit((int) rdi);
                return 0;
            case LLVMAMD64Syscall.SYS_fcntl:
                return LLVMAMD64File.fcntl((int) rdi, (int) rsi, rdx);
            case LLVMAMD64Syscall.SYS_ftruncate:
                return LLVMAMD64File.ftruncate((int) rdi, rsi);
            case LLVMAMD64Syscall.SYS_getuid:
                return LLVMAMD64Security.getuid();
            case LLVMAMD64Syscall.SYS_getgid:
                return LLVMAMD64Security.getgid();
            case LLVMAMD64Syscall.SYS_setuid:
            case LLVMAMD64Syscall.SYS_setgid:
                return -LLVMAMD64Error.ENOSYS;
            case LLVMAMD64Syscall.SYS_futex:
                return -LLVMAMD64Error.ENOSYS;
            // Type compatibility wrappers
            case LLVMAMD64Syscall.SYS_open:
            case LLVMAMD64Syscall.SYS_mmap:
            case LLVMAMD64Syscall.SYS_brk:
            case LLVMAMD64Syscall.SYS_uname:
            case LLVMAMD64Syscall.SYS_getcwd:
                return executeI64(rax, LLVMAddress.fromLong(rdi), rsi, rdx, r10, r8, r9);
            case LLVMAMD64Syscall.SYS_read:
            case LLVMAMD64Syscall.SYS_write:
            case LLVMAMD64Syscall.SYS_fstat:
            case LLVMAMD64Syscall.SYS_readv:
            case LLVMAMD64Syscall.SYS_writev:
                return executeI64(rax, rdi, LLVMAddress.fromLong(rsi), rdx, r10, r8, r9);
            case LLVMAMD64Syscall.SYS_stat:
            case LLVMAMD64Syscall.SYS_lstat:
                return executeI64(rax, LLVMAddress.fromLong(rdi), LLVMAddress.fromLong(rsi), rdx, r10, r8, r9);
            case LLVMAMD64Syscall.SYS_rt_sigaction:
            case LLVMAMD64Syscall.SYS_rt_sigprocmask:
                return executeI64(rax, rdi, LLVMAddress.fromLong(rsi), LLVMAddress.fromLong(rdx), r10, r8, r9);
            default:
                // return -LLVMAMD64Error.ENOSYS;
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("unknown syscall " + rax);
        }
    }
}
