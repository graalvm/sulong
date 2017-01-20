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
package com.oracle.truffle.llvm.parser.bc.irwriter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.truffle.llvm.parser.api.model.enums.AsmDialect;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.BinaryOperationConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.BlockAddressConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.CompareConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.GetElementPointerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.StringConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.UndefinedConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.aggregate.ArrayConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.aggregate.StructureConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.aggregate.VectorConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.floatingpoint.DoubleConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.floatingpoint.FloatConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.floatingpoint.X86FP80Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.visitors.ConstantVisitor;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FloatingPointType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.IntegerType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

final class ConstantPrintVisitor implements ConstantVisitor {

    private final LLVMPrintVersion.LLVMPrintVisitors visitors;

    private final LLVMIRPrinter.PrintTarget out;

    ConstantPrintVisitor(LLVMPrintVersion.LLVMPrintVisitors visitors, LLVMIRPrinter.PrintTarget target) {
        this.visitors = visitors;
        this.out = target;
    }

    @Override
    public void visit(ArrayConstant arrayConstant) {
        final StringJoiner joiner = new StringJoiner(", ", "[", "]");
        if (arrayConstant.getElementCount() == 0) {
            joiner.setEmptyValue("");
        }
        for (int i = 0; i < arrayConstant.getElementCount(); i++) {
            joiner.add(arrayConstant.getElement(i).toString());
        }
        out.print(joiner.toString());
    }

    @Override
    public void visit(StructureConstant structureConstant) {
        final StringJoiner joiner = new StringJoiner(", ", "{", "}");
        if (structureConstant.getElementCount() == 0) {
            joiner.setEmptyValue("");
        }
        for (int i = 0; i < structureConstant.getElementCount(); i++) {
            joiner.add(structureConstant.getElement(i).toString());
        }
        out.print(joiner.toString());
    }

    @Override
    public void visit(VectorConstant vectorConstant) {
        final StringJoiner joiner = new StringJoiner(", ", "<", ">");
        if (vectorConstant.getElementCount() == 0) {
            joiner.setEmptyValue("");
        }
        for (int i = 0; i < vectorConstant.getElementCount(); i++) {
            joiner.add(vectorConstant.getElement(i).toString());
        }
        out.print(joiner.toString());
    }

    @Override
    public void visit(BigIntegerConstant bigIntegerConstant) {
        if (bigIntegerConstant.getType().getBits() == 1) {
            out.print(bigIntegerConstant.getValue().equals(BigInteger.ZERO) ? "false" : "true");
        } else {
            out.print(bigIntegerConstant.getValue().toString());
        }
    }

