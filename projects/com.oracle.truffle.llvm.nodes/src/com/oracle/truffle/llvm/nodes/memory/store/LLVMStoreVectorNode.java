/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.memory.store;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariableAccess;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.vector.LLVMAddressVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMFunctionVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;

public abstract class LLVMStoreVectorNode extends LLVMStoreNode {

    public LLVMStoreVectorNode(Type type) {
        super(type, 0);
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMDoubleVector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMDoubleVector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMFloatVector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMFloatVector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMI16Vector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMI16Vector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMI1Vector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMI1Vector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMI32Vector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMI32Vector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMI64Vector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMI64Vector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMI8Vector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMI8Vector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMAddressVector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMAddressVector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMAddress address, LLVMFunctionVector value) {
        LLVMMemory.putVector(address, value);
        return null;
    }

    @Specialization
    protected Object writeVector(LLVMGlobalVariable address, LLVMFunctionVector value, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        LLVMMemory.putVector(globalAccess.getNativeLocation(address), value);
        return null;
    }
}
