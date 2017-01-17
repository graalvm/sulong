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

public final class ConstantPrintVisitor implements ConstantVisitor {

    private final LLVMPrintVersion.LLVMPrintVisitors printVisitors;

    private final StringRepresentationVisitor stringRepr = new StringRepresentationVisitor();

    public ConstantPrintVisitor(LLVMPrintVersion.LLVMPrintVisitors printVisitors) {
        this.printVisitors = printVisitors;
    }

    public StringRepresentationVisitor getStringRepresentationVisitor() {
        return stringRepr;
    }

    @Override
    public void visit(ArrayConstant arrayConstant) {
        stringRepr.visit(arrayConstant);
    }

    @Override
    public void visit(StructureConstant structureConstant) {
        stringRepr.visit(structureConstant);
    }

    @Override
    public void visit(VectorConstant vectorConstant) {
        stringRepr.visit(vectorConstant);
    }

    @Override
    public void visit(BigIntegerConstant bigIntegerConstant) {
        printVisitors.print(bigIntegerConstant.getType());
        printVisitors.print(' ');
        stringRepr.visit(bigIntegerConstant);
    }

    @Override
    public void visit(BinaryOperationConstant binaryOperationConstant) {
        stringRepr.visit(binaryOperationConstant);
    }

    @Override
    public void visit(BlockAddressConstant blockAddressConstant) {
        stringRepr.visit(blockAddressConstant);
    }

    @Override
    public void visit(CastConstant castConstant) {
        stringRepr.visit(castConstant);
    }

    @Override
    public void visit(CompareConstant compareConstant) {
        stringRepr.visit(compareConstant);
    }

    @Override
    public void visit(DoubleConstant doubleConstant) {
        printVisitors.print(doubleConstant.getType());
        printVisitors.print(' ');
        stringRepr.visit(doubleConstant);
    }

    @Override
    public void visit(FloatConstant floatConstant) {
        printVisitors.print(floatConstant.getType());
        printVisitors.print(' ');
        stringRepr.visit(floatConstant);
    }

    @Override
    public void visit(X86FP80Constant x86fp80Constant) {
        printVisitors.print(x86fp80Constant.getType());
        printVisitors.print(' ');
        stringRepr.visit(x86fp80Constant);
    }

    @Override
    public void visit(FunctionDeclaration functionDeclaration) {
        stringRepr.visit(functionDeclaration);
    }

    @Override
    public void visit(FunctionDefinition functionDefinition) {
        stringRepr.visit(functionDefinition);
    }

    @Override
    public void visit(GetElementPointerConstant getElementPointerConstant) {
        stringRepr.visit(getElementPointerConstant);
    }

    @Override
    public void visit(InlineAsmConstant inlineAsmConstant) {
        stringRepr.visit(inlineAsmConstant);
    }

    @Override
    public void visit(IntegerConstant integerConstant) {
        printVisitors.print(integerConstant.getType());
        printVisitors.print(' ');
        stringRepr.visit(integerConstant);
    }

    @Override
    public void visit(NullConstant nullConstant) {
        printVisitors.print(nullConstant.getType());
        printVisitors.print(' ');
        stringRepr.visit(nullConstant);
    }

    @Override
    public void visit(StringConstant stringConstant) {
        printVisitors.print(stringConstant.getString()); // TODO: correct?
    }

    @Override
    public void visit(UndefinedConstant undefinedConstant) {
        stringRepr.visit(undefinedConstant);
    }

    public class StringRepresentationVisitor implements ConstantVisitor {

        @Override
        public void visit(ArrayConstant arrayConstant) {
            final StringJoiner joiner = new StringJoiner(", ", "[", "]");
            if (arrayConstant.getElementCount() == 0) {
                joiner.setEmptyValue("");
            }
            for (int i = 0; i < arrayConstant.getElementCount(); i++) {
                joiner.add(arrayConstant.getElement(i).toString());
            }
            printVisitors.print(joiner.toString());
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
            printVisitors.print(joiner.toString());
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
            printVisitors.print(joiner.toString());
        }

        @Override
        public void visit(BigIntegerConstant bigIntegerConstant) {
            if (bigIntegerConstant.getType().getBits() == 1) {
                printVisitors.print(bigIntegerConstant.getValue().equals(BigInteger.ZERO) ? "false" : "true");
            } else {
                printVisitors.print(bigIntegerConstant.getValue().toString());
            }
        }

