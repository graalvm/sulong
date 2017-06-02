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
package com.oracle.truffle.llvm.nodes.op.arith.vector;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;

@NodeChildren({@NodeChild("leftNode"), @NodeChild("rightNode")})
public abstract class LLVMI8VectorArithmeticNode extends LLVMExpressionNode {

    public abstract static class LLVMI8VectorAddNode extends LLVMI8VectorArithmeticNode {
        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.add(right);
        }
    }

    public abstract static class LLVMI8VectorMulNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        public LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.mul(right);
        }

    }

    public abstract static class LLVMI8VectorSubNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.sub(right);
        }
    }

    public abstract static class LLVMI8VectorDivNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.div(right);
        }
    }

    public abstract static class LLVMI8VectorUDivNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.divUnsigned(right);
        }
    }

    public abstract static class LLVMI8VectorRemNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.rem(right);
        }
    }

    public abstract static class LLVMI8VectorURemNode extends LLVMI8VectorArithmeticNode {

        @Specialization
        protected LLVMI8Vector executeI8Vector(LLVMI8Vector left, LLVMI8Vector right) {
            return left.remUnsigned(right);
        }
    }
}
