package com.oracle.truffle.llvm.parser.adapters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intel.llvm.ireditor.types.ResolvedArrayType;
import com.intel.llvm.ireditor.types.ResolvedFloatingType;
import com.intel.llvm.ireditor.types.ResolvedFunctionType;
import com.intel.llvm.ireditor.types.ResolvedIntegerType;
import com.intel.llvm.ireditor.types.ResolvedMetadataType;
import com.intel.llvm.ireditor.types.ResolvedNamedType;
import com.intel.llvm.ireditor.types.ResolvedPointerType;
import com.intel.llvm.ireditor.types.ResolvedStructType;
import com.intel.llvm.ireditor.types.ResolvedType;
import com.intel.llvm.ireditor.types.ResolvedUnknownType;
import com.intel.llvm.ireditor.types.ResolvedVarargType;
import com.intel.llvm.ireditor.types.ResolvedVectorType;
import com.intel.llvm.ireditor.types.ResolvedVoidType;

import uk.ac.man.cs.llvm.ir.types.ArrayType;
import uk.ac.man.cs.llvm.ir.types.FloatingPointType;
import uk.ac.man.cs.llvm.ir.types.FunctionType;
import uk.ac.man.cs.llvm.ir.types.IntegerType;
import uk.ac.man.cs.llvm.ir.types.MetaType;
import uk.ac.man.cs.llvm.ir.types.PointerType;
import uk.ac.man.cs.llvm.ir.types.StructureType;
import uk.ac.man.cs.llvm.ir.types.Type;
import uk.ac.man.cs.llvm.ir.types.VectorType;

public class LLVMToBitcodeAdapter {

    private LLVMToBitcodeAdapter() {
    }

    public static Type resolveType(ResolvedType type) {
        if (type instanceof ResolvedNamedType) {
            return resolveType((ResolvedNamedType) type);
        } else if (type.isFunction()) {
            return resolveType((ResolvedFunctionType) type);
        } else if (type.isFloating()) {
            return resolveType((ResolvedFloatingType) type);
        } else if (type.isInteger()) {
            return resolveType((ResolvedIntegerType) type);
        } else if (type.isMetadata()) {
            return resolveType((ResolvedMetadataType) type);
        } else if (type.isPointer()) {
            return resolveType((ResolvedPointerType) type);
        } else if (type.isStruct()) {
            return resolveType((ResolvedStructType) type);
        } else if (type.isVararg()) {
            throw new AssertionError("varargs are only expected inside functions");
        } else if (type.isVector()) {
            return resolveType((ResolvedVectorType) type);
        } else if (type.isVoid()) {
            return resolveType((ResolvedVoidType) type);
        } else if (type.isUnknown()) {
            return resolveType((ResolvedUnknownType) type);
        } else if (type instanceof ResolvedArrayType) {
            return resolveType((ResolvedArrayType) type);
        }

        throw new AssertionError("Unknown type: " + type + " - " + type.getClass().getTypeName());
    }

    private static Map<ResolvedNamedType, Type> namedTypes = new HashMap<>();

    public static Type resolveType(ResolvedNamedType type) {
        if (!namedTypes.containsKey(type)) {
            namedTypes.put(type, MetaType.UNKNOWN); // TODO: resolve cycles
            namedTypes.put(type, resolveType(type.getReferredType()));
        }

        return namedTypes.get(type);
    }

    public static Type resolveType(ResolvedFunctionType type) {
        Type returnType = resolveType(type.getReturnType());
        List<Type> args = new ArrayList<>();
        boolean hasVararg = false;
        for (ResolvedType arg : type.getParameters()) {
            assert !hasVararg; // should be the last element of the parameterlist
            if (arg.isVararg()) {
                hasVararg = true;
            } else {
                args.add(resolveType(arg));
            }
        }
        return new FunctionType(returnType, args.toArray(new Type[args.size()]), hasVararg);
    }

    public static Type resolveType(ResolvedFloatingType type) {
        switch (type.getBits().intValue()) {
            case 16:
                return FloatingPointType.HALF;

            case 32:
                return FloatingPointType.FLOAT;

            case 64:
                return FloatingPointType.DOUBLE;

            case 80:
                return FloatingPointType.X86_FP80;

            case 128:
                return FloatingPointType.FP128;

            default:
                throw new AssertionError("Unknown bitsize: " + type.getBits());
        }
    }

