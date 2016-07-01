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
package com.oracle.truffle.llvm.nodes.impl.intrinsics.interop;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.nodes.base.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.impl.intrinsics.llvm.LLVMIntrinsic.LLVMBooleanIntrinsic;
import com.oracle.truffle.llvm.types.LLVMTruffleObject;

public final class LLVMTruffleBinary {

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMTruffleIsBoxed extends LLVMBooleanIntrinsic {

        @Child private Node foreignIsBoxed = Message.IS_BOXED.createNode();

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, LLVMTruffleObject value) {
            if (value.getOffset() != 0 || value.getName() != null) {
                throw new IllegalAccessError("Pointee must be unmodified");
            }
            return ForeignAccess.sendIsBoxed(foreignIsBoxed, frame, value.getObject());
        }

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, TruffleObject value) {
            return ForeignAccess.sendIsBoxed(foreignIsBoxed, frame, value);
        }
    }

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMTruffleIsExecutable extends LLVMBooleanIntrinsic {

        @Child private Node foreignIsExecutable = Message.IS_EXECUTABLE.createNode();

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, LLVMTruffleObject value) {
            if (value.getOffset() != 0 || value.getName() != null) {
                throw new IllegalAccessError("Pointee must be unmodified");
            }
            return ForeignAccess.sendIsExecutable(foreignIsExecutable, frame, value.getObject());
        }

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, TruffleObject value) {
            return ForeignAccess.sendIsExecutable(foreignIsExecutable, frame, value);
        }
    }

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMTruffleIsNull extends LLVMBooleanIntrinsic {

        @Child private Node foreignIsNull = Message.IS_NULL.createNode();

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, LLVMTruffleObject value) {
            if (value.getOffset() != 0 || value.getName() != null) {
                throw new IllegalAccessError("Pointee must be unmodified");
            }
            return ForeignAccess.sendIsNull(foreignIsNull, frame, value.getObject());
        }

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, TruffleObject value) {
            return ForeignAccess.sendIsNull(foreignIsNull, frame, value);
        }
    }

    @NodeChildren({@NodeChild(type = LLVMExpressionNode.class)})
    public abstract static class LLVMTruffleHasSize extends LLVMBooleanIntrinsic {

        @Child private Node foreignHasSize = Message.HAS_SIZE.createNode();

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, LLVMTruffleObject value) {
            if (value.getOffset() != 0 || value.getName() != null) {
                throw new IllegalAccessError("Pointee must be unmodified");
            }
            return ForeignAccess.sendHasSize(foreignHasSize, frame, value.getObject());
        }

        @Specialization
        public boolean executeIntrinsic(VirtualFrame frame, TruffleObject value) {
            return ForeignAccess.sendHasSize(foreignHasSize, frame, value);
        }
    }
}
