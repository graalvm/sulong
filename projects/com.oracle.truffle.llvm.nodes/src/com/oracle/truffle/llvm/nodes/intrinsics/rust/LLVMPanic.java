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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobal;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMToNativeNode;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

@NodeChild(type = LLVMExpressionNode.class)
public abstract class LLVMPanic extends LLVMIntrinsic {

    protected PanicLocType createPanicLocation() {
        DataSpecConverter dataSpecConverter = getContextReference().get().getDataSpecConverter();
        return PanicLocType.create(dataSpecConverter);
    }

    @Specialization
    protected Object doOp(VirtualFrame frame, LLVMGlobal panicLocVar,
                    @Cached("toNative()") LLVMToNativeNode globalAccess,
                    @Cached("createPanicLocation()") PanicLocType panicLoc,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        LLVMAddress addr = globalAccess.executeWithTarget(frame, panicLocVar);
        CompilerDirectives.transferToInterpreter();
        throw panicLoc.read(memory, addr.getVal());
    }

    static final class PanicLocType {

        private final StrSliceType strslice;
        private final long offsetFilename;
        private final long offsetLineNr;

        private PanicLocType(DataSpecConverter dataLayout, Type type, StrSliceType strslice) {
            this.strslice = strslice;
            StructureType structureType = (StructureType) ((PointerType) type).getElementType(0);
            this.offsetFilename = structureType.getOffsetOf(1, dataLayout);
            this.offsetLineNr = structureType.getOffsetOf(2, dataLayout);
        }

        RustPanicException read(LLVMMemory memory, long address) {
            CompilerAsserts.neverPartOfCompilation();
            String desc = strslice.read(memory, address);
            String filename = strslice.read(memory, address + offsetFilename);
            int linenr = memory.getI32(address + offsetLineNr);
            return new RustPanicException(desc, filename, linenr);
        }

        static PanicLocType create(DataSpecConverter dataLayout) {
            CompilerAsserts.neverPartOfCompilation();
            StrSliceType strslice = StrSliceType.create(dataLayout);
            Type type = new PointerType((new StructureType(false, new Type[]{strslice.getType(), strslice.getType(), PrimitiveType.I32})));
            return new PanicLocType(dataLayout, type, strslice);
        }
    }

    private static final class StrSliceType {

        private final long lengthOffset;
        private final Type type;

        private StrSliceType(DataSpecConverter dataLayout, Type type) {
            this.lengthOffset = ((StructureType) type).getOffsetOf(1, dataLayout);
            this.type = type;
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

        public Type getType() {
            return type;
        }

        static StrSliceType create(DataSpecConverter dataLayout) {
            Type type = new StructureType(false, new Type[]{new PointerType(PrimitiveType.I8), PrimitiveType.I64});
            return new StrSliceType(dataLayout, type);
        }
    }
}
