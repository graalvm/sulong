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
package com.oracle.truffle.llvm.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.NodeCost;

public final class LLVMBrUnconditionalNodeWrapper implements InstrumentableFactory<LLVMBrUnconditionalNode> {

    @Override
    public WrapperNode createWrapper(LLVMBrUnconditionalNode delegateNode, ProbeNode probeNode) {
        return new LLVMBrUnconditionalNodeWrapper0(delegateNode, delegateNode, probeNode);
    }

    private static final class LLVMBrUnconditionalNodeWrapper0 extends LLVMBrUnconditionalNode implements WrapperNode {

        @Child private LLVMBrUnconditionalNode delegateNode;
        @Child private ProbeNode probeNode;

        private LLVMBrUnconditionalNodeWrapper0(LLVMBrUnconditionalNode wrappedNode, LLVMBrUnconditionalNode delegateNode, ProbeNode probeNode) {
            super(wrappedNode);
            this.delegateNode = delegateNode;
            this.probeNode = probeNode;
        }

        @Override
        public LLVMBrUnconditionalNode getDelegateNode() {
            return delegateNode;
        }

        @Override
        public ProbeNode getProbeNode() {
            return probeNode;
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }

        @Override
        public void execute(VirtualFrame frame) {
            // this class is not created automatically simply because the
            // @SuppressWarnings("unused") in LLVMBrUnconditionalNode::execute would be copied over
            // and cause a compile warning
            try {
                probeNode.onEnter(frame);
                delegateNode.execute(frame);
                probeNode.onReturnValue(frame, null);
            } catch (Throwable t) {
                probeNode.onReturnExceptional(frame, t);
                throw t;
            }
        }
    }
}
