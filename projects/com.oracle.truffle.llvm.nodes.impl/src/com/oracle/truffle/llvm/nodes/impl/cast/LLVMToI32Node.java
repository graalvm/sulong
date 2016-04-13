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
package com.oracle.truffle.llvm.nodes.impl.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMAddressNode;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMFunctionNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVM80BitFloatNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVMDoubleNode;
import com.oracle.truffle.llvm.nodes.impl.base.floating.LLVMFloatNode;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI16Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI1Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI32Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI64Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMI8Node;
import com.oracle.truffle.llvm.nodes.impl.base.integers.LLVMIVarBitNode;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.types.LLVMIVarBit;
import com.oracle.truffle.llvm.types.floating.LLVM80BitFloat;

public abstract class LLVMToI32Node extends LLVMI32Node {

    @NodeChild(value = "fromNode", type = LLVMI1Node.class)
    public abstract static class LLVMI1ToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(boolean from) {
            return from ? -1 : 0;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI1Node.class)
    public abstract static class LLVMI1ToI32ZeroExtNode extends LLVMToI32Node {

        @Specialization
        public int executeI32(boolean from) {
            return from ? 1 : 0;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI8Node.class)
    public abstract static class LLVMI8ToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(byte from) {
            return from;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI8Node.class)
    public abstract static class LLVMI8ToI32ZeroExtNode extends LLVMToI32Node {

        @Specialization
        public int executeI32(byte from) {
            return from & LLVMI8Node.MASK;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI16Node.class)
    public abstract static class LLVMI16ToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(short from) {
            return from;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI16Node.class)
    public abstract static class LLVMI16ToI32ZeroExtNode extends LLVMToI32Node {

        @Specialization
        public int executeI32(short from) {
            return from & LLVMI16Node.MASK;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMAddressNode.class)
    public abstract static class LLVMAddressToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(LLVMAddress from) {
            return (int) from.getVal();
        }
    }

    @NodeChild(value = "fromNode", type = LLVMI64Node.class)
    public abstract static class LLVMI64ToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(long from) {
            return (int) from;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMIVarBitNode.class)
    public abstract static class LLVMIVarBitToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(LLVMIVarBit from) {
            return from.getIntValue();
        }
    }

    @NodeChild(value = "fromNode", type = LLVMIVarBitNode.class)
    public abstract static class LLVMIVarBitToI32ZeroExtNode extends LLVMToI32Node {

        @Specialization
        public int executeI32(LLVMIVarBit from) {
            return from.getZeroExtendedIntValue();
        }
    }

    @NodeChild(value = "fromNode", type = LLVMFloatNode.class)
    public abstract static class LLVMFloatToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(float from) {
            return (int) from;
        }
    }

    @NodeChild(value = "fromNode", type = LLVMFloatNode.class)
    public abstract static class LLVMFloatToI32BitNode extends LLVMToI32Node {

        @Specialization
        public int executeI32(float from) {
            return Float.floatToIntBits(from);
        }
    }

    @NodeChild(value = "fromNode", type = LLVMDoubleNode.class)
    public abstract static class LLVMDoubleToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(double from) {
            return (int) from;
        }
    }

    @NodeChild(value = "fromNode", type = LLVM80BitFloatNode.class)
    public abstract static class LLVM80BitFloatToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(LLVM80BitFloat from) {
            return from.getIntValue();
        }
    }

    @NodeChild(value = "fromNode", type = LLVMFunctionNode.class)
    public abstract static class LLVMFunctionToI32Node extends LLVMToI32Node {

        @Specialization
        public int executeI32(LLVMFunctionDescriptor from) {
            return from.getFunctionIndex();
        }
    }

}
