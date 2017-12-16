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
package com.oracle.truffle.llvm.runtime.memory;

import java.lang.reflect.Field;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.vector.LLVMAddressVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;

import sun.misc.Unsafe;

@SuppressWarnings("static-method")
public final class LLVMMemory {

    private static final Unsafe unsafe = getUnsafe();

    private static Unsafe getUnsafe() {
        CompilerAsserts.neverPartOfCompilation();
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    private static final LLVMMemory INSTANCE = new LLVMMemory();

    public static LLVMMemory getInstance() {
        return INSTANCE;
    }

    private LLVMMemory() {
    }

    /** Use {@link com.oracle.truffle.llvm.runtime.memory.LLVMMemSetNode} instead. */
    @Deprecated
    public void memset(LLVMAddress address, long size, byte value) {
        try {
            unsafe.setMemory(address.getVal(), size, value);
        } catch (Throwable e) {
            // this avoids unnecessary exception edges in the compiled code
            CompilerDirectives.transferToInterpreter();
            throw e;
        }
    }

    /** Use {@link com.oracle.truffle.llvm.runtime.memory.LLVMMemMoveNode} instead. */
    @Deprecated
    public void copyMemory(long sourceAddress, long targetAddress, long length) {
        unsafe.copyMemory(sourceAddress, targetAddress, length);
    }

    public void free(LLVMAddress address) {
        free(address.getVal());
    }

    public void free(long address) {
        try {
            unsafe.freeMemory(address);
        } catch (Throwable e) {
            // this avoids unnecessary exception edges in the compiled code
            CompilerDirectives.transferToInterpreter();
            throw e;
        }
    }

    public LLVMAddress allocateMemory(long size) {
        try {
            return LLVMAddress.fromLong(unsafe.allocateMemory(size));
        } catch (Throwable e) {
            // this avoids unnecessary exception edges in the compiled code
            CompilerDirectives.transferToInterpreter();
            throw e;
        }
    }

    public LLVMAddress reallocateMemory(LLVMAddress addr, long size) {
        // a null pointer is a valid argument
        try {
            return LLVMAddress.fromLong(unsafe.reallocateMemory(addr.getVal(), size));
        } catch (Throwable e) {
            // this avoids unnecessary exception edges in the compiled code
            CompilerDirectives.transferToInterpreter();
            throw e;
        }
    }

    public boolean getI1(LLVMAddress addr) {
        return getI1(addr.getVal());
    }

    public boolean getI1(long ptr) {
        assert ptr != 0;
        return unsafe.getByte(ptr) != 0;
    }

    public byte getI8(LLVMAddress addr) {
        return getI8(addr.getVal());
    }

    public byte getI8(long ptr) {
        assert ptr != 0;
        return unsafe.getByte(ptr);
    }

    public short getI16(LLVMAddress addr) {
        return getI16(addr.getVal());
    }

    public short getI16(long ptr) {
        assert ptr != 0;
        return unsafe.getShort(ptr);
    }

    public int getI32(LLVMAddress addr) {
        return getI32(addr.getVal());
    }

    public int getI32(long ptr) {
        assert ptr != 0;
        return unsafe.getInt(ptr);
    }

    public LLVMIVarBit getIVarBit(LLVMAddress addr, int bitWidth) {
        if (bitWidth % Byte.SIZE != 0) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError();
        }
        int bytes = bitWidth / Byte.SIZE;
        byte[] loadedBytes = new byte[bytes];
        long currentAddressPtr = addr.getVal();
        for (int i = loadedBytes.length - 1; i >= 0; i--) {
            loadedBytes[i] = getI8(currentAddressPtr);
            currentAddressPtr += Byte.BYTES;
        }
        return LLVMIVarBit.create(bitWidth, loadedBytes, bitWidth, false);
    }

    public long getI64(LLVMAddress addr) {
        return getI64(addr.getVal());
    }

    public long getI64(long ptr) {
        assert ptr != 0;
        return unsafe.getLong(ptr);
    }

