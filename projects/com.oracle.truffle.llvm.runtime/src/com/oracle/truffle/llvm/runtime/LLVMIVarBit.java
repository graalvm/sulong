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
package com.oracle.truffle.llvm.runtime;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;

// see https://bugs.chromium.org/p/nativeclient/issues/detail?id=3360 for use cases where variable ints arise
@ValueType
public final class LLVMIVarBit {

    private final int bits;

    private final byte[] arr; // represents value as big-endian two's-complement

    private LLVMIVarBit() {
        this.bits = 0;
        this.arr = null;
    }

    private LLVMIVarBit(int bits, byte[] arr, int arrBits, boolean signExtend) {
        this.bits = bits;

        this.arr = new byte[getByteSize()];
        if (getByteSize() >= arr.length) {
            System.arraycopy(arr, 0, this.arr, getByteSize() - arr.length, arr.length);
        } else {
            System.arraycopy(arr, arr.length - getByteSize(), this.arr, 0, this.arr.length);
        }

        int mostSignificantByte = arr.length - (arrBits / Byte.SIZE) - (arrBits % Byte.SIZE != 0 ? 1 : 0);
        if (mostSignificantByte >= 0) {
            boolean shouldAddLeadingOnes = signExtend && ((arr[mostSignificantByte] & (1 << ((arrBits - 1) %
                            Byte.SIZE))) != 0);
            int thisArrMostSignificantByte = Math.max(0, this.arr.length - arr.length + mostSignificantByte);
            if (shouldAddLeadingOnes) {
                // set MSB bit's outside of given bitwidth
                if (getByteSize() >= arr.length) {
                    for (int i = 0; i < thisArrMostSignificantByte; i++) {
                        this.arr[i] = (byte) 0xFF;
                    }
                }
                if (arrBits % Byte.SIZE != 0) {
                    this.arr[thisArrMostSignificantByte] |= 0xFF << (arrBits % Byte.SIZE);
                }
            } else {
                // clear MSB bit's outside of given bitwidth
                if (getByteSize() >= arr.length) {
                    for (int i = 0; i < thisArrMostSignificantByte; i++) {
                        this.arr[i] = (byte) 0x00;
                    }
                }
                if (arrBits % Byte.SIZE != 0) {
                    this.arr[thisArrMostSignificantByte] &= 0xFF >>> (8 - (arrBits % Byte.SIZE));
                }
            }
        }

        assert this.arr.length == getByteSize();
    }

    public static LLVMIVarBit create(int bitWidth, byte[] loadedBytes, int loadedArrBits, boolean signExtend) {
        return new LLVMIVarBit(bitWidth, loadedBytes, loadedArrBits, signExtend);
    }

    public static LLVMIVarBit createNull() {
        return new LLVMIVarBit();
    }

    public static LLVMIVarBit createZeroExt(int bits, byte from) {
        return create(bits, ByteBuffer.allocate(Byte.BYTES).put(from).array(), Byte.SIZE, false);
    }

    public static LLVMIVarBit createZeroExt(int bits, short from) {
        return create(bits, ByteBuffer.allocate(Short.BYTES).putShort(from).array(), Short.SIZE, false);
    }

    public static LLVMIVarBit createZeroExt(int bits, int from) {
        return create(bits, ByteBuffer.allocate(Integer.BYTES).putInt(from).array(), Integer.SIZE, false);
    }

    public static LLVMIVarBit createZeroExt(int bits, long from) {
        return create(bits, ByteBuffer.allocate(Long.BYTES).putLong(from).array(), Long.SIZE, false);
    }

    public static LLVMIVarBit fromBigInteger(int bits, BigInteger from) {
        return asIVar(bits, from);
    }

    public static LLVMIVarBit fromByte(int bits, byte from) {
        return create(bits, ByteBuffer.allocate(Byte.BYTES).put(from).array(), Byte.SIZE, true);
    }

    public static LLVMIVarBit fromShort(int bits, short from) {
        return create(bits, ByteBuffer.allocate(Short.BYTES).putShort(from).array(), Short.SIZE, true);
    }

    public static LLVMIVarBit fromInt(int bits, int from) {
        return create(bits, ByteBuffer.allocate(Integer.BYTES).putInt(from).array(), Integer.SIZE, true);
    }

    public static LLVMIVarBit fromLong(int bits, long from) {
        return create(bits, ByteBuffer.allocate(Long.BYTES).putLong(from).array(), Long.SIZE, true);
    }

    private int getByteSize() {
        int nrFullBytes = bits / Byte.SIZE;
        if (bits % Byte.SIZE != 0) {
            return nrFullBytes + 1;
        } else {
            return nrFullBytes;
        }
    }

