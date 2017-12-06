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
package com.oracle.truffle.llvm.nodes.intrinsics.c;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI64StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.LLVMLongjmpException;
import com.oracle.truffle.llvm.runtime.LLVMLongjmpTarget;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

@NodeChild(value = "jmpbuf", type = LLVMExpressionNode.class)
public abstract class LLVMSetjmp extends LLVMIntrinsic {
    @Child private LLVMStoreNode storeI64 = LLVMI64StoreNodeGen.create();

    private static LLVMLongjmpTarget getPC() {
        CompilerDirectives.transferToInterpreter();
        Frame frame = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY);
        FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(LLVMLongjmpException.CURRENT_INSTRUCTION_FRAME_SLOT_ID);
        return (LLVMLongjmpTarget) FrameUtil.getObjectSafe(frame, slot);
    }

    private static int getReturnValue() {
        CompilerDirectives.transferToInterpreter();
        Frame frame = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY);
        FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(LLVMLongjmpException.SETJMP_RETURN_VALUE_FRAME_SLOT_ID);
        return FrameUtil.getIntSafe(frame, slot);
    }

    private void storeFrame(long id) {
        CompilerDirectives.transferToInterpreter();
        Frame frame = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY);
        MaterializedFrame materialized = frame.materialize();
        FrameDescriptor descriptor = materialized.getFrameDescriptor();
        List<? extends FrameSlot> slots = descriptor.getSlots();
        Object[] data = new Object[slots.size()];
        FrameDescriptor newfd = descriptor.copy();
        for (FrameSlot slot : slots) {
            newfd.findFrameSlot(slot.getIdentifier()).setKind(slot.getKind());
            int i = slot.getIndex();
            switch (slot.getKind()) {
                case Boolean:
                    data[i] = FrameUtil.getBooleanSafe(materialized, slot);
                    break;
                case Byte:
                    data[i] = FrameUtil.getByteSafe(materialized, slot);
                    break;
                case Int:
                    data[i] = FrameUtil.getIntSafe(materialized, slot);
                    break;
                case Long:
                    data[i] = FrameUtil.getLongSafe(materialized, slot);
                    break;
                case Float:
                    data[i] = FrameUtil.getFloatSafe(materialized, slot);
                    break;
                case Double:
                    data[i] = FrameUtil.getDoubleSafe(materialized, slot);
                    break;
                case Object:
                    data[i] = FrameUtil.getObjectSafe(materialized, slot);
                    break;
                case Illegal:
                    data[i] = null;
            }
        }
        getContextReference().get().storeSetjmpEnvironment(id, newfd, data);
    }

    @Specialization
    protected int doOp(VirtualFrame frame, Object env) {
        int returnValue = getReturnValue();
        if (returnValue == 0) {
            LLVMLongjmpTarget target = getPC();
            storeFrame(target.getHash());
            storeI64.executeWithTarget(frame, env, target.getHash());
        }
        return returnValue;
    }
}
