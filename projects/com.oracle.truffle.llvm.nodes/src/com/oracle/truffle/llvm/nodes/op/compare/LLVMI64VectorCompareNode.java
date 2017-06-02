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
package com.oracle.truffle.llvm.nodes.op.compare;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI64Vector;

@NodeChildren({
                @NodeChild(value = "leftNode", type = LLVMExpressionNode.class),
                @NodeChild(value = "rightNode", type = LLVMExpressionNode.class)})
public abstract class LLVMI64VectorCompareNode extends LLVMExpressionNode {

    protected LLVMI1Vector doCompare(LLVMI64Vector left, LLVMI64Vector right) {
        int length = left.getLength();
        boolean[] values = new boolean[length];

        for (int i = 0; i < length; i++) {
            values[i] = comparison(left.getValue(i), right.getValue(i));
        }

        return LLVMI1Vector.create(values);
    }

    protected abstract boolean comparison(long lhs, long rhs);

    public abstract static class LLVMI64VectorEqNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs == rhs;
        }

    }

    public abstract static class LLVMI64VectorNeNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs != rhs;
        }

    }

    public abstract static class LLVMI64VectorSltNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs < rhs;
        }

    }

    public abstract static class LLVMI64VectorSleNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs <= rhs;
        }

    }

    public abstract static class LLVMI64VectorSgtNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs > rhs;
        }

    }

    public abstract static class LLVMI64VectorSgeNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return lhs >= rhs;
        }

    }

    public abstract static class LLVMI64VectorUgtNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return Long.compareUnsigned(lhs, rhs) > 0;
        }

    }

    public abstract static class LLVMI64VectorUgeNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return Long.compareUnsigned(lhs, rhs) >= 0;
        }

    }

    public abstract static class LLVMI64VectorUltNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return Long.compareUnsigned(lhs, rhs) < 0;
        }

    }

    public abstract static class LLVMI64VectorUleNode extends LLVMI64VectorCompareNode {

        @Specialization
        public LLVMI1Vector executeI1Vector(LLVMI64Vector left, LLVMI64Vector right) {
            return doCompare(left, right);
        }

        @Override
        protected boolean comparison(long lhs, long rhs) {
            return Long.compareUnsigned(lhs, rhs) <= 0;
        }

    }

}
