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
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.CommonRustTypes.OptionType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.LLVMEnvArgsOs.ArgsOsType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.RustContext.OsStringType;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.RustContext.SysArgsType;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;

@NodeChildren({@NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class)})
public abstract class LLVMEnvArgsOsNext extends LLVMIntrinsic {
    private final RustContext rustContext;

    public LLVMEnvArgsOsNext(RustContext rustContext) {
        this.rustContext = rustContext;
    }

    protected OsStringType createOsString() {
        DataSpecConverter datalayout = getContextReference().get().getDataSpecConverter();
        return OsStringType.create(datalayout);
    }

    protected OptionType createOptionOfOsString(OsStringType osString) {
        return OptionType.create(osString.getType());
    }

    protected ArgsOsType createArgsOs() {
        DataSpecConverter datalayout = getContextReference().get().getDataSpecConverter();
        return ArgsOsType.create(datalayout);
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object doOp(LLVMAddress optionOfOsStringAddr, LLVMAddress argsOsAddr,
                    @Cached("createOsString()") OsStringType osString,
                    @Cached("createOptionOfOsString(osString)") OptionType optionOfOsString,
                    @Cached("createArgsOs()") ArgsOsType argsOs,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        long osStringAddr = optionOfOsString.getElementAddr(optionOfOsStringAddr.getVal());
        SysArgsType sysArgsType = rustContext.getSysArgs().getType();
        argsOs.next(memory, argsOsAddr.getVal(), osStringAddr, sysArgsType);
        return null;
    }

}