        @Override
        public void visit(BinaryOperationConstant binaryOperationConstant) {
            printVisitors.print(binaryOperationConstant.getOperator().toString());
            printVisitors.print(' ');
            printVisitors.print(binaryOperationConstant.getType().toString());
            if (binaryOperationConstant.getLHS() instanceof Constant) {
                printVisitors.print(' ');
                printVisitors.getIRWriterUtil().printConstantValue((Constant) binaryOperationConstant.getLHS());
            } else {
                printVisitors.print(' ');
                printVisitors.print(binaryOperationConstant.getLHS().getType());
                printVisitors.print(' ');
                printVisitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getLHS());
            }
            printVisitors.print(',');
            if (binaryOperationConstant.getRHS() instanceof Constant) {
                printVisitors.print(' ');
                printVisitors.getIRWriterUtil().printConstantValue((Constant) binaryOperationConstant.getRHS());
            } else {
                printVisitors.print(' ');
                printVisitors.print(binaryOperationConstant.getRHS().getType());
                printVisitors.print(' ');
                printVisitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getRHS());
            }
            printVisitors.print(' ');
            printVisitors.print(binaryOperationConstant.getType());
            printVisitors.print(' ');
            printVisitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getLHS());
            printVisitors.print(", ");
            printVisitors.getIRWriterUtil().printSymbolName(binaryOperationConstant.getRHS());
        }

        @Override
        public void visit(BlockAddressConstant blockAddressConstant) {
            printVisitors.print(String.format("label %s", blockAddressConstant.getInstructionBlock().getName()));
        }

        @Override
        public void visit(CastConstant castConstant) {
            printVisitors.print(castConstant.getOperator());
            printVisitors.print(' ');
            printVisitors.print(castConstant.getValue().getType());
            printVisitors.print(' ');
            printVisitors.getIRWriterUtil().printSymbolName(castConstant.getValue());
            printVisitors.print(' ');
            printVisitors.print(castConstant.getType());
        }

        private static final String LLVMIR_LABEL_COMPARE = "icmp";
        private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

        @Override
        public void visit(CompareConstant compareConstant) {
            if (compareConstant.getOperator().isFloatingPoint()) {
                printVisitors.print(LLVMIR_LABEL_COMPARE_FP);
            } else {
                printVisitors.print(LLVMIR_LABEL_COMPARE);
            }
            printVisitors.print(' ');
            printVisitors.print(compareConstant.getOperator());
            printVisitors.print(' ');
            printVisitors.print(compareConstant.getType());
            printVisitors.print(' ');

            if (compareConstant.getLHS() instanceof Constant) {
                printVisitors.getIRWriterUtil().printConstantValue((Constant) compareConstant.getLHS());
            } else {
                printVisitors.getIRWriterUtil().printSymbolName(compareConstant.getLHS());
            }

            printVisitors.print(", ");
            if (compareConstant.getRHS() instanceof Constant) {
                printVisitors.getIRWriterUtil().printConstantValue((Constant) compareConstant.getRHS());
            } else {
                printVisitors.getIRWriterUtil().printSymbolName(compareConstant.getRHS());
            }
        }

        @Override
        public void visit(DoubleConstant doubleConstant) {
            printVisitors.print(String.format(Locale.ROOT, "%.15f", doubleConstant.getValue()));
        }

        @Override
        public void visit(FloatConstant floatConstant) {
            printVisitors.print(String.format(Locale.ROOT, "%.6f", floatConstant.getValue()));
        }

        @Override
        public void visit(X86FP80Constant x86fp80Constant) {
            // TODO: remove dependency to getStringValue?
            printVisitors.print(x86fp80Constant.getStringValue());
        }

        private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

        @Override
        public void visit(FunctionDeclaration functionDeclaration) {
            // TODO: remove dependency to toString
            Stream<String> argumentStream = Arrays.stream(functionDeclaration.getArgumentTypes()).map(Type::toString);
            if (functionDeclaration.isVarArg()) {
                argumentStream = Stream.concat(argumentStream, Stream.of("..."));
            }
            printVisitors.print(String.format("declare %s %s(%s)", functionDeclaration.getReturnType(), functionDeclaration.getName(),
                            argumentStream.collect(Collectors.joining(", "))));
        }

        @Override
        public void visit(FunctionDefinition functionDefinition) {
            // TODO: remove dependency to toString
            Stream<String> parameterStream = functionDefinition.getParameters().stream().map(FunctionParameter::toString);
            if (functionDefinition.isVarArg()) {
                parameterStream = Stream.concat(parameterStream, Stream.of("..."));
            }

            printVisitors.print(String.format("define %s %s(%s)", functionDefinition.getReturnType(), functionDefinition.getName(),
                            parameterStream.collect(Collectors.joining(", "))));
        }

        @Override
        public void visit(GetElementPointerConstant getElementPointerConstant) {
            // getelementptr
            printVisitors.print(LLVMIR_LABEL_GET_ELEMENT_POINTER);

            // [inbounds]
            if (getElementPointerConstant.isInbounds()) {
                printVisitors.print(" inbounds");
            }

            // <pty>* <ptrval>
            printVisitors.print(" (");
            printVisitors.print(getElementPointerConstant.getBasePointer().getType());
            printVisitors.print(' ');
            printVisitors.getIRWriterUtil().printInnerSymbolValue(getElementPointerConstant.getBasePointer());

            // {, <ty> <idx>}*
            for (final Symbol sym : getElementPointerConstant.getIndices()) {
                printVisitors.print(", ");
                sym.getType().accept(printVisitors.getTypeVisitor());
                printVisitors.print(" ");
                printVisitors.getIRWriterUtil().printInnerSymbolValue(sym);
            }

            printVisitors.print(')');
        }

        private static final String LLVMIR_LABEL_ASM = "asm";

        private static final String LLVMIR_ASM_KEYWORD_SIDEEFFECT = "sideeffect";

        private static final String LLVMIR_ASM_KEYWORD_ALIGNSTACK = "alignstack";

        @Override
        public void visit(InlineAsmConstant inlineAsmConstant) {
            final FunctionType decl = (FunctionType) ((PointerType) inlineAsmConstant.getType()).getPointeeType();

            if (decl.getReturnType() != MetaType.VOID) {
                printVisitors.print(decl.getReturnType());
                printVisitors.print(' ');
            }

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                printVisitors.print(decl);
                printTypeSignature(decl);
                printVisitors.print(' ');
            }

            printVisitors.print(LLVMIR_LABEL_ASM);

            if (inlineAsmConstant.hasSideEffects()) {
                printVisitors.print(' ');
                printVisitors.print(LLVMIR_ASM_KEYWORD_SIDEEFFECT);
            }

            if (inlineAsmConstant.needsAlignedStack()) {
                printVisitors.print(' ');
                printVisitors.print(LLVMIR_ASM_KEYWORD_ALIGNSTACK);
            }

            if (inlineAsmConstant.getDialect() != AsmDialect.AT_T) {
                printVisitors.print(' ');
                printVisitors.print(inlineAsmConstant.getDialect().getIrString());
            }

            printVisitors.print(' ');
            printVisitors.print(inlineAsmConstant.getAsmExpression());

            printVisitors.print(", ");
            printVisitors.print(inlineAsmConstant.getAsmFlags());
        }

        // TODO: move into visitor?
        private void printTypeSignature(FunctionType functionType) {
            // TODO: remove dependency to toString
            Stream<String> argTypeStream = Arrays.stream(functionType.getArgumentTypes()).map(Type::toString);
            if (functionType.isVarArg()) {
                argTypeStream = Stream.concat(argTypeStream, Stream.of("..."));
            }
            printVisitors.print(argTypeStream.collect(Collectors.joining(", ", "(", ")")));
        }

        @Override
        public void visit(IntegerConstant integerConstant) {
            if (integerConstant.getType().getBits() == 1) {
                printVisitors.print(integerConstant.getValue() == 0 ? "false" : "true");
            } else {
                printVisitors.print(String.valueOf(integerConstant.getValue()));
            }
        }

        @Override
        public void visit(NullConstant nullConstant) {
            if (nullConstant.getType() instanceof IntegerType) {
                printVisitors.print(String.valueOf(0));
            } else if (nullConstant.getType() instanceof FloatingPointType) {
                printVisitors.print(String.valueOf(0.0));
            } else {
                printVisitors.print("null");
            }
        }

        @Override
        public void visit(StringConstant stringConstant) {
            printVisitors.print("c\"");
            for (int i = 0; i < stringConstant.getString().length(); i++) {
                byte b = (byte) stringConstant.getString().charAt(i);
                if (b < ' ' || b >= '~') {
                    printVisitors.print(String.format("\\%02X", b));
                } else {
                    printVisitors.print((char) b);
                }
            }
            if (stringConstant.getType() instanceof ArrayType && ((ArrayType) stringConstant.getType()).getLength() > stringConstant.getString().length()) {
                printVisitors.print("\\00");
            }
            printVisitors.print("\"");
        }

        @Override
        public void visit(UndefinedConstant undefinedConstant) {
            printVisitors.print("undef");
        }

    }
}
