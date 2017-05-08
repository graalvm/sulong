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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMBoxedPrimitive;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMFunction;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMFunctionHandle;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMSharedGlobalVariable;
import com.oracle.truffle.llvm.runtime.LLVMTruffleAddress;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.interop.LLVMDataEscapeNode;
import com.oracle.truffle.llvm.runtime.interop.LLVMDataEscapeNodeGen;
import com.oracle.truffle.llvm.runtime.interop.ToLLVMNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMHeap;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.memory.LLVMNativeFunctions;
import com.oracle.truffle.llvm.runtime.memory.LLVMNativeFunctions.MemCopyNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;

@NodeChildren(value = {@NodeChild(type = LLVMExpressionNode.class, value = "pointerNode")})
public abstract class LLVMStoreNode extends LLVMExpressionNode {

    @Child protected Node foreignWrite = Message.WRITE.createNode();
    @Child protected LLVMDataEscapeNode dataEscape;
    protected final Type valueType;

    private final SourceSection sourceSection;

    public LLVMStoreNode(Type valueType, SourceSection sourceSection) {
        this.valueType = valueType;
        this.dataEscape = LLVMDataEscapeNodeGen.create(valueType);
        this.sourceSection = sourceSection;
    }

    protected void doForeignAccess(LLVMTruffleObject addr, int stride, Object value, LLVMContext context) {
        try {
            ForeignAccess.sendWrite(foreignWrite, addr.getObject(), (int) (addr.getOffset() / stride), dataEscape.executeWithTarget(value, context));
        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void doForeignAccess(TruffleObject addr, Object value, LLVMContext context) {
        try {
            ForeignAccess.sendWrite(foreignWrite, addr, 0, dataEscape.executeWithTarget(value, context));
        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMI1StoreNode extends LLVMStoreNode {

        public LLVMI1StoreNode(SourceSection source) {
            super(PrimitiveType.I1, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, boolean value) {
            address.putI1(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, boolean value) {
            LLVMMemory.putI1(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, boolean value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, 1, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, boolean value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putI1(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, boolean value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I1), value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMI8StoreNode extends LLVMStoreNode {

        public LLVMI8StoreNode(SourceSection source) {
            super(PrimitiveType.I8, source);
        }

        @Specialization
        public Object execute(LLVMAddress address, byte value) {
            LLVMMemory.putI8(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, byte value) {
            address.putI8(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, byte value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, 1, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, byte value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putI8(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, byte value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I8), value, context);
            return null;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMI16StoreNode extends LLVMStoreNode {

        public LLVMI16StoreNode(SourceSection source) {
            super(PrimitiveType.I16, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, short value) {
            address.putI16(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, short value) {
            LLVMMemory.putI16(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, short value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.I16_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, short value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putI16(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, short value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I16), value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMI32StoreNode extends LLVMStoreNode {

        public LLVMI32StoreNode(SourceSection source) {
            super(PrimitiveType.I32, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, int value) {
            address.putI32(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, int value) {
            LLVMMemory.putI32(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, int value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.I32_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, int value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putI32(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, int value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I32), value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMI64StoreNode extends LLVMStoreNode {

        public LLVMI64StoreNode(SourceSection source) {
            super(PrimitiveType.I64, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, long value) {
            address.putI64(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, long value) {
            LLVMMemory.putI64(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, long value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.I64_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, long value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putI64(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, long value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.I64), value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMIVarBitStoreNode extends LLVMStoreNode {

        public LLVMIVarBitStoreNode(VariableBitWidthType type, SourceSection source) {
            super(type, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, LLVMIVarBit value) {
            LLVMMemory.putIVarBit(address.getNativeLocation(), value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, LLVMIVarBit value) {
            LLVMMemory.putIVarBit(address, value);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMFloatStoreNode extends LLVMStoreNode {

        public LLVMFloatStoreNode(SourceSection source) {
            super(PrimitiveType.FLOAT, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, float value) {
            address.putFloat(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, float value) {
            LLVMMemory.putFloat(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, float value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.FLOAT_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, float value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putFloat(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, float value, @Cached("getContext()") LLVMContext context) {
            execute(new LLVMTruffleObject(address, PrimitiveType.FLOAT), value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMDoubleStoreNode extends LLVMStoreNode {

        public LLVMDoubleStoreNode(SourceSection source) {
            super(PrimitiveType.DOUBLE, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, double value) {
            address.putDouble(value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, double value) {
            LLVMMemory.putDouble(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, double value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.DOUBLE_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, double value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putDouble(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, double value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVM80BitFloatStoreNode extends LLVMStoreNode {

        public LLVM80BitFloatStoreNode(SourceSection source) {
            super(PrimitiveType.X86_FP80, source);
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, LLVM80BitFloat value) {
            LLVMMemory.put80BitFloat(address.getNativeLocation(), value);
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, LLVM80BitFloat value) {
            LLVMMemory.put80BitFloat(address, value);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMAddressStoreNode extends LLVMStoreNode {

        public LLVMAddressStoreNode(Type type, SourceSection source) {
            super(type, source);
        }

        @Specialization
        public Object doAddress(LLVMAddress address, LLVMAddress value) {
            LLVMMemory.putAddress(address, value);
            return null;
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, LLVMAddress value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putAddress(LLVMAddress.fromLong((long) address.getValue()), value);
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization
        public Object execute(LLVMBoxedPrimitive address, LLVMGlobalVariable value) {
            if (address.getValue() instanceof Long) {
                LLVMMemory.putAddress(LLVMAddress.fromLong((long) address.getValue()), value.getNativeLocation());
                return null;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalAccessError("Cannot access address: " + address.getValue());
            }
        }

        @Specialization
        public Object doAddress(LLVMGlobalVariable address, LLVMAddress value) {
            address.putAddress(value);
            return null;
        }

        @Specialization
        public Object doAddress(LLVMAddress address, LLVMGlobalVariable value) {
            LLVMMemory.putAddress(address, value.getNativeLocation());
            return null;
        }

        @Specialization
        public Object doAddress(LLVMGlobalVariable address, LLVMGlobalVariable value) {
            LLVMMemory.putAddress(address.getNativeLocation(), value.getNativeLocation());
            return null;
        }

        @Specialization(guards = "notLLVM(value)")
        public Object doAddress(LLVMGlobalVariable address, TruffleObject value) {
            address.putTruffleObject(value);
            return null;
        }

        @Specialization
        public Object doAddress(LLVMGlobalVariable address, LLVMBoxedPrimitive value) {
            address.putBoxedPrimitive(value);
            return null;
        }

        @Specialization
        public Object doAddress(LLVMGlobalVariable address, LLVMTruffleObject value) {
            address.putLLVMTruffleObject(value);
            return null;
        }

        @Specialization(guards = "notLLVM(value)")
        public Object execute(LLVMAddress address, TruffleObject value, @Cached("getForceLLVMAddressNode()") LLVMForceLLVMAddressNode toLLVMAddress) {
            LLVMMemory.putAddress(address, toLLVMAddress.executeWithTarget(value));
            return null;
        }

        @Specialization(guards = "notLLVM(value)")
        public Object execute(LLVMBoxedPrimitive address, TruffleObject value, @Cached("getForceLLVMAddressNode()") LLVMForceLLVMAddressNode convertAddress,
                        @Cached("getForceLLVMAddressNode()") LLVMForceLLVMAddressNode convertValue) {
            LLVMMemory.putAddress(convertAddress.executeWithTarget(address), convertValue.executeWithTarget(value));
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, LLVMAddress value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.ADDRESS_SIZE_IN_BYTES, new LLVMTruffleAddress(value, valueType, context), context);
            return null;
        }

        @Specialization
        public Object execute(LLVMTruffleObject address, Object value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, LLVMExpressionNode.ADDRESS_SIZE_IN_BYTES, value, context);
            return null;
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, LLVMAddress value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, new LLVMTruffleAddress(value, valueType, context), context);
            return null;
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, LLVMGlobalVariable value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, new LLVMSharedGlobalVariable(value, context), context);
            return null;
        }

        @Specialization(guards = "notLLVM(address)")
        public Object execute(TruffleObject address, Object value, @Cached("getContext()") LLVMContext context) {
            doForeignAccess(address, value, context);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMGlobalVariableStoreNode extends LLVMExpressionNode {

        protected final LLVMGlobalVariable descriptor;

        private final SourceSection source;

        public LLVMGlobalVariableStoreNode(LLVMGlobalVariable descriptor, SourceSection source) {
            this.descriptor = descriptor;
            this.source = source;
        }

        @Specialization
        public Object executeNative(LLVMAddress value) {
            descriptor.putAddress(value);
            return null;
        }

        @Specialization
        public Object executeNative(LLVMTruffleAddress value) {
            descriptor.putAddress(value.getAddress());
            return null;
        }

        @Specialization
        public Object executeNative(LLVMFunctionHandle value) {
            descriptor.putFunction(value);
            return null;
        }

        @Specialization
        public Object executeNative(LLVMFunctionDescriptor value) {
            descriptor.putFunction(value);
            return null;
        }

        @Specialization
        public Object executeNative(LLVMGlobalVariable value) {
            descriptor.putGlobal(value);
            return null;
        }

        @Specialization
        public Object executeNative(LLVMTruffleObject value) {
            descriptor.putLLVMTruffleObject(value);
            return null;
        }

        @Child private ToLLVMNode toLLVM = ToLLVMNode.createNode(long.class);

        @Specialization
        public Object executeLLVMBoxedPrimitive(LLVMBoxedPrimitive value) {
            descriptor.putBoxedPrimitive(value);
            return null;
        }

        @Specialization(guards = "notLLVM(value)")
        public Object executeManaged(TruffleObject value) {
            descriptor.putTruffleObject(value);
            return null;
        }

        @Override
        public SourceSection getSourceSection() {
            return source;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    public abstract static class LLVMFunctionStoreNode extends LLVMStoreNode {

        public LLVMFunctionStoreNode(Type type, SourceSection source) {
            super(type, source);
        }

        @Specialization
        public Object execute(LLVMAddress address, LLVMFunction function) {
            LLVMHeap.putFunctionIndex(address, function.getFunctionIndex());
            return null;
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, LLVMFunction function) {
            address.putFunction(function);
            return null;
        }

    }

    @NodeChild(type = LLVMExpressionNode.class, value = "valueNode")
    @NodeField(type = int.class, name = "structSize")
    public abstract static class LLVMStructStoreNode extends LLVMStoreNode {

        @Child private MemCopyNode memCopy;

        public abstract int getStructSize();

        protected LLVMStructStoreNode(LLVMNativeFunctions heapFunctions, Type type, SourceSection source) {
            super(type, source);
            memCopy = heapFunctions.createMemCopyNode();
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, LLVMGlobalVariable value) {
            memCopy.execute(address.getNativeLocation(), value.getNativeLocation(), getStructSize());
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, LLVMGlobalVariable value) {
            memCopy.execute(address, value.getNativeLocation(), getStructSize());
            return null;
        }

        @Specialization
        public Object execute(LLVMGlobalVariable address, LLVMAddress value) {
            memCopy.execute(address.getNativeLocation(), value, getStructSize());
            return null;
        }

        @Specialization
        public Object execute(LLVMAddress address, LLVMAddress value) {
            memCopy.execute(address, value, getStructSize());
            return null;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMI1ArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMI1ArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI8(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI8(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                boolean currentValue = values[i].executeI1(frame);
                LLVMMemory.putI1(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMI8ArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMI8ArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI8(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI8(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                byte currentValue = values[i].executeI8(frame);
                LLVMMemory.putI8(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMI16ArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMI16ArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI8(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI8(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                short currentValue = values[i].executeI16(frame);
                LLVMMemory.putI16(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMI32ArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMI32ArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI32(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI32(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                int currentValue = values[i].executeI32(frame);
                LLVMMemory.putI32(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMI64ArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMI64ArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI64(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI64(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                long currentValue = values[i].executeI64(frame);
                LLVMMemory.putI64(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMFloatArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMFloatArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeI64(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeI64(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                float currentValue = values[i].executeFloat(frame);
                LLVMMemory.putFloat(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMDoubleArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMDoubleArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeDouble(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeDouble(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                double currentValue = values[i].executeDouble(frame);
                LLVMMemory.putDouble(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVM80BitFloatArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVM80BitFloatArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return write80BitFloat(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress write80BitFloat(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                try {
                    LLVM80BitFloat currentValue = values[i].executeLLVM80BitFloat(frame);
                    LLVMMemory.put80BitFloat(currentAddress, currentValue);
                    currentAddress = currentAddress.increment(stride);
                } catch (UnexpectedResultException e) {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(e);
                }
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMAddressArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        @Children private final LLVMForceLLVMAddressNode[] toLLVM;
        private final int stride;

        public LLVMAddressArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
            this.toLLVM = getForceLLVMAddressNodes(values.length);
        }

        public LLVMExpressionNode[] getValues() {
            return values;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeDouble(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeDouble(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                LLVMAddress currentValue = toLLVM[i].executeWithTarget(values[i].executeGeneric(frame));
                LLVMMemory.putAddress(currentAddress, currentValue);
                currentAddress = currentAddress.increment(stride);
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMFunctionArrayLiteralNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        public LLVMFunctionArrayLiteralNode(LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeDouble(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeDouble(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                try {
                    LLVMFunctionDescriptor currentValue = (LLVMFunctionDescriptor) values[i].executeTruffleObject(frame);
                    LLVMHeap.putFunctionIndex(currentAddress, currentValue.getFunctionIndex());
                    currentAddress = currentAddress.increment(stride);
                } catch (UnexpectedResultException e) {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(e);
                }
            }
            return addr;
        }

    }

    @NodeChild(value = "address", type = LLVMExpressionNode.class)
    public abstract static class LLVMAddressArrayCopyNode extends LLVMExpressionNode {

        @Children private final LLVMExpressionNode[] values;
        private final int stride;

        @Child private MemCopyNode memCopy;

        public LLVMAddressArrayCopyNode(LLVMNativeFunctions heapFunctions, LLVMExpressionNode[] values, int stride) {
            this.values = values;
            this.stride = stride;
            this.memCopy = heapFunctions.createMemCopyNode();
        }

        @Specialization
        protected LLVMAddress write(VirtualFrame frame, LLVMGlobalVariable global) {
            return writeDouble(frame, global.getNativeLocation());
        }

        @Specialization
        @ExplodeLoop
        protected LLVMAddress writeDouble(VirtualFrame frame, LLVMAddress addr) {
            LLVMAddress currentAddress = addr;
            for (int i = 0; i < values.length; i++) {
                try {
                    LLVMAddress currentValue = values[i].executeLLVMAddress(frame);
                    memCopy.execute(currentAddress, currentValue, stride);
                    currentAddress = currentAddress.increment(stride);
                } catch (UnexpectedResultException e) {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(e);
                }
            }
            return addr;
        }

    }

}
