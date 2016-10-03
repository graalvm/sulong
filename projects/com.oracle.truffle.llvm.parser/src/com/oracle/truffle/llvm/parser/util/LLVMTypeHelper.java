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
package com.oracle.truffle.llvm.parser.util;

import com.intel.llvm.ireditor.lLVM_IR.StructureConstant;
import com.intel.llvm.ireditor.lLVM_IR.TypedConstant;
import com.intel.llvm.ireditor.types.ResolvedType;
import com.intel.llvm.ireditor.types.TypeResolver;
import com.oracle.truffle.llvm.parser.LLVMBaseType;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.LLVMType;
import com.oracle.truffle.llvm.parser.adapters.LLVMToBitcodeAdapter;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException.UnsupportedReason;
import com.oracle.truffle.llvm.types.LLVMFunctionDescriptor.LLVMRuntimeType;

import uk.ac.man.cs.llvm.ir.types.ArrayType;
import uk.ac.man.cs.llvm.ir.types.FloatingPointType;
import uk.ac.man.cs.llvm.ir.types.FunctionType;
import uk.ac.man.cs.llvm.ir.types.IntegerType;
import uk.ac.man.cs.llvm.ir.types.MetaType;
import uk.ac.man.cs.llvm.ir.types.PointerType;
import uk.ac.man.cs.llvm.ir.types.StructureType;
import uk.ac.man.cs.llvm.ir.types.Type;
import uk.ac.man.cs.llvm.ir.types.VectorType;

public class LLVMTypeHelper {

    private final LLVMParserRuntime runtime;

    public LLVMTypeHelper(LLVMParserRuntime runtime) {
        this.runtime = runtime;
    }

    @Deprecated
    public int getByteSize(ResolvedType resolvedType) {
        return getByteSize(LLVMToBitcodeAdapter.resolveType(resolvedType));
    }

    public int getByteSize(Type type) {
        return type.sizeof();
    }

    public int getStructureSizeByte(StructureConstant structure, TypeResolver typeResolver) {
        int sumByte = 0;
        int largestAlignment = 0;
        for (TypedConstant constant : structure.getList().getTypedConstants()) {
            Type type = LLVMToBitcodeAdapter.resolveType(typeResolver.resolve(constant.getType()));
            int alignmentByte = getAlignmentByte(type);
            sumByte += computePaddingByte(sumByte, type);
            sumByte += getByteSize(type);
            largestAlignment = Math.max(alignmentByte, largestAlignment);
        }
        int padding;
        if (structure.getPacked() != null || sumByte == 0) {
            padding = 0;
        } else {
            padding = computePadding(sumByte, largestAlignment);
        }
        int totalSizeByte = sumByte + padding;
        return totalSizeByte;
    }

    private int getStructSizeByte(StructureType type) {
        int sumByte = 0;
        for (int i = 0; i < type.getElementCount(); i++) {
            if (!type.isPacked()) {
                sumByte += computePaddingByte(sumByte, type.getElementType(i));
            }
            sumByte += getByteSize(type.getElementType(i));
        }
        int padding;
        if (type.isPacked() || sumByte == 0) {
            padding = 0;
        } else {
            padding = computePadding(sumByte, largestAlignmentByte(type));
        }
        int totalSizeByte = sumByte + padding;
        return totalSizeByte;
    }

    private static int computePadding(int offset, int alignment) {
        if (alignment == 0) {
            throw new AssertionError();
        }
        int padding = (alignment - (offset % alignment)) % alignment;
        return padding;
    }

    private int largestAlignmentByte(StructureType structType) {
        int largestAlignment = 0;
        for (int i = 0; i < structType.getElementCount(); i++) {
            int alignment = getAlignmentByte(structType.getElementType(i));
            largestAlignment = Math.max(largestAlignment, alignment);
        }
        return largestAlignment;
    }

    @Deprecated
    public static LLVMType getLLVMType(ResolvedType resolvedType) {
        return getLLVMType(LLVMToBitcodeAdapter.resolveType(resolvedType));
    }

