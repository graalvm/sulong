package com.oracle.truffle.llvm.writer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;

import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.BigIntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.FloatingPointType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.IntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.IntegerType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantPointerType;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantType;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

public class TypePrintVisitor implements TypeVisitor {

    private final PrintWriter out;

    public TypePrintVisitor(PrintWriter out) {
        this.out = out;
    }

    public TypePrintVisitor(OutputStream out) {
        this(new PrintWriter(out));
    }

    @Override
    public void visit(BigIntegerConstantType bigIntegerConstantType) {
        if (bigIntegerConstantType.getType().getBits() == 1) {
            out.print(bigIntegerConstantType.getValue().equals(BigInteger.ZERO) ? "i1 false" : "i1 true");
        }
        bigIntegerConstantType.getType().accept(this);
        out.print(String.format(" %s", bigIntegerConstantType.getValue()));
    }

    @Override
    public void visit(FloatingPointType floatingPointType) {
        out.print(floatingPointType.name().toLowerCase());
    }

    @Override
    public void visit(FunctionType functionType) {
        functionType.getReturnType().accept(this);

        out.print(" (");

        for (int i = 0; i < functionType.getArgumentTypes().length; i++) {
            if (i > 0) {
                out.print(", ");
            }
            functionType.getArgumentTypes()[i].accept(this);
        }

        if (functionType.isVarArg()) {
            if (functionType.getArgumentTypes().length > 0) {
                out.print(", ");
            }
            out.print("...");
        }
        out.print(")");
    }

    @Override
    public void visit(IntegerConstantType integerConstantType) {
        if (integerConstantType.getType().getBits() == 1) {
            out.print(integerConstantType.getValue() == 0 ? "i1 false" : "i1 true");
        }
        integerConstantType.getType().accept(this);
        out.print(String.format(" %d", integerConstantType.getValue()));
    }

    @Override
    public void visit(IntegerType integerType) {
        out.print(String.format("i%d", integerType.getBits()));
    }

    @Override
    public void visit(MetadataConstantType metadataConstantType) {
        metadataConstantType.getType().accept(this);
        out.print(String.format(" %d", metadataConstantType.getValue()));
    }

    @Override
    public void visit(MetadataConstantPointerType metadataConstantPointerType) {
        out.print(String.format("!!%d", metadataConstantPointerType.getSymbolIndex()));
    }

    @Override
    public void visit(MetaType metaType) {
        out.print(metaType.name().toLowerCase());
    }

    @Override
    public void visit(PointerType pointerType) {
        pointerType.getPointeeType().accept(this);
        out.print("*");
    }

    @Override
    public void visit(ArrayType arrayType) {
        out.print(String.format("[%d", arrayType.getLength()));
        out.print(" x ");
        arrayType.getElementType().accept(this);
        out.print("]");
    }

    @Override
    public void visit(StructureType structureType) {
        if (structureType.getName().equals(ValueSymbol.UNKNOWN)) {
            out.print(structureType.toDeclarationString());
        } else {
            out.print("%" + structureType.getName());
        }
    }

    @Override
    public void visit(VectorType vectorType) {
        out.print(String.format("<%d", vectorType.getLength()));
        out.print(" x ");
        vectorType.getElementType().accept(this);
        out.print(">");
    }

}
