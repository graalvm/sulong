/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.impl.memory.load;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMAddressNode;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI32Node;
import com.oracle.truffle.llvm.nodes.impl.intrinsics.interop.ToLLVMNode;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.LLVMTruffleObject;
import com.oracle.truffle.llvm.types.memory.LLVMMemory;

@NodeChild(type = LLVMAddressNode.class)
public abstract class LLVMI32LoadNode extends LLVMI32Node {
    @Child protected Node foreignRead = Message.READ.createNode();
    @Child protected ToLLVMNode toLLVM = new ToLLVMNode();
    protected static final Class<?> type = int.class;

    protected int doForeignAccess(VirtualFrame frame, LLVMTruffleObject addr) {
        try {
            int index = (int) (addr.getOffset() / LLVMI32Node.BYTE_SIZE);
            Object value = ForeignAccess.sendRead(foreignRead, frame, addr.getObject(), index);
            return (int) toLLVM.convert(frame, value, type);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            throw new IllegalStateException(e);
        }
    }

    public abstract static class LLVMI32DirectLoadNode extends LLVMI32LoadNode {

        @Specialization
        public int executeI32(LLVMAddress addr) {
            return LLVMMemory.getI32(addr);
        }

        @Specialization
        public int executeI32(VirtualFrame frame, LLVMTruffleObject addr) {
            return doForeignAccess(frame, addr);
        }

    }

    public abstract static class LLVMI32ProfilingLoadNode extends LLVMI32LoadNode {

        private final IntValueProfile profile = IntValueProfile.createIdentityProfile();

        @Specialization
        public int executeI32(LLVMAddress addr) {
            int val = LLVMMemory.getI32(addr);
            return profile.profile(val);
        }

        @Specialization
        public int executeI32(VirtualFrame frame, LLVMTruffleObject addr) {
            return doForeignAccess(frame, addr);
        }

    }

}
