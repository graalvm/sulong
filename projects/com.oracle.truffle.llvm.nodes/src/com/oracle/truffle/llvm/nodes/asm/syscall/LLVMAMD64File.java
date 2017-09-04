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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.llvm.nodes.asm.support.LLVMAMD64String;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;

import sun.misc.SharedSecrets;

public class LLVMAMD64File {
    public static final int O_ACCMODE = 00000003;
    public static final int O_RDONLY = 00000000;
    public static final int O_WRONLY = 00000001;
    public static final int O_RDWR = 00000002;
    public static final int O_CREAT = 00000100;
    public static final int O_EXCL = 00000200;
    public static final int O_NOCTTY = 00000400;
    public static final int O_TRUNC = 00001000;
    public static final int O_APPEND = 00002000;
    public static final int O_NONBLOCK = 00004000;
    public static final int O_DSYNC = 00010000;
    public static final int FASYNC = 00020000;
    public static final int O_DIRECT = 00040000;
    public static final int O_LARGEFILE = 00100000;
    public static final int O_DIRECTORY = 00200000;
    public static final int O_NOFOLLOW = 00400000;
    public static final int O_NOATIME = 01000000;
    public static final int O_CLOEXEC = 02000000;
    public static final int O_TMPFILE = 020000000;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    private static boolean test(int flags, int flag) {
        return (flags & flag) == flag;
    }

    private static FileDescriptor getFd(int fd) {
        FileDescriptor f = new FileDescriptor();
        SharedSecrets.getJavaIOFileDescriptorAccess().set(f, fd);
        return f;
    }

    private static FileChannel getInputChannel(int fd) {
        return new FileInputStream(getFd(fd)).getChannel();
    }

    private static FileChannel getOutputChannel(int fd) {
        return new FileOutputStream(getFd(fd)).getChannel();
    }

    @TruffleBoundary
    public static int open(String filename, int flags, @SuppressWarnings("unused") int mode) {
        FileDescriptor fd = null;
        int rdwr = flags & 0x3;
        try {
            switch (rdwr) {
                case O_RDONLY:
                    if (test(flags, O_TMPFILE)) {
                        return -LLVMAMD64Error.EINVAL;
                    }
                    fd = new FileInputStream(filename).getFD();
                    break;
                case O_WRONLY:
                    if (test(flags, O_TMPFILE)) {
                        return -LLVMAMD64Error.EOPNOTSUPP;
                    }
                    if (new File(filename).isDirectory()) {
                        return -LLVMAMD64Error.EISDIR;
                    }
                    fd = new FileOutputStream(filename).getFD();
                    break;
                case O_RDWR:
                    if (test(flags, O_TMPFILE)) {
                        return -LLVMAMD64Error.EOPNOTSUPP;
                    }
                    if (new File(filename).isDirectory()) {
                        return -LLVMAMD64Error.EISDIR;
                    }
                    fd = new RandomAccessFile(filename, "rw").getFD();
                    break;
                default:
                    return -LLVMAMD64Error.EINVAL;
            }
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.ENOENT;
        }

        return SharedSecrets.getJavaIOFileDescriptorAccess().get(fd);
    }

    @TruffleBoundary
    public static int close(int fd) {
        FileDescriptor f = getFd(fd);
        FileInputStream fin = new FileInputStream(f);
        try {
            fin.close();
            return 0;
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.EBADF;
        }
    }

    @TruffleBoundary
    public static int write(int fd, byte[] data) {
        FileDescriptor f = getFd(fd);
        FileOutputStream fout = new FileOutputStream(f);
        try {
            fout.write(data);
            return data.length;
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.EBADF;
        }
    }

    @TruffleBoundary
    public static int read(int fd, byte[] buf) {
        FileDescriptor f = getFd(fd);
        FileInputStream fin = new FileInputStream(f);
        try {
            int result = fin.read(buf);
            if (result == -1) { // EOF
                return 0;
            } else {
                return result;
            }
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.EBADF;
        }
    }

    @TruffleBoundary
    public static int write(int fd, LLVMAddress ptr, int size) {
        FileChannel chan = getOutputChannel(fd);
        ByteBuffer buf = LLVMAMD64String.getBuffer(ptr, size);
        try {
            return chan.write(buf);
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.EBADF;
        }
    }

