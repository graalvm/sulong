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
package com.oracle.truffle.llvm.nodes.vars;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.nodes.base.LLVMStructWriteNode;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.types.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.types.memory.LLVMHeap;
import com.oracle.truffle.llvm.types.memory.LLVMMemory;

public class StructLiteralNode extends LLVMExpressionNode {

    public static class LLVMI1StructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMI1StructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            boolean value = LLVMExpressionNode.expectI1(valueNode, frame);
            LLVMMemory.putI1(address, value);
            return null;
        }
    }

    public static class LLVMI8StructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMI8StructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            byte value = LLVMExpressionNode.expectI8(valueNode, frame);
            LLVMMemory.putI8(address, value);
            return null;
        }
    }

    public static class LLVMI16StructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMI16StructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            short value = LLVMExpressionNode.expectI16(valueNode, frame);
            LLVMMemory.putI16(address, value);
            return null;
        }
    }

    public static class LLVMI32StructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMI32StructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            int value = LLVMExpressionNode.expectI32(valueNode, frame);
            LLVMMemory.putI32(address, value);
            return null;
        }
    }

    public static class LLVMI64StructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMI64StructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            long value = LLVMExpressionNode.expectI64(valueNode, frame);
            LLVMMemory.putI64(address, value);
            return null;
        }

    }

    public static class LLVMFloatStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMFloatStructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            float value = LLVMExpressionNode.expectFloat(valueNode, frame);
            LLVMMemory.putFloat(address, value);
            return null;
        }

    }

    public static class LLVMDoubleStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMDoubleStructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            double value = LLVMExpressionNode.expectDouble(valueNode, frame);
            LLVMMemory.putDouble(address, value);
            return null;
        }

    }

    public static class LLVM80BitFloatStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVM80BitFloatStructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            LLVM80BitFloat value = LLVMExpressionNode.expectLLVM80BitFloat(valueNode, frame);
            LLVMMemory.put80BitFloat(address, value);
            return null;
        }

    }

    public static class LLVMCompoundStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;
        private int size;

        public LLVMCompoundStructWriteNode(LLVMExpressionNode valueNode, int size) {
            this.valueNode = valueNode;
            this.size = size;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            LLVMAddress value = LLVMExpressionNode.expectLLVMAddress(valueNode, frame);
            LLVMHeap.memCopy(address, value, size);
            return null;
        }

    }

    public static class LLVMEmptyStructWriteNode extends LLVMStructWriteNode {

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            return null;
        }

    }

    @Child private LLVMExpressionNode address;
    @CompilationFinal(dimensions = 1) private final int[] offsets;
    @Children private final LLVMStructWriteNode[] elementWriteNodes;

    public StructLiteralNode(int[] offsets, LLVMStructWriteNode[] elementWriteNodes, LLVMExpressionNode address) {
        this.offsets = offsets;
        this.elementWriteNodes = elementWriteNodes;
        this.address = address;
    }

    @Override
    @ExplodeLoop
    public LLVMAddress executeLLVMAddress(VirtualFrame frame) {
        LLVMAddress addr = LLVMExpressionNode.expectLLVMAddress(address, frame);
        for (int i = 0; i < offsets.length; i++) {
            LLVMAddress currentAddr = addr.increment(offsets[i]);
            elementWriteNodes[i].executeWrite(frame, currentAddr);
        }
        return addr;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLLVMAddress(frame);
    }

    public static class LLVMAddressStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMAddressStructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            LLVMAddress value = LLVMExpressionNode.expectLLVMAddress(valueNode, frame);
            LLVMMemory.putAddress(address, value);
            return null;
        }

    }

    public static class LLVMFunctionStructWriteNode extends LLVMStructWriteNode {

        @Child private LLVMExpressionNode valueNode;

        public LLVMFunctionStructWriteNode(LLVMExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeWrite(VirtualFrame frame, LLVMAddress address) {
            LLVMFunctionDescriptor value = LLVMExpressionNode.expectLLVMFunctionDescriptor(valueNode, frame);
            LLVMHeap.putFunctionIndex(address, value.getFunctionIndex());
            return null;
        }

    }

}
