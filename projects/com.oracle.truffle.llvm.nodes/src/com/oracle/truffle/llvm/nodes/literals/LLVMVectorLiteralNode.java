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
package com.oracle.truffle.llvm.nodes.literals;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.types.LLVMAddress;
import com.oracle.truffle.llvm.types.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.types.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.types.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.types.vector.LLVMI8Vector;

public class LLVMVectorLiteralNode {

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorI1LiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorI1LiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMI1Vector executeI1Vector(VirtualFrame frame, LLVMAddress target) {
            boolean[] vals = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectI1(values[i], frame);
            }
            return LLVMI1Vector.fromI1Array(target, vals);
        }

    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorI8LiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorI8LiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMI8Vector executeI8Vector(VirtualFrame frame, LLVMAddress target) {
            byte[] vals = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectI8(values[i], frame);
            }
            return LLVMI8Vector.fromI8Array(target, vals);
        }

    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorI16LiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorI16LiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMI16Vector executeI16Vector(VirtualFrame frame, LLVMAddress target) {
            short[] vals = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectI16(values[i], frame);
            }
            return LLVMI16Vector.fromI16Array(target, vals);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorI32LiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorI32LiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMI32Vector executeI32Vector(VirtualFrame frame, LLVMAddress target) {
            int[] vals = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectI32(values[i], frame);
            }
            return LLVMI32Vector.fromI32Array(target, vals);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorI64LiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorI64LiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMI64Vector executeI64Vector(VirtualFrame frame, LLVMAddress target) {
            long[] vals = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectI64(values[i], frame);
            }
            return LLVMI64Vector.fromI64Array(target, vals);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorFloatLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorFloatLiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMFloatVector executeFloatVector(VirtualFrame frame, LLVMAddress target) {
            float[] vals = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectFloat(values[i], frame);
            }
            return LLVMFloatVector.fromFloatArray(target, vals);
        }

    }

    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMVectorDoubleLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;

        public LLVMVectorDoubleLiteralNode(LLVMExpressionNode[] values) {
            this.values = values;
        }

        @ExplodeLoop
        @Specialization
        public LLVMDoubleVector executeDoubleVector(VirtualFrame frame, LLVMAddress target) {
            double[] vals = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                vals[i] = LLVMExpressionNode.expectDouble(values[i], frame);
            }
            return LLVMDoubleVector.fromDoubleArray(target, vals);
        }

    }

}