    @TruffleBoundary
    private static BigInteger asBigInteger(LLVMIVarBit right) {
        if (right.getBytes() == null) {
            return BigInteger.ZERO;
        }
        return new BigInteger(right.getBytes());
    }

    @TruffleBoundary
    public BigInteger asUnsignedBigInteger() {
        if (arr == null || arr.length == 0) {
            return BigInteger.ZERO;
        }
        byte[] newArr = new byte[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 1, arr.length);
        return new BigInteger(newArr);
    }

    @TruffleBoundary
    public BigInteger asBigInteger() {
        if (arr != null && arr.length != 0) {
            return new BigInteger(arr);
        } else {
            return BigInteger.ZERO;
        }
    }

    @TruffleBoundary
    private ByteBuffer getByteBuffer(int minSizeBytes, boolean signExtend) {
        int allocationSize = Math.max(minSizeBytes, getByteSize());
        ByteBuffer bb = ByteBuffer.allocate(allocationSize).order(ByteOrder.BIG_ENDIAN);
        boolean truncation = bits > minSizeBytes * Byte.SIZE;
        boolean shouldAddLeadingOnes = signExtend && mostSignificantBit();
        if (!truncation) {
            int bytesToFillUp = minSizeBytes - getByteSize();
            if (shouldAddLeadingOnes) {
                for (int i = 0; i < bytesToFillUp; i++) {
                    bb.put((byte) -1);
                }
            } else {
                for (int i = 0; i < bytesToFillUp; i++) {
                    bb.put((byte) 0);
                }
            }
        }
        if (bits % Byte.SIZE == 0) {
            bb.put(arr, 0, getByteSize());
        } else {
            BitSet bitSet = new BitSet(Byte.SIZE);
            int bitsToSet = bits % Byte.SIZE;
            for (int i = 0; i < bitsToSet; i++) {
                boolean isBitSet = ((arr[0] >> i) & 1) == 1;
                if (isBitSet) {
                    bitSet.set(i);
                }
            }

            if (shouldAddLeadingOnes) {
                for (int i = bitsToSet; i < Byte.SIZE; i++) {
                    bitSet.set(i);
                }
            }
            byte firstByteResult;
            if (bitSet.isEmpty()) {
                firstByteResult = 0;
            } else {
                firstByteResult = bitSet.toByteArray()[0];
            }
            // FIXME actually need to truncate or sign extend individual bits
            bb.put(firstByteResult);
            for (int i = 1; i < arr.length; i++) {
                bb.put(arr[i]);
            }
        }

        bb.position(Math.max(0, getByteSize() - minSizeBytes));
        return bb;
    }

    private boolean mostSignificantBit() {
        return getBit(bits - 1);
    }

    private boolean getBit(int pos) {
        int selectedBytePos = arr.length - 1 - (pos / Byte.SIZE);
        byte selectedByte = arr[selectedBytePos];
        int selectedBitPos = pos % Byte.SIZE;
        return ((selectedByte >> selectedBitPos) & 1) == 1;
    }

    @TruffleBoundary
    public byte getByteValue() {
        return getByteBuffer(Byte.BYTES, true).get();
    }

    @TruffleBoundary
    public byte getZeroExtendedByteValue() {
        return getByteBuffer(Byte.BYTES, false).get();
    }

    @TruffleBoundary
    public short getShortValue() {
        return getByteBuffer(Short.BYTES, true).getShort();
    }

    @TruffleBoundary
    public short getZeroExtendedShortValue() {
        return getByteBuffer(Short.BYTES, false).getShort();
    }

    @TruffleBoundary
    public int getIntValue() {
        return getByteBuffer(Integer.BYTES, true).getInt();
    }

    @TruffleBoundary
    public int getZeroExtendedIntValue() {
        return getByteBuffer(Integer.BYTES, false).getInt();
    }

    @TruffleBoundary
    public long getLongValue() {
        return getByteBuffer(Long.BYTES, true).getLong();
    }

    @TruffleBoundary
    public long getZeroExtendedLongValue() {
        return getByteBuffer(Long.BYTES, false).getLong();
    }

    public int getBitSize() {
        return bits;
    }

    public byte[] getBytes() {
        assert arr.length == getByteSize() : arr.length + " " + getByteSize();
        return arr;
    }

    @TruffleBoundary
    public byte[] getSignExtendedBytes() {
        return getByteBuffer(arr.length, true).array();
    }

