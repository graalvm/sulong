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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

final class CommonRustTypes {

    static class VecType {

        final StructureType type;
        final StructureType rawVec;
        final long offsetVecLen;
        final long offsetRawVecSize;
        final int vecCap;

        VecType(DataSpecConverter datalayout, StructureType type, StructureType rawVec, int vecCap) {
            this.type = type;
            this.rawVec = rawVec;
            this.offsetVecLen = type.getOffsetOf(1, datalayout);
            this.offsetRawVecSize = rawVec.getOffsetOf(1, datalayout);
            this.vecCap = vecCap;
        }

        void copy(LLVMMemory memory, long srcAddr, long destAddr) {
            memory.putI64(destAddr + offsetRawVecSize, vecCap);
            long srcVecLen = memory.getI64(srcAddr + offsetVecLen);
            memory.putI64(destAddr + offsetVecLen, srcVecLen);
            LLVMAddress srcElemAddr = memory.getAddress(srcAddr);
            memory.putAddress(destAddr, srcElemAddr);
        }

        Type getType() {
            return type;
        }

        static VecType create(DataSpecConverter datalayout, Type element, int vecCap) {
            StructureType nonZero = new StructureType(false, new Type[]{new PointerType(element)});
            StructureType phantomData = new StructureType(false, new Type[]{});
            StructureType unique = new StructureType(false, new Type[]{nonZero, phantomData});
            StructureType rawVec = new StructureType(false, new Type[]{unique, PrimitiveType.I64});
            StructureType type = new StructureType(false, new Type[]{rawVec, PrimitiveType.I64});
            return new VecType(datalayout, type, rawVec, vecCap);
        }

    }

    static final class VecU8Type extends VecType {

        private VecU8Type(DataSpecConverter datalayout, StructureType type, StructureType rawVec, int vecCap) {
            super(datalayout, type, rawVec, vecCap);
        }

        @SuppressWarnings("static-method")
        void destruct(LLVMMemory memory, long address) {
            LLVMAddress strAddr = memory.getAddress(address);
            memory.free(strAddr);
        }

        @TruffleBoundary
        String readString(LLVMMemory memory, long address) {
            int vecLen = memory.getI32(address + offsetVecLen); // long to int
            long strAddr = memory.getAddress(address).getVal();
            if (strAddr == LLVMAddress.nullPointer().getVal()) {
                return null;
            }
            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < vecLen; i++) {
                strBuilder.append((char) Byte.toUnsignedInt(memory.getI8(strAddr)));
                strAddr += Byte.BYTES;
            }
            return strBuilder.toString();
        }

        void writeString(LLVMMemory memory, long address, String str) {
            memory.putI64(address + offsetRawVecSize, vecCap);

            long vecLen;
            LLVMAddress strAddr;
            if (str == null) {
                vecLen = 0;
                strAddr = LLVMAddress.nullPointer();
            } else {
                vecLen = str.length();
                strAddr = memory.allocateRustString(str);
            }
            memory.putI64(address + offsetVecLen, vecLen);
            memory.putAddress(address, strAddr);
        }

        static VecU8Type create(DataSpecConverter datalayout) {
            VecType vec = VecType.create(datalayout, PrimitiveType.I8, RustContext.SIZED_TYPE_CAP);
            return new VecU8Type(datalayout, vec.type, vec.rawVec, vec.vecCap);
        }

    }

    static final class StrSliceType {

        private final Type type;
        private final long lengthOffset;

        private StrSliceType(DataSpecConverter dataLayout, StructureType type) {
            this.type = type;
            this.lengthOffset = type.getOffsetOf(1, dataLayout);
        }

        @TruffleBoundary
        String read(LLVMMemory memory, long address) {
            long strAddr = memory.getAddress(address).getVal();
            int strLen = memory.getI32(address + lengthOffset);
            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < strLen; i++) {
                strBuilder.append((char) Byte.toUnsignedInt(memory.getI8(strAddr)));
                strAddr += Byte.BYTES;
            }
            return strBuilder.toString();
        }

        Type getType() {
            return type;
        }

        static StrSliceType create(DataSpecConverter dataLayout) {
            StructureType type = new StructureType(false, new Type[]{new PointerType(PrimitiveType.I8), PrimitiveType.I64});
            return new StrSliceType(dataLayout, type);
        }

    }

    static class IntoIterType {

        final StructureType type;
        final long offsetIterCap;
        final long offsetBeginPtr;
        final long offsetEndPtr;
        final int iterCap;

        IntoIterType(DataSpecConverter datalayout, StructureType type, int iterCap) {
            this.type = type;
            this.offsetIterCap = type.getOffsetOf(1, datalayout);
            this.offsetBeginPtr = type.getOffsetOf(2, datalayout);
            this.offsetEndPtr = type.getOffsetOf(3, datalayout);
            this.iterCap = iterCap;
        }

        void write(LLVMMemory memory, long address, long beginAddr, long endAddr) {
            memory.putAddress(address, beginAddr);
            memory.putI64(address + offsetIterCap, iterCap);
            memory.putAddress(address + offsetBeginPtr, beginAddr);
            memory.putAddress(address + offsetEndPtr, endAddr);
        }

        LLVMAddress next(LLVMMemory memory, long address, int offsetElem) {
            LLVMAddress sharedAddr = memory.getAddress(address);
            LLVMAddress endAddr = memory.getAddress(address + offsetEndPtr);
            if (endAddr.getVal() == sharedAddr.getVal()) {
                return null;
            } else {
                memory.putAddress(address, sharedAddr.getVal() + offsetElem);
                return sharedAddr;
            }
        }

        Type getType() {
            return type;
        }

        static IntoIterType create(DataSpecConverter datalayout, Type element, int iterCap) {
            PointerType ptrElem = new PointerType(element);
            StructureType nonZero = new StructureType(false, new Type[]{ptrElem});
            StructureType phantomData = new StructureType(false, new Type[]{});
            StructureType sharedElem = new StructureType(false, new Type[]{nonZero, phantomData});
            StructureType type = new StructureType(false, new Type[]{
                            sharedElem, PrimitiveType.I64, ptrElem, ptrElem
            });
            return new IntoIterType(datalayout, type, iterCap);
        }

    }

    static class OptionType {

        final Type type;

        OptionType(StructureType type) {
            this.type = type;
        }

        long getElementAddr(long address) {
            return address;
        }

        Type getType() {
            return type;
        }

        static OptionType create(Type element) {
            StructureType type = new StructureType(false, new Type[]{element});
            return new OptionType(type);
        }

    }

}
