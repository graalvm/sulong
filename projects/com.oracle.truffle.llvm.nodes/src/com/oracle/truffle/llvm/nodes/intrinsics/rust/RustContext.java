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
package com.oracle.truffle.llvm.nodes.intrinsics.rust;

import java.util.Arrays;

import com.oracle.truffle.llvm.nodes.intrinsics.rust.CommonRustTypes.IntoIterType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.CommonRustTypes.VecU8Type;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

public final class RustContext {

    static final int SIZED_TYPE_CAP = 0;
    static final int ZERO_SIZED_TYPE_CAP = -1;

    private SysArgs sysArgs;

    void sysInit(LLVMMemory memory, DataSpecConverter datalayout, Object[] args) {
        this.sysArgs = new SysArgs(memory, datalayout, args);
    }

    SysArgs getSysArgs() {
        return sysArgs;
    }

    void dispose(LLVMMemory memory) {
        sysArgs.destruct(memory);
    }

    static final class SysArgs {

        private final SysArgsType type;
        private final LLVMAddress address;
        private final Object[] args;

        SysArgs(LLVMMemory memory, DataSpecConverter datalayout, Object[] args) {
            this.type = SysArgsType.create(datalayout, args.length);
            this.address = type.allocate(memory);
            this.args = args;
            type.write(memory, address.getVal(), args);
        }

        void intoIter(LLVMMemory memory, long intoIterAddress, IntoIterType intoIter) {
            type.intoIter(memory, address.getVal(), intoIterAddress, intoIter);
        }

        void destruct(LLVMMemory memory) {
            type.destruct(memory, address.getVal(), args);
        }

        SysArgsType getType() {
            return type;
        }

        LLVMAddress getAddress() {
            return address;
        }

    }

    static final class SysArgsType {

        private final Type type;
        private final OsStringType osString;
        private final int size;
        private final int offsetOsString;

        private SysArgsType(DataSpecConverter datalayout, StructureType type, OsStringType osString) {
            this.type = type;
            this.osString = osString;
            this.size = type.getSize(datalayout);
            this.offsetOsString = osString.getType().getSize(datalayout);
        }

        LLVMAddress allocate(LLVMMemory memory) {
            return memory.allocateMemory(size);
        }

        void destruct(LLVMMemory memory, long address, Object[] args) {
            long sysArgsAddr = address;
            for (int i = 0; i < args.length; i++) {
                osString.destruct(memory, sysArgsAddr);
                sysArgsAddr += offsetOsString;
            }
            memory.free(address);
        }

        // trait-IntoIterator::into_iter
        void intoIter(LLVMMemory memory, long address, long intoIterAddress, IntoIterType intoIter) {
            intoIter.write(memory, intoIterAddress, address, address + size);
        }

        void write(LLVMMemory memory, long address, Object[] args) {
            long sysArgsAddr = address;
            for (int i = 0; i < args.length; i++) {
                osString.write(memory, sysArgsAddr, args[i].toString());
                sysArgsAddr += offsetOsString;
            }
        }

        int getArgOffset() {
            return offsetOsString;
        }

        Type getType() {
            return type;
        }

        static SysArgsType create(DataSpecConverter datalayout, int argc) {
            OsStringType osString = OsStringType.create(datalayout);
            Type[] elemTypes = new Type[argc];
            Arrays.fill(elemTypes, osString.getType());
            StructureType type = new StructureType(true, elemTypes);
            return new SysArgsType(datalayout, type, osString);
        }

    }

    static final class OsStringType {

        private final Type type;
        private final VecU8Type vecu8;

        private OsStringType(StructureType type, VecU8Type vecu8) {
            this.type = type;
            this.vecu8 = vecu8;
        }

        void destruct(LLVMMemory memory, long address) {
            vecu8.destruct(memory, address);
        }

        String read(LLVMMemory memory, long address) {
            return vecu8.readString(memory, address);
        }

        void write(LLVMMemory memory, long address, String str) {
            vecu8.writeString(memory, address, str);
        }

        void copy(LLVMMemory memory, long srcAddr, long destAddr) {
            vecu8.copy(memory, srcAddr, destAddr);
        }

        Type getType() {
            return type;
        }

        static OsStringType create(DataSpecConverter datalayout) {
            VecU8Type vecu8 = VecU8Type.create(datalayout);
            StructureType buf = new StructureType(false, new Type[]{vecu8.getType()});
            StructureType type = new StructureType(false, new Type[]{buf});
            return new OsStringType(type, vecu8);
        }

    }

}
