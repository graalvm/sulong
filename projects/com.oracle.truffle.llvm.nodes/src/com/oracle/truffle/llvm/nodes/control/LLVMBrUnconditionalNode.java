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
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

@Instrumentable(factory = LLVMBrUnconditionalNodeWrapper.class)
public class LLVMBrUnconditionalNode extends LLVMControlFlowNode {

    @Child private LLVMExpressionNode phi;
    private final int successor;

    public LLVMBrUnconditionalNode(int successor, LLVMExpressionNode phi, SourceSection sourceSection) {
        super(sourceSection);
        this.successor = successor;
        this.phi = phi;
    }

    public LLVMBrUnconditionalNode(LLVMBrUnconditionalNode wrappedNode) {
        super(wrappedNode.getSourceSection());
        this.successor = wrappedNode.successor;
        this.phi = wrappedNode.phi;
    }

    @Override
    public int getSuccessorCount() {
        return 1;
    }

    @Override
    public LLVMExpressionNode getPhiNode(int successorIndex) {
        assert successorIndex == 0;
        return phi;
    }

    public int getSuccessor() {
        return successor;
    }

    public void execute(@SuppressWarnings("unused") VirtualFrame frame) {
        // we need an execute method so the debugger can step on this node
    }
}