    @TruffleBoundary
    public LLVMIVarBit add(LLVMIVarBit right) {
        return asIVar(asBigInteger().add(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit mul(LLVMIVarBit right) {
        return asIVar(asBigInteger().multiply(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit sub(LLVMIVarBit right) {
        return asIVar(asBigInteger().subtract(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit div(LLVMIVarBit right) {
        return asIVar(asBigInteger().divide(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit rem(LLVMIVarBit right) {
        return asIVar(asBigInteger().remainder(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit unsignedRem(LLVMIVarBit right) {
        return asIVar(asUnsignedBigInteger().remainder(asBigInteger(right)));
    }

    @TruffleBoundary
    public LLVMIVarBit unsignedDiv(LLVMIVarBit right) {
        return asIVar(asUnsignedBigInteger().divide(asBigInteger(right)));
    }

    public int compare(LLVMIVarBit other) {
        for (int i = 0; i < getByteSize(); i++) {
            int diff = arr[i] - other.getBytes()[i];
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private interface SimpleOp {
        byte op(byte a, byte b);
    }

    private LLVMIVarBit performOp(LLVMIVarBit right, SimpleOp op) {
        assert bits == right.bits;
        byte[] newArr = new byte[getByteSize()];
        byte[] other = right.getBytes();
        assert arr.length == other.length : Arrays.toString(arr) + " " + Arrays.toString(other);
        for (int i = 0; i < newArr.length; i++) {
            newArr[i] = op.op(arr[i], other[i]);
        }
        return new LLVMIVarBit(bits, newArr, bits, false);
    }

    @TruffleBoundary
    public LLVMIVarBit and(LLVMIVarBit right) {
        return performOp(right, (byte a, byte b) -> (byte) (a & b));
    }

    @TruffleBoundary
    public LLVMIVarBit or(LLVMIVarBit right) {
        return performOp(right, (byte a, byte b) -> (byte) (a | b));
    }

    @TruffleBoundary
    public LLVMIVarBit xor(LLVMIVarBit right) {
        return performOp(right, (byte a, byte b) -> (byte) (a ^ b));
    }

    @TruffleBoundary
    public LLVMIVarBit leftShift(LLVMIVarBit right) {
        BigInteger result = asBigInteger().shiftLeft(right.getIntValue());
        return asIVar(bits, result);
    }

    @TruffleBoundary
    private LLVMIVarBit asIVar(BigInteger result) {
        return asIVar(bits, result);
    }

    private static LLVMIVarBit asIVar(int bitSize, BigInteger result) {
        int destSize = Math.max(Byte.BYTES, bitSize / Byte.SIZE);
        byte[] newArr = new byte[destSize];
        byte[] bigIntArr = result.toByteArray();

        if (newArr.length > bigIntArr.length) {
            int diff = newArr.length - bigIntArr.length;
            for (int j = diff; j < newArr.length; j++) {
                newArr[j] = bigIntArr[j - diff];
            }
            for (int j = 0; j < diff; j++) {
                newArr[j] = bigIntArr[0] < 0 ? (byte) -1 : 0;
            }
        } else {
            int diff = bigIntArr.length - newArr.length;
            for (int j = 0; j < newArr.length; j++) {
                newArr[j] = bigIntArr[j + diff];
            }
        }
        int resultLengthIncludingSign = result.bitLength() + (result.signum() == -1 ? 1 : 0);
        return new LLVMIVarBit(bitSize, newArr, resultLengthIncludingSign, result.signum() == -1);
    }

    @TruffleBoundary
    public LLVMIVarBit logicalRightShift(LLVMIVarBit right) {
        int shiftAmount = right.getIntValue();
        BigInteger mask = BigInteger.valueOf(-1).shiftLeft(bits - shiftAmount).not();
        BigInteger result = new BigInteger(arr).shiftRight(shiftAmount).and(mask);
        return asIVar(result);
    }

    @TruffleBoundary
    public LLVMIVarBit arithmeticRightShift(LLVMIVarBit right) {
        BigInteger result = asBigInteger().shiftRight(right.getIntValue());
        return asIVar(result);
    }

    @TruffleBoundary
    public int signedCompare(LLVMIVarBit other) {
        return asBigInteger().compareTo(other.asBigInteger());
    }

    @TruffleBoundary
    public int unsignedCompare(LLVMIVarBit other) {
        return asUnsignedBigInteger().compareTo(other.asUnsignedBigInteger());
    }

    @TruffleBoundary
    public boolean isZero() {
        return arr == null || arr.length == 0 || BigInteger.ZERO.equals(asBigInteger());
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return String.format("i%d %s", getBitSize(), asBigInteger().toString());
    }
}