    // Checkstyle: stop magic number name check
    public static LLVMType getLLVMType(Type targetType2) {
        if (targetType2 instanceof IntegerType) {
            switch (((IntegerType) targetType2).getBitCount()) {
                case 1:
                    return new LLVMType(LLVMBaseType.I1);
                case 8:
                    return new LLVMType(LLVMBaseType.I8);
                case 16:
                    return new LLVMType(LLVMBaseType.I16);
                case 32:
                    return new LLVMType(LLVMBaseType.I32);
                case 64:
                    return new LLVMType(LLVMBaseType.I64);
                default:
                    return new LLVMType(LLVMBaseType.I_VAR_BITWIDTH);
            }
        } else if (targetType2 instanceof FloatingPointType) {
            switch (((FloatingPointType) targetType2).width()) {
                case 32:
                    return new LLVMType(LLVMBaseType.FLOAT);
                case 64:
                    return new LLVMType(LLVMBaseType.DOUBLE);
                case 80:
                    return new LLVMType(LLVMBaseType.X86_FP80);
                default:
                    throw new LLVMUnsupportedException(UnsupportedReason.FLOAT_OTHER_TYPE_NOT_IMPLEMENTED);
            }
        } else if (targetType2 instanceof PointerType) {
            if (((PointerType) targetType2).getPointeeType() instanceof FunctionType) {
                return new LLVMType(LLVMBaseType.FUNCTION_ADDRESS);
            } else {
                try {
                    return new LLVMType(LLVMBaseType.ADDRESS, getLLVMType(((PointerType) targetType2).getPointeeType()));
                } catch (LLVMUnsupportedException e) {
                    // generic pointer
                    return new LLVMType(LLVMBaseType.ADDRESS, new LLVMType(LLVMBaseType.VOID));
                }
            }
        } else if (targetType2.equals(MetaType.VOID)) {
            return new LLVMType(LLVMBaseType.VOID);
        } else if (targetType2 instanceof ArrayType) {
            return new LLVMType(LLVMBaseType.ARRAY);
        } else if (targetType2 instanceof StructureType) {
            return new LLVMType(LLVMBaseType.STRUCT);
        } else if (targetType2 instanceof VectorType) {
            switch (getLLVMType(((VectorType) targetType2).getElementType()).getType()) {
                case I1:
                    return new LLVMType(LLVMBaseType.I1_VECTOR);
                case I8:
                    return new LLVMType(LLVMBaseType.I8_VECTOR);
                case I16:
                    return new LLVMType(LLVMBaseType.I16_VECTOR);
                case I32:
                    return new LLVMType(LLVMBaseType.I32_VECTOR);
                case I64:
                    return new LLVMType(LLVMBaseType.I64_VECTOR);
                case FLOAT:
                    return new LLVMType(LLVMBaseType.FLOAT_VECTOR);
                case DOUBLE:
                    return new LLVMType(LLVMBaseType.DOUBLE_VECTOR);
                case ADDRESS:
                    return new LLVMType(LLVMBaseType.ADDRESS_VECTOR, getLLVMType(((VectorType) targetType2).getElementType()).getPointee());
                default:
                    throw new AssertionError(targetType2);
            }
        }

        else {
            throw new LLVMUnsupportedException(UnsupportedReason.OTHER_TYPE_NOT_IMPLEMENTED);
        }
    }// Checkstyle: resume magic number name check

    @Deprecated
    public int goIntoTypeGetLengthByte(ResolvedType resolvedType, int index) {
        return goIntoTypeGetLengthByte(LLVMToBitcodeAdapter.resolveType(resolvedType), index);
    }

    public int goIntoTypeGetLengthByte(Type currentType, int index) {
        if (currentType == null) {
            return 0; // TODO: better throw an exception
        } else if (currentType instanceof PointerType) {
            return getByteSize(((PointerType) currentType).getPointeeType()) * index;
        } else if (currentType instanceof ArrayType) {
            return getByteSize(((ArrayType) currentType).getElementType()) * index;
        } else if (currentType instanceof VectorType) {
            return getByteSize(((VectorType) currentType).getElementType()) * index;
        } else if (currentType instanceof StructureType) {
            StructureType type = (StructureType) currentType;
            int sum = 0;
            for (int i = 0; i < index; i++) {
                Type containedType = type.getElementType(i);
                sum += getByteSize(containedType);
                if (!isPackedStructType(type)) {
                    sum += computePaddingByte(sum, containedType);
                }
            }
            if (!isPackedStructType(currentType) && getStructSizeByte(type) > sum) {
                sum += computePaddingByte(sum, type);
            }
            return sum;
        } else {
            throw new AssertionError(currentType);
        }
    }