    public static Type resolveType(ResolvedIntegerType type) {
        switch (type.getBits().intValue()) {

            case 1:
                return IntegerType.BOOLEAN;

            case 8:
                return IntegerType.BYTE;

            case 16:
                return IntegerType.SHORT;

            case 32:
                return IntegerType.INTEGER;

            case 64:
                return IntegerType.LONG;

            default:
                return new IntegerType(type.getBits().intValue());
        }
    }

    public static Type resolveType(@SuppressWarnings("unused") ResolvedMetadataType type) {
        return MetaType.METADATA;
    }

    public static Type resolveType(ResolvedPointerType type) {
        Type pointedType = resolveType(type.getContainedType(-1));
        return new PointerType(pointedType);
    }

    public static Type resolveType(ResolvedStructType type) {
        List<Type> elements = new ArrayList<>();
        for (ResolvedType arg : type.getFieldTypes()) {
            elements.add(resolveType(arg));
        }
        return new StructureType(type.isPacked(), elements.toArray(new Type[elements.size()]));
    }

    public static Type resolveType(ResolvedVectorType type) {
        Type elementType = resolveType(type.getContainedType(-1));
        return new VectorType(elementType, type.getSize());
    }

    public static Type resolveType(@SuppressWarnings("unused") ResolvedVoidType type) {
        return MetaType.VOID;
    }

    public static Type resolveType(@SuppressWarnings("unused") ResolvedUnknownType type) {
        return MetaType.UNKNOWN;
    }

    public static Type resolveType(ResolvedArrayType type) {
        Type elementType = resolveType(type.getContainedType(-1));
        return new ArrayType(elementType, type.getSize());
    }

    // temporary solution to convert the new type back to the old one
    public static ResolvedType unresolveType(Type type) {
        if (type instanceof FunctionType) {
            return unresolveType((FunctionType) type);
        } else if (type instanceof FloatingPointType) {
            return unresolveType((FloatingPointType) type);
        } else if (type instanceof IntegerType) {
            return unresolveType((IntegerType) type);
        } else if (type instanceof MetaType) {
            return unresolveType((MetaType) type);
        } else if (type instanceof PointerType) {
            return unresolveType((PointerType) type);
        } else if (type instanceof StructureType) {
            return unresolveType((StructureType) type);
        } else if (type instanceof ArrayType) {
            return unresolveType((ArrayType) type);
        } else if (type instanceof VectorType) {
            return unresolveType((VectorType) type);
        }

        throw new AssertionError("Unknown type: " + type + " - " + type.getClass().getTypeName());
    }

    public static ResolvedType unresolveType(FunctionType type) {
        ResolvedType returnType = unresolveType(type.getReturnType());
        List<ResolvedType> paramTypes = new ArrayList<>();
        for (Type t : type.getArgumentTypes()) {
            paramTypes.add(unresolveType(t));
        }
        if (type.isVarArg()) {
            paramTypes.add(new ResolvedVarargType());
        }
        return new ResolvedFunctionType(returnType, paramTypes);
    }

    public static ResolvedType unresolveType(FloatingPointType type) {
        return new ResolvedFloatingType(type.name(), type.width());
    }

    public static ResolvedType unresolveType(IntegerType type) {
        return new ResolvedIntegerType(type.getBitCount());
    }

    public static ResolvedType unresolveType(MetaType type) {
        switch (type) {
            case UNKNOWN:
                return new ResolvedUnknownType();

            case VOID:
                return new ResolvedVoidType();

            default:
                throw new AssertionError("Unknown type: " + type);
        }
    }

    public static ResolvedType unresolveType(PointerType type) {
        ResolvedType pointeeType = unresolveType(type.getPointeeType());
        BigInteger addrSpace = new BigInteger(new byte[]{(byte) 0}); // TODO: important?
        return new ResolvedPointerType(pointeeType, addrSpace);
    }

    public static ResolvedType unresolveType(StructureType type) {
        List<ResolvedType> fieldTypes = new ArrayList<>();
        for (int i = 0; i < type.getElementCount(); i++) {
            fieldTypes.add(unresolveType(type.getElementType(i)));
        }
        return new ResolvedStructType(fieldTypes, type.isPacked(), false);
    }

    public static ResolvedType unresolveType(ArrayType type) {
        ResolvedType elementType = unresolveType(type.getElementType());
        return new ResolvedArrayType(type.getElementCount(), elementType);
    }

    public static ResolvedType unresolveType(VectorType type) {
        ResolvedType elementType = unresolveType(type.getElementType());
        return new ResolvedVectorType(type.getElementCount(), elementType);
    }
}
