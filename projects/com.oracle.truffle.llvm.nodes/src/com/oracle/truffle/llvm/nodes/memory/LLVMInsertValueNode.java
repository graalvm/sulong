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
package com.oracle.truffle.llvm.nodes.memory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.memory.LLVMHeap;
import com.oracle.truffle.llvm.types.memory.LLVMMemory;

public abstract class LLVMInsertValueNode extends LLVMExpressionNode {

    @Child LLVMExpressionNode sourceAggregate;
    @Child LLVMExpressionNode targetAggregate;
    final int sourceAggregateSize;
    final int offset;

    public LLVMInsertValueNode(LLVMExpressionNode sourceAggregate, LLVMExpressionNode targetAggregate, int sourceAggregateSize, int offset) {
        this.sourceAggregate = sourceAggregate;
        this.targetAggregate = targetAggregate;
        this.sourceAggregateSize = sourceAggregateSize;
        this.offset = offset;
    }

    @Override
    public LLVMAddress executeLLVMAddress(VirtualFrame frame) {
        LLVMAddress sourceAggr = LLVMExpressionNode.expectLLVMAddress(sourceAggregate, frame);
        LLVMAddress targetAggr = LLVMExpressionNode.expectLLVMAddress(targetAggregate, frame);
        LLVMHeap.memCopy(targetAggr, sourceAggr, sourceAggregateSize);
        return targetAggr;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLLVMAddress(frame);
    }

    public static class LLVMInsertFloatValueNode extends LLVMInsertValueNode {

        @Child private LLVMExpressionNode element;

        public LLVMInsertFloatValueNode(LLVMExpressionNode sourceAggregate, LLVMExpressionNode targetAggregate, int sourceAggregateSize, int offset, LLVMExpressionNode element) {
            super(sourceAggregate, targetAggregate, sourceAggregateSize, offset);
            this.element = element;
        }

        @Override
        public LLVMAddress executeLLVMAddress(VirtualFrame frame) {
            LLVMAddress targetAggr = super.executeLLVMAddress(frame);
            LLVMAddress insertPosition = targetAggr.increment(offset);
            float value = LLVMExpressionNode.expectFloat(element, frame);
            LLVMMemory.putFloat(insertPosition, value);
            return targetAggr;
        }
    }

    public static class LLVMInsertDoubleValueNode extends LLVMInsertValueNode {

        @Child private LLVMExpressionNode element;

        public LLVMInsertDoubleValueNode(LLVMExpressionNode sourceAggregate, LLVMExpressionNode targetAggregate, int sourceAggregateSize, int offset, LLVMExpressionNode element) {
            super(sourceAggregate, targetAggregate, sourceAggregateSize, offset);
            this.element = element;
        }

        @Override
        public LLVMAddress executeLLVMAddress(VirtualFrame frame) {
            LLVMAddress targetAggr = super.executeLLVMAddress(frame);
            LLVMAddress insertPosition = targetAggr.increment(offset);
            double value = LLVMExpressionNode.expectDouble(element, frame);
            LLVMMemory.putDouble(insertPosition, value);
            return targetAggr;
        }
    }

}
