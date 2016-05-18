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
package com.oracle.truffle.llvm.nodes.impl.base;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.nodes.base.LLVMStatementNode;

/**
 * This node represents a terminator instruction in LLVM IR. This node decides which basic block is
 * executed next, or terminates execution of a function.
 *
 * Since this class has an additional field, it can't be simply wrapped by the wrapper node
 * generated from super class {@link LLVMStatementNode}. Therefore this class is also {@link Instrumentable}.
 *
 * @see <a href="http://llvm.org/docs/LangRef.html#terminator-instructions">terminator
 *      instructions</a>
 */
@Instrumentable(factory = LLVMTerminatorNodeWrapper.class)
public abstract class LLVMTerminatorNode extends LLVMStatementNode {

    private final int[] successors;

    /**
     * Constructor which creates a new terminator node and sets the source section and successors.
     *
     * @param sourceSection the source section of this node
     * @param successors the successors of this node
     */
    public LLVMTerminatorNode(SourceSection sourceSection, int... successors) {
        super(sourceSection);
        this.successors = successors;
    }

    /**
     * Copy constructor needed for {@link Instrumentable} nodes which have constructors with more than one parameter.
     *
     * @param nodeToCopy the node which should be copied
     */
    public LLVMTerminatorNode(LLVMTerminatorNode nodeToCopy) {
        this(nodeToCopy.getSourceSection(), nodeToCopy.getSuccessors());
    }

    public abstract int executeGetSuccessorIndex(VirtualFrame frame);

    @Override
    public void executeVoid(VirtualFrame frame) {
        executeGetSuccessorIndex(frame);
    }

    public int nrSuccessors() {
        return successors.length;
    }

    public int[] getSuccessors() {
        return successors;
    }

}