    @Deprecated
    public static ResolvedType goIntoType(ResolvedType currentType, int index) {
        return currentType.getContainedType(index);
    }

    @Deprecated
    public static boolean isPackedStructType(ResolvedType currentType) {
        return isPackedStructType(LLVMToBitcodeAdapter.resolveType(currentType));
    }

    public static boolean isPackedStructType(Type currentType) {
        if (!(currentType instanceof StructureType)) {
            return false;
        }
        return ((StructureType) currentType).isPacked();
    }

    @Deprecated
    public int computePaddingByte(int currentOffset, ResolvedType type) {
        return computePaddingByte(currentOffset, LLVMToBitcodeAdapter.resolveType(type));
    }

    public int computePaddingByte(int currentOffset, Type type) {
        int alignmentByte = getAlignmentByte(type);
        if (alignmentByte == 0) {
            return 0;
        } else {
            return computePadding(currentOffset, alignmentByte);
        }
    }

    interface LayoutConverter {
        int getBitAlignment(LLVMBaseType type);
    }

    @Deprecated
    public int getAlignmentByte(ResolvedType resolvedType) {
        return getAlignmentByte(LLVMToBitcodeAdapter.resolveType(resolvedType));
    }

    public int getAlignmentByte(Type resolvedType) {
        if (resolvedType instanceof StructureType) {
            return largestAlignmentByte((StructureType) resolvedType);
        } else if (resolvedType instanceof ArrayType) {
            return getAlignmentByte(((ArrayType) resolvedType).getElementType());
        } else if (resolvedType instanceof VectorType) {
            return getAlignmentByte(((VectorType) resolvedType).getElementType());
        } else {
            LLVMBaseType type = getLLVMType(resolvedType).getType();
            return runtime.getBitAlignment(type) / Byte.SIZE;
        }
    }

    public static boolean isVectorType(LLVMBaseType llvmType) {
        switch (llvmType) {
            case I1_VECTOR:
            case I8_VECTOR:
            case I16_VECTOR:
            case I32_VECTOR:
            case I64_VECTOR:
            case FLOAT_VECTOR:
            case DOUBLE_VECTOR:
                return true;
            case ARRAY:
            case DOUBLE:
            case F128:
            case FLOAT:
            case FUNCTION_ADDRESS:
            case HALF:
            case I1:
            case I16:
            case I32:
            case I64:
            case I8:
            case ADDRESS:
            case PPC_FP128:
            case STRUCT:
            case VOID:
            case X86_FP80:
            case I_VAR_BITWIDTH:
                return false;
            default:
                throw new AssertionError(llvmType);
        }
    }

    public static LLVMRuntimeType[] convertTypes(LLVMType... llvmParamTypes) {
        LLVMRuntimeType[] types = new LLVMRuntimeType[llvmParamTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = convertType(llvmParamTypes[i]);
        }
        return types;
    }

    public static LLVMRuntimeType convertType(LLVMType llvmReturnType) {
        if (llvmReturnType.isPointer()) {
            switch (llvmReturnType.getPointee().getType()) {
                case I1:
                    return LLVMRuntimeType.I1_POINTER;
                case I8:
                    return LLVMRuntimeType.I8_POINTER;
                case I16:
                    return LLVMRuntimeType.I16_POINTER;
                case I32:
                    return LLVMRuntimeType.I32_POINTER;
                case I64:
                    return LLVMRuntimeType.I64_POINTER;
                case HALF:
                    return LLVMRuntimeType.HALF_POINTER;
                case FLOAT:
                    return LLVMRuntimeType.FLOAT_POINTER;
                case DOUBLE:
                    return LLVMRuntimeType.DOUBLE_POINTER;
                default:
                    return LLVMRuntimeType.ADDRESS;
            }
        } else {
            return LLVMRuntimeType.valueOf(llvmReturnType.getType().toString());
        }
    }

    public static boolean isCompoundType(LLVMBaseType type) {
        return type == LLVMBaseType.ARRAY || type == LLVMBaseType.STRUCT || isVectorType(type);
    }

}