    @TruffleBoundary
    public static int read(int fd, LLVMAddress ptr, int size) {
        FileChannel chan = getInputChannel(fd);
        ByteBuffer buf = LLVMAMD64String.getBuffer(ptr, size);
        try {
            int result = chan.read(buf);
            if (result == -1) { // EOF
                return 0;
            } else {
                return result;
            }
        } catch (IOException e) {
            // TODO: return proper error code
            return -LLVMAMD64Error.EBADF;
        }
    }

    public static long readv(int fd, LLVMAddress iov, long iovcnt) {
        if (iovcnt < 0) {
            return -LLVMAMD64Error.EINVAL;
        }
        LLVMAddress ptr = iov;
        long total = 0;
        for (int i = 0; i < iovcnt; i++) {
            LLVMAddress base = LLVMMemory.getAddress(ptr);
            ptr = ptr.increment(8);
            long size = LLVMMemory.getI64(ptr);
            ptr = ptr.increment(8);
            byte[] buf = new byte[(int) size];
            int bytes = read(fd, buf);
            if (bytes < 0) { // Error
                return bytes;
            }
            LLVMAMD64String.memcpy(buf, base, bytes);
            total += bytes;
            if (bytes < size) {
                break;
            }
        }
        return total;
    }

    public static long writev(int fd, LLVMAddress iov, long iovcnt) {
        if (iovcnt < 0) {
            return -LLVMAMD64Error.EINVAL;
        }
        LLVMAddress ptr = iov;
        long total = 0;
        for (int i = 0; i < iovcnt; i++) {
            LLVMAddress base = LLVMMemory.getAddress(ptr);
            ptr = ptr.increment(8);
            long size = LLVMMemory.getI64(ptr);
            ptr = ptr.increment(8);
            byte[] buf = LLVMAMD64String.memcpy(base, (int) size);
            int result = write(fd, buf);
            if (result < 0) {
                return result;
            }
            total += result;
        }
        return total;
    }

    @TruffleBoundary
    public static long lseek(int fd, long offset, int whence) {
        FileDescriptor f = getFd(fd);
        FileInputStream in = new FileInputStream(f);
        FileChannel chan = in.getChannel();
        try {
            long pos;
            switch (whence) {
                case SEEK_SET:
                    pos = offset;
                    break;
                case SEEK_CUR:
                    pos = chan.position() + offset;
                    break;
                case SEEK_END:
                    pos = chan.size() + offset;
                    break;
                default:
                    return -LLVMAMD64Error.EINVAL;
            }
            if (offset > 0 && pos < 0) {
                return -LLVMAMD64Error.EOVERFLOW;
            }
            if (pos < 0) {
                return -LLVMAMD64Error.EINVAL;
            }
            chan.position(pos);
            return pos;
        } catch (IOException e) {
            return -LLVMAMD64Error.EBADF;
        }
    }

    @SuppressWarnings("unused")
    public static long ioctl(int fd, int cmd, long arg) {
        return -LLVMAMD64Error.ENOSYS;
    }

    @SuppressWarnings("unused")
    public static long fcntl(int fd, int cmd, long arg) {
        return -LLVMAMD64Error.ENOSYS;
    }

    public static int ftruncate(int fd, long length) {
        FileChannel chan = getOutputChannel(fd);
        try {
            chan.truncate(length);
        } catch (IOException e) {
            return -LLVMAMD64Error.EBADF;
        }
        return 0;
    }

    @TruffleBoundary
    public static int stat(LLVMAddress pathname, LLVMAddress statbuf) {

        LLVMAddress ptr = statbuf;
        long dev = 0;
        long ino = 0;
        long mode = 0;
        long nlink = 0;
        long uid = 0;
        long gid = 0;
        long rdev = 0;
        long size = 0;
        long blksize = 0;
        long blocks = 0;
        // timespec at_atim
        // timespec at_mtim
        // timespec at_ctim

        LLVMMemory.putI64(ptr, dev);
        LLVMMemory.putI64(ptr.increment(8), ino);
        LLVMMemory.putI64(ptr.increment(16), mode);
        LLVMMemory.putI64(ptr.increment(24), nlink);
        LLVMMemory.putI64(ptr.increment(32), uid);
        LLVMMemory.putI64(ptr.increment(40), gid);
        LLVMMemory.putI64(ptr.increment(48), rdev);
        LLVMMemory.putI64(ptr.increment(56), size);
        LLVMMemory.putI64(ptr.increment(64), blksize);
        LLVMMemory.putI64(ptr.increment(72), blocks);
        return -LLVMAMD64Error.ENOSYS;
    }

    public static int dup2(int oldfd, int newfd) {
        return -LLVMAMD64Error.ENOSYS;
    }
}