    public float getFloat(LLVMAddress addr) {
        return getFloat(addr.getVal());
    }

    public float getFloat(long ptr) {
        assert ptr != 0;
        return unsafe.getFloat(ptr);
    }

    public double getDouble(LLVMAddress addr) {
        return getDouble(addr.getVal());
    }

    public double getDouble(long ptr) {
        assert ptr != 0;
        return unsafe.getDouble(ptr);
    }

    public LLVM80BitFloat get80BitFloat(LLVMAddress addr) {
        byte[] bytes = new byte[LLVM80BitFloat.BYTE_WIDTH];
        long currentPtr = addr.getVal();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = getI8(currentPtr);
            currentPtr += Byte.BYTES;
        }
        return LLVM80BitFloat.fromBytes(bytes);
    }

    public LLVMAddress getAddress(LLVMAddress addr) {
        return getAddress(addr.getVal());
    }

    public LLVMAddress getAddress(long ptr) {
        assert ptr != 0;
        return LLVMAddress.fromLong(unsafe.getAddress(ptr));
    }

    public void putI1(LLVMAddress addr, boolean value) {
        putI1(addr.getVal(), value);
    }

    public void putI1(long ptr, boolean value) {
        assert ptr != 0;
        unsafe.putByte(ptr, (byte) (value ? 1 : 0));
    }

    public void putI8(LLVMAddress addr, byte value) {
        putI8(addr.getVal(), value);
    }

    public void putI8(long ptr, byte value) {
        assert ptr != 0;
        unsafe.putByte(ptr, value);
    }

    public void putI16(LLVMAddress addr, short value) {
        putI16(addr.getVal(), value);
    }

    public void putI16(long ptr, short value) {
        assert ptr != 0;
        unsafe.putShort(ptr, value);
    }

    public void putI32(LLVMAddress addr, int value) {
        putI32(addr.getVal(), value);
    }

    public void putI32(long ptr, int value) {
        assert ptr != 0;
        unsafe.putInt(ptr, value);
    }

    public void putI64(LLVMAddress addr, long value) {
        putI64(addr.getVal(), value);
    }

    public void putI64(long ptr, long value) {
        assert ptr != 0;
        unsafe.putLong(ptr, value);
    }

    public void putIVarBit(LLVMAddress addr, LLVMIVarBit value) {
        byte[] bytes = value.getBytes();
        long currentptr = addr.getVal();
        for (int i = bytes.length - 1; i >= 0; i--) {
            putI8(currentptr, bytes[i]);
            currentptr += Byte.BYTES;
        }
    }

    private void putByteArray(LLVMAddress addr, byte[] bytes) {
        putByteArray(addr.getVal(), bytes);
    }

    private void putByteArray(long ptr, byte[] bytes) {
        long currentptr = ptr;
        for (int i = 0; i < bytes.length; i++) {
            putI8(currentptr, bytes[i]);
            currentptr += Byte.BYTES;
        }
    }

    public void putFloat(LLVMAddress addr, float value) {
        putFloat(addr.getVal(), value);
    }

    public void putFloat(long ptr, float value) {
        assert ptr != 0;
        unsafe.putFloat(ptr, value);
    }

    public void putDouble(LLVMAddress addr, double value) {
        putDouble(addr.getVal(), value);
    }

    public void putDouble(long ptr, double value) {
        assert ptr != 0;
        unsafe.putDouble(ptr, value);
    }

    public void put80BitFloat(LLVMAddress addr, LLVM80BitFloat value) {
        putByteArray(addr, value.getBytes());
    }

    public void put80BitFloat(long ptr, LLVM80BitFloat value) {
        putByteArray(ptr, value.getBytes());
    }

    public void putAddress(LLVMAddress addr, LLVMAddress value) {
        putAddress(addr.getVal(), value);
    }

    public void putAddress(LLVMAddress addr, long ptrValue) {
        putAddress(addr.getVal(), ptrValue);
    }

    public void putAddress(long ptr, LLVMAddress value) {
        putAddress(ptr, value.getVal());
    }

    public void putAddress(long ptr, long ptrValue) {
        assert ptr != 0;
        unsafe.putAddress(ptr, ptrValue);
    }

    private static final int I32_SIZE = 4;
    private static final int I8_SIZE = 1;
    private static final int I1_SIZE = 1;
    private static final int I16_SIZE = 2;
    private static final int I64_SIZE = 8;
    private static final int FLOAT_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;
    private static final int ADDRESS_LENGTH = 8;

    public LLVMI32Vector getI32Vector(LLVMAddress address, int size) {
        int[] vector = new int[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getI32(currentPtr);
            currentPtr += I32_SIZE;
        }
        return LLVMI32Vector.create(vector);
    }

    public LLVMI8Vector getI8Vector(LLVMAddress address, int size) {
        byte[] vector = new byte[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getI8(currentPtr);
            currentPtr += I8_SIZE;
        }
        return LLVMI8Vector.create(vector);
    }

    public LLVMI1Vector getI1Vector(LLVMAddress address, int size) {
        boolean[] vector = new boolean[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getI1(currentPtr);
            currentPtr += I1_SIZE;
        }
        return LLVMI1Vector.create(vector);
    }

    public LLVMI16Vector getI16Vector(LLVMAddress address, int size) {
        short[] vector = new short[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getI16(currentPtr);
            currentPtr += I16_SIZE;
        }
        return LLVMI16Vector.create(vector);
    }

    public LLVMI64Vector getI64Vector(LLVMAddress address, int size) {
        long[] vector = new long[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getI64(currentPtr);
            currentPtr += I64_SIZE;
        }
        return LLVMI64Vector.create(vector);
    }

    public LLVMFloatVector getFloatVector(LLVMAddress address, int size) {
        float[] vector = new float[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getFloat(currentPtr);
            currentPtr += FLOAT_SIZE;
        }
        return LLVMFloatVector.create(vector);
    }

    public LLVMDoubleVector getDoubleVector(LLVMAddress address, int size) {
        double[] vector = new double[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getDouble(currentPtr);
            currentPtr += DOUBLE_SIZE;
        }
        return LLVMDoubleVector.create(vector);
    }

    public LLVMAddressVector getAddressVector(LLVMAddress address, int size) {
        LLVMAddress[] vector = new LLVMAddress[size];
        long currentPtr = address.getVal();
        for (int i = 0; i < size; i++) {
            vector[i] = getAddress(currentPtr);
            currentPtr += ADDRESS_LENGTH;
        }
        return LLVMAddressVector.create(vector);
    }

    // watch out for casts such as I32* to I32Vector* when changing the way how vectors are
    // implemented
    public void putVector(LLVMAddress address, LLVMDoubleVector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putDouble(currentPtr, vector.getValue(i));
            currentPtr += DOUBLE_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMFloatVector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putFloat(currentPtr, vector.getValue(i));
            currentPtr += FLOAT_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMI16Vector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putI16(currentPtr, vector.getValue(i));
            currentPtr += I16_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMI1Vector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putI1(currentPtr, vector.getValue(i));
            currentPtr += I1_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMI32Vector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putI32(currentPtr, vector.getValue(i));
            currentPtr += I32_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMI64Vector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putI64(currentPtr, vector.getValue(i));
            currentPtr += I64_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMI8Vector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putI8(currentPtr, vector.getValue(i));
            currentPtr += I8_SIZE;
        }
    }

    public void putVector(LLVMAddress address, LLVMAddressVector vector) {
        long currentPtr = address.getVal();
        for (int i = 0; i < vector.getLength(); i++) {
            putAddress(currentPtr, vector.getValue(i));
            currentPtr += ADDRESS_LENGTH;
        }
    }

    private void allocateString(String string, LLVMAddress address) {
        long currentAddress = address.getVal();
        for (int i = 0; i < string.length(); i++) {
            byte c = (byte) string.charAt(i);
            putI8(currentAddress, c);
            currentAddress++;
        }
    }

    public LLVMAddress allocateCString(String string) {
        LLVMAddress baseAddress = allocateMemory(string.length() + 1);
        allocateString(string, baseAddress);
        putI8(baseAddress.getVal() + string.length(), (byte) 0);
        return baseAddress;
    }

    public LLVMAddress allocateRustString(String string) {
        LLVMAddress baseAddress = allocateMemory(string.length());
        allocateString(string, baseAddress);
        return baseAddress;
    }

    // current hack: we cannot directly store the LLVMFunction in the native memory due to GC
    public static final int FUNCTION_PTR_SIZE_BYTE = 8;

    public void putFunctionPointer(LLVMAddress address, long functionIndex) {
        putI64(address, functionIndex);
    }

    public void putFunctionPointer(long ptr, long functionIndex) {
        putI64(ptr, functionIndex);
    }

    public long getFunctionPointer(LLVMAddress addr) {
        return getI64(addr);
    }

    @ValueType
    public static final class CMPXCHGI32 {
        private final int value;
        private final boolean swap;

        private CMPXCHGI32(int value, boolean swap) {
            this.value = value;
            this.swap = swap;
        }

        public int getValue() {
            return value;
        }

        public boolean isSwap() {
            return swap;
        }
    }

    public CMPXCHGI32 compareAndSwapI32(LLVMAddress p, int comparisonValue, int newValue) {
        while (true) {
            boolean b = unsafe.compareAndSwapInt(null, p.getVal(), comparisonValue, newValue);
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, b)) {
                return new CMPXCHGI32(comparisonValue, b);
            } else {
                int t = unsafe.getIntVolatile(null, p.getVal());
                if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, t == comparisonValue)) {
                    continue;
                } else {
                    return new CMPXCHGI32(t, b);
                }
            }
        }
    }

    @ValueType
    public static final class CMPXCHGI64 {
        private final long value;
        private final boolean swap;

        private CMPXCHGI64(long value, boolean swap) {
            this.value = value;
            this.swap = swap;
        }

        public long getValue() {
            return value;
        }

        public boolean isSwap() {
            return swap;
        }
    }

    public CMPXCHGI64 compareAndSwapI64(LLVMAddress p, long comparisonValue, long newValue) {
        while (true) {
            boolean b = unsafe.compareAndSwapLong(null, p.getVal(), comparisonValue, newValue);
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, b)) {
                return new CMPXCHGI64(comparisonValue, b);
            } else {
                long t = unsafe.getLongVolatile(null, p.getVal());
                if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, t == comparisonValue)) {
                    continue;
                } else {
                    return new CMPXCHGI64(t, b);
                }
            }
        }
    }

    @ValueType
    public static final class CMPXCHGI8 {
        private final byte value;
        private final boolean swap;

        private CMPXCHGI8(byte value, boolean swap) {
            this.value = value;
            this.swap = swap;
        }

        public byte getValue() {
            return value;
        }

        public boolean isSwap() {
            return swap;
        }
    }

    private static long alignToI32(long address) {
        long mask = 3;
        return (address & ~mask);
    }

    private static int getI8Index(long address) {
        long mask = 3;
        return (int) (address & mask);
    }

    private static byte getI8At(int value, int index) {
        return (byte) ((value >> (8 * index)) & 0xff);
    }

    private static int replaceI8(int index, int value, byte replaceByte) {
        return (value & ~(0xFF << (index * 8))) | ((replaceByte & 0xFF) << (index * 8));
    }

    public CMPXCHGI8 compareAndSwapI8(LLVMAddress p, byte comparisonValue, byte newValue) {
        int byteIndex = getI8Index(p.getVal());
        long address = alignToI32(p.getVal());
        while (true) {
            int t = unsafe.getIntVolatile(null, address);
            byte b = getI8At(t, byteIndex);
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, b != comparisonValue)) {
                return new CMPXCHGI8(b, false);
            } else {
                int newVal = replaceI8(byteIndex, t, newValue);
                boolean c = unsafe.compareAndSwapInt(null, address, t, newVal);
                if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, c)) {
                    return new CMPXCHGI8(comparisonValue, true);
                } else {
                    continue;
                }
            }
        }
    }

    @ValueType
    public static final class CMPXCHGI16 {
        private final short value;
        private final boolean swap;

        public CMPXCHGI16(short value, boolean swap) {
            this.value = value;
            this.swap = swap;
        }

        public short getValue() {
            return value;
        }

        public boolean isSwap() {
            return swap;
        }
    }

    private static int getI16Index(long address) {
        long mask = 3;
        return (int) (address & mask) >> 1;
    }

    private static short getI16At(int value, int index) {
        return (short) ((value >> (16 * index)) & 0xFFFF);
    }

    private static int replaceI16(int index, int value, short replace) {
        return (value & ~(0xFFFF << (index * 16))) | ((replace & 0xFFFF) << (index * 16));
    }

    public CMPXCHGI16 compareAndSwapI16(LLVMAddress p, short comparisonValue, short newValue) {
        int idx = getI16Index(p.getVal());
        long address = alignToI32(p.getVal());
        while (true) {
            int t = unsafe.getIntVolatile(null, address);
            short b = getI16At(t, idx);
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, b != comparisonValue)) {
                return new CMPXCHGI16(b, false);
            } else {
                int newVal = replaceI16(idx, t, newValue);
                boolean c = unsafe.compareAndSwapInt(null, address, t, newVal);
                if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, c)) {
                    return new CMPXCHGI16(comparisonValue, true);
                } else {
                    continue;
                }
            }
        }
    }

    public long getAndSetI64(LLVMAddress address, long value) {
        return unsafe.getAndSetLong(null, address.getVal(), value);
    }

    public long getAndAddI64(LLVMAddress address, long value) {
        return unsafe.getAndAddLong(null, address.getVal(), value);
    }

    public long getAndSubI64(LLVMAddress address, long value) {
        return unsafe.getAndAddLong(null, address.getVal(), -value);
    }

    public long getAndOpI64(LLVMAddress address, long value, LongBinaryOperator f) {
        long addr = address.getVal();
        long old;
        long nevv;
        do {
            old = getI64(address);
            nevv = f.applyAsLong(old, value);
        } while (!unsafe.compareAndSwapLong(null, addr, old, nevv));
        return old;
    }

    public int getAndSetI32(LLVMAddress address, int value) {
        return unsafe.getAndSetInt(null, address.getVal(), value);
    }

    public int getAndAddI32(LLVMAddress address, int value) {
        return unsafe.getAndAddInt(null, address.getVal(), value);
    }

    public int getAndSubI32(LLVMAddress address, int value) {
        return unsafe.getAndAddInt(null, address.getVal(), -value);
    }

    public int getAndOpI32(LLVMAddress address, int value, IntBinaryOperator f) {
        long addr = address.getVal();
        int old;
        int nevv;
        do {
            old = getI32(address);
            nevv = f.applyAsInt(old, value);
        } while (!unsafe.compareAndSwapInt(null, addr, old, nevv));
        return old;
    }

    public short getAndOpI16(LLVMAddress address, short value, BinaryOperator<Short> f) {
        short old;
        short nevv;
        do {
            old = getI16(address);
            nevv = f.apply(old, value);
        } while (!compareAndSwapI16(address, old, nevv).swap);
        return old;
    }

    public byte getAndOpI8(LLVMAddress address, byte value, BinaryOperator<Byte> f) {
        byte old;
        byte nevv;
        do {
            old = getI8(address);
            nevv = f.apply(old, value);
        } while (!compareAndSwapI8(address, old, nevv).swap);
        return old;
    }

    public boolean getAndOpI1(LLVMAddress address, boolean value, BinaryOperator<Boolean> f) {
        byte old;
        boolean nevv;
        do {
            old = getI8(address);
            nevv = f.apply(old != 0, value);
        } while (!compareAndSwapI8(address, old, (byte) (nevv ? 1 : 0)).swap);
        return old != 0;
    }

    public void fullFence() {
        unsafe.fullFence();
    }
}
