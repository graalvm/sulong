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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.CommonRustTypes.IntoIterType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.RustContext.OsStringType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.RustContext.SysArgsType;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

@NodeChild(type = LLVMExpressionNode.class)
public abstract class LLVMEnvArgsOs extends LLVMIntrinsic {

    private final RustContext rustContext;

    public LLVMEnvArgsOs(RustContext rustContext) {
        this.rustContext = rustContext;
    }

    protected ArgsOsType createArgsOs() {
        DataSpecConverter datalayout = getContextReference().get().getDataSpecConverter();
        return ArgsOsType.create(datalayout);
    }

    @Specialization
    public Object doOp(LLVMAddress argsOsAddr,
                    @Cached("createArgsOs()") ArgsOsType argsOs,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        rustContext.getSysArgs().intoIter(memory, argsOs.getIntoIterAddress(argsOsAddr.getVal()), argsOs.getIntoIter());
        return null;
    }

    static final class ArgsOsType {

        private final Type type;
        private final OsStringType osString;
        private final IntoIterType intoIterOfOsString;

        private ArgsOsType(StructureType type, IntoIterType intoIterOfOsString, OsStringType osString) {
            this.type = type;
            this.osString = osString;
            this.intoIterOfOsString = intoIterOfOsString;
        }

        // trait-Iterator::next
        void next(LLVMMemory memory, long address, long osStringAddr, SysArgsType sysArgsType) {
            LLVMAddress nextAddr = intoIterOfOsString.next(memory, address, sysArgsType.getArgOffset());
            if (nextAddr == null) {
                osString.write(memory, osStringAddr, null);
            } else {
                osString.copy(memory, nextAddr.getVal(), osStringAddr);
            }
        }

        IntoIterType getIntoIter() {
            return intoIterOfOsString;
        }

        @SuppressWarnings("static-method")
        long getIntoIterAddress(long address) {
            return address;
        }

        Type getType() {
            return type;
        }

        static ArgsOsType create(DataSpecConverter datalayout) {
            OsStringType osString = OsStringType.create(datalayout);
            IntoIterType intoIterOfOsString = IntoIterType.create(datalayout, osString.getType(), RustContext.SIZED_TYPE_CAP);
            StructureType phantomData = new StructureType(false, new Type[]{});
            StructureType impArgs = new StructureType(false, new Type[]{intoIterOfOsString.getType(), phantomData});
            StructureType type = new StructureType(false, new Type[]{impArgs});
            return new ArgsOsType(type, intoIterOfOsString, osString);
        }

    }

}
