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
package com.oracle.truffle.llvm.nodes.func;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.nodes.func.LLVMCallNodeFactory.ArgumentNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.NeedsStack;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.FunctionType;

@NeedsStack
public final class LLVMCallNode extends LLVMExpressionNode {

    public static final int USER_ARGUMENT_OFFSET = 1;

    @Child private LLVMExpressionNode functionNode;
    @Children private final LLVMExpressionNode[] argumentNodes;
    @Children private final ArgumentNode[] prepareArgumentNodes;
    @Child private LLVMLookupDispatchNode dispatchNode;

    private final SourceSection sourceSection;

    public LLVMCallNode(FunctionType functionType, LLVMExpressionNode functionNode, LLVMExpressionNode[] argumentNodes, SourceSection sourceSection) {
        this.functionNode = functionNode;
        this.argumentNodes = argumentNodes;
        this.dispatchNode = LLVMLookupDispatchNodeGen.create(functionType);
        this.prepareArgumentNodes = new ArgumentNode[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            this.prepareArgumentNodes[i] = ArgumentNodeGen.create(null);
        }
        this.sourceSection = sourceSection;
    }

    @ExplodeLoop
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object function = functionNode.executeGeneric(frame);
        Object[] argValues = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            argValues[i] = prepareArgumentNodes[i].executeWithTarget(argumentNodes[i].executeGeneric(frame));
        }
        return dispatchNode.executeDispatch(frame, function, argValues);
    }

    @NodeChild
    protected abstract static class ArgumentNode extends LLVMExpressionNode {

        protected abstract Object executeWithTarget(Object value);

        @Specialization
        LLVMAddress doAddress(LLVMAddress address) {
            return address.copy();
        }

        protected static boolean notAddress(Object value) {
            return !(value instanceof LLVMAddress);
        }

        @Specialization(guards = "notAddress(value)")
        Object doOther(Object value) {
            return value;
        }
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.StatementTag.class || tag == StandardTags.CallTag.class || super.isTaggedWith(tag);
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }
}