    @Override
    public void visit(BinaryOperationConstant binaryOperationConstant) {
        out.print(binaryOperationConstant.getOperator().toString());
        out.print(" ");
        out.print(binaryOperationConstant.getType().toString());
        if (binaryOperationConstant.getLHS() instanceof Constant) {
            out.print(" ");
            visitors.getIRWriterUtil().printConstantValue((Constant) binaryOperationConstant.getLHS());
        } else {
            out.print(" ");
            binaryOperationConstant.getLHS().getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getLHS());
        }
        out.print(",");
        if (binaryOperationConstant.getRHS() instanceof Constant) {
            out.print(" ");
            visitors.getIRWriterUtil().printConstantValue((Constant) binaryOperationConstant.getRHS());
        } else {
            out.print(" ");
            binaryOperationConstant.getRHS().getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getRHS());
        }
        out.print(" ");
        binaryOperationConstant.getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getLHS());
        out.print(", ");
        visitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getRHS());
    }

    @Override
    public void visit(BlockAddressConstant blockAddressConstant) {
        out.print(String.format("label %s", blockAddressConstant.getInstructionBlock().getName()));
    }

    @Override
    public void visit(CastConstant castConstant) {
        out.print(castConstant.getOperator().toString());
        out.print(" ");
        castConstant.getValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printSymbolName(castConstant.getValue());
        out.print(" ");
        castConstant.getType().accept(visitors.getTypeVisitor());
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareConstant compareConstant) {
        if (compareConstant.getOperator().isFloatingPoint()) {
            out.print(LLVMIR_LABEL_COMPARE_FP);
        } else {
            out.print(LLVMIR_LABEL_COMPARE);
        }
        out.print(" ");
        out.print(compareConstant.getOperator().toString());
        out.print(" ");
        compareConstant.getType().accept(visitors.getTypeVisitor());
        out.print(" ");

        if (compareConstant.getLHS() instanceof Constant) {
            visitors.getIRWriterUtil().printConstantValue((Constant) compareConstant.getLHS());
        } else {
            visitors.getIRWriterUtil().printSymbolName(compareConstant.getLHS());
        }

        out.print(", ");
        if (compareConstant.getRHS() instanceof Constant) {
            visitors.getIRWriterUtil().printConstantValue((Constant) compareConstant.getRHS());
        } else {
            visitors.getIRWriterUtil().printSymbolName(compareConstant.getRHS());
        }
    }

    @Override
    public void visit(DoubleConstant doubleConstant) {
        out.print(String.format(Locale.ROOT, "%.15f", doubleConstant.getValue()));
    }

    @Override
    public void visit(FloatConstant floatConstant) {
        out.print(String.format(Locale.ROOT, "%.6f", floatConstant.getValue()));
    }

    @Override
    public void visit(X86FP80Constant x86fp80Constant) {
        // TODO: remove dependency to getStringValue?
        out.print(x86fp80Constant.getStringValue());
    }

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    @Override
    public void visit(FunctionDeclaration functionDeclaration) {
        // TODO: remove dependency to toString
        Stream<String> argumentStream = Arrays.stream(functionDeclaration.getArgumentTypes()).map(Type::toString);
        if (functionDeclaration.isVarArg()) {
            argumentStream = Stream.concat(argumentStream, Stream.of("..."));
        }
        out.print(String.format("declare %s %s(%s)", functionDeclaration.getReturnType(), functionDeclaration.getName(),
                        argumentStream.collect(Collectors.joining(", "))));
    }

    @Override
    public void visit(FunctionDefinition functionDefinition) {
        // TODO: remove dependency to toString
        Stream<String> parameterStream = functionDefinition.getParameters().stream().map(FunctionParameter::toString);
        if (functionDefinition.isVarArg()) {
            parameterStream = Stream.concat(parameterStream, Stream.of("..."));
        }

        out.print(String.format("define %s %s(%s)", functionDefinition.getReturnType(), functionDefinition.getName(),
                        parameterStream.collect(Collectors.joining(", "))));
    }

    @Override
    public void visit(GetElementPointerConstant getElementPointerConstant) {
        // getelementptr
        out.print(LLVMIR_LABEL_GET_ELEMENT_POINTER);

        // [inbounds]
        if (getElementPointerConstant.isInbounds()) {
            out.print(" inbounds");
        }

        // <pty>* <ptrval>
        out.print(" (");
        getElementPointerConstant.getBasePointer().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(getElementPointerConstant.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : getElementPointerConstant.getIndices()) {
            out.print(", ");
            sym.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(sym);
        }

        out.print(")");
    }

    private static final String LLVMIR_LABEL_ASM = "asm";

    private static final String LLVMIR_ASM_KEYWORD_SIDEEFFECT = "sideeffect";

    private static final String LLVMIR_ASM_KEYWORD_ALIGNSTACK = "alignstack";

    @Override
    public void visit(InlineAsmConstant inlineAsmConstant) {
        final FunctionType decl = (FunctionType) ((PointerType) inlineAsmConstant.getType()).getPointeeType();

        if (decl.getReturnType() != MetaType.VOID) {
            decl.getReturnType().accept(visitors.getTypeVisitor());
            out.print(" ");
        }

        if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
            out.print(decl.toString()); // TODO use visitors
            printTypeSignature(decl);
            out.print(" ");
        }

        out.print(LLVMIR_LABEL_ASM);

        if (inlineAsmConstant.hasSideEffects()) {
            out.print(" ");
            out.print(LLVMIR_ASM_KEYWORD_SIDEEFFECT);
        }

        if (inlineAsmConstant.needsAlignedStack()) {
            out.print(" ");
            out.print(LLVMIR_ASM_KEYWORD_ALIGNSTACK);
        }

        if (inlineAsmConstant.getDialect() != AsmDialect.AT_T) {
            out.print(" ");
            out.print(inlineAsmConstant.getDialect().getIrString());
        }

        out.print(" ");
        out.print(inlineAsmConstant.getAsmExpression());

        out.print(", ");
        out.print(inlineAsmConstant.getAsmFlags());
    }

    // TODO: move into visitor?
    private void printTypeSignature(FunctionType functionType) {
        // TODO: remove dependency to toString
        Stream<String> argTypeStream = Arrays.stream(functionType.getArgumentTypes()).map(Type::toString);
        if (functionType.isVarArg()) {
            argTypeStream = Stream.concat(argTypeStream, Stream.of("..."));
        }
        out.print(argTypeStream.collect(Collectors.joining(", ", "(", ")")));
    }

    @Override
    public void visit(IntegerConstant integerConstant) {
        if (integerConstant.getType().getBits() == 1) {
            out.print(integerConstant.getValue() == 0 ? "false" : "true");
        } else {
            out.print(String.valueOf(integerConstant.getValue()));
        }
    }

    @Override
    public void visit(NullConstant nullConstant) {
        if (nullConstant.getType() instanceof IntegerType) {
            out.print(String.valueOf(0));
        } else if (nullConstant.getType() instanceof FloatingPointType) {
            out.print(String.valueOf(0.0));
        } else {
            out.print("null");
        }
    }

    @Override
    public void visit(StringConstant stringConstant) {
        out.print("c\"");
        for (int i = 0; i < stringConstant.getString().length(); i++) {
            byte b = (byte) stringConstant.getString().charAt(i);
            if (b < ' ' || b >= '~') {
                out.print(String.format("\\%02X", b));
            } else {
                out.print(Character.toString((char) b));
            }
        }
        if (stringConstant.getType() instanceof ArrayType && ((ArrayType) stringConstant.getType()).getLength() > stringConstant.getString().length()) {
            out.print("\\00");
        }
        out.print("\"");
    }

    @Override
    public void visit(UndefinedConstant undefinedConstant) {
        out.print("undef");
    }
}
