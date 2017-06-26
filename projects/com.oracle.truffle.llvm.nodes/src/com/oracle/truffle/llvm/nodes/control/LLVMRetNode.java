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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.nodes.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.func.LLVMArgNode;
import com.oracle.truffle.llvm.nodes.func.LLVMArgNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMFunctionHandle;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariableAccess;
import com.oracle.truffle.llvm.runtime.memory.LLVMProfiledMemMove;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;

public abstract class LLVMRetNode extends LLVMControlFlowNode {

    public LLVMRetNode(SourceSection sourceSection) {
        super(sourceSection);
    }

    public LLVMRetNode(LLVMRetNode wrappedNode) {
        super(wrappedNode.getSourceSection());
    }

    @Override
    public int getSuccessorCount() {
        return 1;
    }

    public int getSuccessor() {
        return LLVMBasicBlockNode.RETURN_FROM_FUNCTION;
    }

    @Override
    public LLVMExpressionNode getPhiNode(int successorIndex) {
        assert successorIndex == 0;
        return null;
    }

    public abstract Object execute(VirtualFrame frame);

    @Instrumentable(factory = LLVMI1RetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMI1RetNode extends LLVMRetNode {

        public LLVMI1RetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(boolean retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMI8RetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMI8RetNode extends LLVMRetNode {

        public LLVMI8RetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(byte retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMI16RetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMI16RetNode extends LLVMRetNode {

        public LLVMI16RetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(short retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMI32RetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMI32RetNode extends LLVMRetNode {

        public LLVMI32RetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(int retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMI64RetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMI64RetNode extends LLVMRetNode {

        public LLVMI64RetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(long retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMIVarBitRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMIVarBitRetNode extends LLVMRetNode {

        public LLVMIVarBitRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(LLVMIVarBit retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMFloatRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMFloatRetNode extends LLVMRetNode {

        public LLVMFloatRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(float retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMDoubleRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMDoubleRetNode extends LLVMRetNode {

        public LLVMDoubleRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(double retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVM80BitFloatRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVM80BitFloatRetNode extends LLVMRetNode {

        public LLVM80BitFloatRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(LLVM80BitFloat retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMAddressRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMAddressRetNode extends LLVMRetNode {

        public LLVMAddressRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(Object retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMFunctionRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMFunctionRetNode extends LLVMRetNode {

        public LLVMFunctionRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(LLVMFunctionHandle retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(TruffleObject retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMVectorRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorRetNode extends LLVMRetNode {

        public LLVMVectorRetNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute(LLVMDoubleVector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMFloatVector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMI16Vector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMI1Vector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMI32Vector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMI64Vector retResult) {
            return retResult;
        }

        @Specialization
        public Object execute(LLVMI8Vector retResult) {
            return retResult;
        }

    }

    @Instrumentable(factory = LLVMStructRetNodeWrapper.class)
    @NodeChild(value = "retResult", type = LLVMExpressionNode.class)
    @NodeField(name = "structSize", type = int.class)
    public abstract static class LLVMStructRetNode extends LLVMRetNode {

        @Child private LLVMArgNode argIdx1 = LLVMArgNodeGen.create(1);
        private final LLVMProfiledMemMove profiledMemMove;

        public abstract int getStructSize();

        public LLVMStructRetNode(SourceSection sourceSection) {
            super(sourceSection);
            this.profiledMemMove = new LLVMProfiledMemMove();
        }

        @Specialization
        public Object execute(VirtualFrame frame, LLVMAddress retResult) {
            return returnStruct(frame, retResult);
        }

        private Object returnStruct(VirtualFrame frame, LLVMAddress retResult) {
            try {
                LLVMAddress retStructAddress = argIdx1.executeLLVMAddress(frame);
                profiledMemMove.memmove(retStructAddress, retResult, getStructSize());
                return retStructAddress;
            } catch (UnexpectedResultException e) {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(e);
            }
        }

        @Specialization
        public Object execute(VirtualFrame frame, LLVMGlobalVariable retResult, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
            return returnStruct(frame, globalAccess.getNativeLocation(retResult));
        }

    }

    @Instrumentable(factory = LLVMVoidReturnNodeWrapper.class)
    public abstract static class LLVMVoidReturnNode extends LLVMRetNode {

        public LLVMVoidReturnNode(SourceSection sourceSection) {
            super(sourceSection);
        }

        @Specialization
        public Object execute() {
            return null;
        }
    }

}
