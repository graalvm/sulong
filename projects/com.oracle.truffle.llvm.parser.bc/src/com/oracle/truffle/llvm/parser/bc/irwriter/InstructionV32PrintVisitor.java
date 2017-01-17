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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.oracle.truffle.llvm.parser.api.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.api.model.enums.Flag;
import com.oracle.truffle.llvm.parser.api.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.CallInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.CastInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.CompareInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ConditionalBranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ExtractValueInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.IndirectBranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.InsertValueInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.PhiInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.SelectInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ShuffleVectorInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.StoreInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.SwitchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.SwitchOldInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.UnreachableInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.VoidCallInstruction;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class InstructionV32PrintVisitor implements InstructionVisitor {

    private final LLVMPrintVersion.LLVMPrintVisitors printVisitors;

    public InstructionV32PrintVisitor(LLVMPrintVersion.LLVMPrintVisitors printVisitors) {
        this.printVisitors = printVisitors;
    }

    private static final String INDENTATION = "    ";

    private static final String LLVMIR_LABEL_ALLOCATE = "alloca";

    private static String getSymbolName(Symbol s) {
        if (s instanceof ValueSymbol) {
            return ((ValueSymbol) s).getName();
        } else {
            return s.toString(); // TODO
        }
    }

    @Override
    public void visit(AllocateInstruction allocate) {
        printVisitors.print(INDENTATION);
        // <result> = alloca <type>
        printVisitors.print(String.format("%s = %s %s", allocate.getName(), LLVMIR_LABEL_ALLOCATE, allocate.getPointeeType()));

        // [, <ty> <NumElements>]
        if (!(allocate.getCount() instanceof IntegerConstant && ((IntegerConstant) allocate.getCount()).getValue() == 1)) {
            printVisitors.print(String.format(", %s %s", allocate.getCount().getType(), allocate.getCount()));
        }

        // [, align <alignment>]
        if (allocate.getAlign() != 0) {
            printVisitors.print(String.format(", align %d", 1 << (allocate.getAlign() - 1)));
        }

        printVisitors.println();
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        printVisitors.print(INDENTATION);
        // <result> = <op>
        printVisitors.print(String.format("%s = %s", operation.getName(), operation.getOperator()));

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            printVisitors.print(" " + flag);
        }

        // <ty> <op1>, <op2>
        printVisitors.print(String.format(" %s %s, %s", operation.getType(),
                        getSymbolName(operation.getLHS()),
                        getSymbolName(operation.getRHS())));

        printVisitors.println();
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    @Override
    public void visit(BranchInstruction branch) {
        printVisitors.print(INDENTATION);
        printVisitors.println(String.format("%s label %s", LLVMIR_LABEL_BRANCH, branch.getSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_CALL = "call";

    @Override
    public void visit(CallInstruction call) {
        printVisitors.print(INDENTATION);
        // <result> = [tail] call
        printVisitors.print(String.format("%s = %s", call.getName(), LLVMIR_LABEL_CALL)); // TODO:
                                                                                          // [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            FunctionType decl = (FunctionType) call.getCallTarget();

            printVisitors.print(String.format(" %s", decl.getReturnType().toString()));

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                printVisitors.print(String.format(" %s*", decl)); // TODO: implement
                                                                  // getTypeSignature()
            }
            printVisitors.print(String.format(" %s", decl.getName()));
        } else if (call.getCallTarget() instanceof LoadInstruction) {
            Type targetType = ((LoadInstruction) call.getCallTarget()).getSource().getType();
            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }
            if (targetType instanceof FunctionType) {
                printVisitors.print(String.format(" %s %s", ((FunctionType) targetType).getReturnType(), getSymbolName(call.getCallTarget())));
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (call.getCallTarget() instanceof FunctionParameter) {
            printVisitors.print(String.format(" %s", call.getCallTarget().getType().toString()));

        } else if (call.getCallTarget() instanceof InlineAsmConstant) {
            printVisitors.print(String.format(" %s", call.getCallTarget().toString()));

        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        List<Symbol> arguments = new ArrayList<>(call.getArgumentCount());
        for (int i = 0; i < call.getArgumentCount(); i++) {
            arguments.add(call.getArgument(i));
        }
        printVisitors.print(argsToString(arguments));

        // [fn attrs]
        // TODO: implement
        printVisitors.println();
    }

    private static String argsToString(List<Symbol> arguments) {
        return arguments.stream().map(s -> {
            if (s instanceof Constant) {
                return s.toString();
            } else {
                return String.format("%s %s", s.getType().toString(), getSymbolName(s));
            }
        }).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public void visit(CastInstruction cast) {
        printVisitors.print(INDENTATION);
        printVisitors.println(String.format("%s = %s %s %s to %s", cast.getName(), cast.getOperator(), cast.getValue().getType(), getSymbolName(cast.getValue()), cast.getType()));
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareInstruction operation) {
        printVisitors.print(INDENTATION);
        if (operation.getOperator().isFloatingPoint()) {
            // <result> = fcmp <cond> <ty> <op1>, <op2>
            printVisitors.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE_FP, operation.getOperator(), operation.getBaseType(),
                            getSymbolName(operation.getLHS()), getSymbolName(operation.getRHS())));
        } else {
            // <result> = icmp <cond> <ty> <op1>, <op2>
            printVisitors.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE, operation.getOperator(), operation.getBaseType(),
                            getSymbolName(operation.getLHS()), getSymbolName(operation.getRHS())));
        }
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        printVisitors.print(INDENTATION);
        // br i1 <cond>, label <iftrue>, label <iffalse>
        printVisitors.println(String.format("%s %s %s, label %s, label %s", LLVMIR_LABEL_CONDITIONAL_BRANCH,
                        branch.getCondition().getType(), getSymbolName(branch.getCondition()),
                        branch.getTrueSuccessor().getName(),
                        branch.getFalseSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    @Override
    public void visit(ExtractElementInstruction extract) {
        printVisitors.print(INDENTATION);
        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        printVisitors.println(String.format("%s = %s %s %s, %s %s", extract.getName(), LLVMIR_LABEL_EXTRACT_ELEMENT,
                        extract.getVector().getType(), getSymbolName(extract.getVector()),
                        extract.getIndex().getType(), getSymbolName(extract.getIndex())));
    }

    private static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    @Override
    public void visit(ExtractValueInstruction extract) {
        printVisitors.print(INDENTATION);
        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        printVisitors.println(String.format("%s = %s %s %s, %d", extract.getName(), LLVMIR_LABEL_EXTRACT_VALUE,
                        extract.getAggregate().getType(), getSymbolName(extract.getAggregate()),
                        extract.getIndex()));
    }

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    @Override
    public void visit(GetElementPointerInstruction gep) {
        printVisitors.print(INDENTATION);
        // <result> = getelementptr
        printVisitors.print(String.format("%s = %s", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER));

        // [inbounds]
        if (gep.isInbounds()) {
            printVisitors.print(" inbounds");
        }

        // <pty>* <ptrval>
        printVisitors.print(String.format(" %s %s", gep.getBasePointer().getType(), getSymbolName(gep.getBasePointer())));

        // {, <ty> <idx>}*
        for (Symbol sym : gep.getIndices()) {
            if (sym instanceof Constant) {
                printVisitors.print(String.format(", %s", sym.toString()));
            } else {
                printVisitors.print(String.format(", %s %s", sym.getType().toString(), getSymbolName(sym)));
            }
        }

        printVisitors.println();
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    @Override
    public void visit(IndirectBranchInstruction branch) {
        printVisitors.print(INDENTATION);
        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        // @formatter:off
        printVisitors.println(String.format("%s %s %s, [ %s ]", LLVMIR_LABEL_INDIRECT_BRANCH,
                        branch.getAddress().getType(), getSymbolName(branch.getAddress()),
                        branch.getSuccessors().stream().map(s ->
                             String.format("label %s", s.getName())
                        ).collect(Collectors.joining(", "))));
        // @formatter:on
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    @Override
    public void visit(InsertElementInstruction insert) {
        printVisitors.print(INDENTATION);
        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        printVisitors.println(String.format("%s = %s %s %s, %s %s, %s %s", insert.getName(), LLVMIR_LABEL_INSERT_ELEMENT,
                        insert.getVector().getType(), getSymbolName(insert.getVector()),
                        insert.getValue().getType(), getSymbolName(insert.getValue()),
                        insert.getIndex().getType(), getSymbolName(insert.getIndex())));
    }

    private static final String LLVMIR_LABEL_INSERT_VALUE = "insertvalue";

    @Override
    public void visit(InsertValueInstruction insert) {
        printVisitors.print(INDENTATION);
        // <result> = insertvalue <aggregate type> <val>, <ty> <elt>, <idx>{, <idx>}*
        printVisitors.println(String.format("%s = %s %s %s, %s %s, %d", insert.getName(), LLVMIR_LABEL_INSERT_VALUE,
                        insert.getAggregate().getType(), getSymbolName(insert.getAggregate()),
                        insert.getValue().getType(), getSymbolName(insert.getValue()),
                        insert.getIndex()));
    }

    private static final String LLVMIR_LABEL_LOAD = "load";

    @Override
    public void visit(LoadInstruction load) {
        printVisitors.print(INDENTATION);
        // <result> = load
        printVisitors.print(String.format("%s = %s", load.getName(), LLVMIR_LABEL_LOAD));

        if (load.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            // [volatile]
            if (load.isVolatile()) {
                printVisitors.print(" volatile");
            }

            // <ty>* <pointer>
            printVisitors.print(String.format(" %s %s", load.getSource().getType(), getSymbolName(load.getSource())));

            // [, align <alignment>]
            if (load.getAlign() != 0) {
                printVisitors.print(String.format(", align %d", 1 << (load.getAlign() - 1)));
            }

            // [, !nontemporal !<index>][, !invariant.load !<index>]
            // TODO: implement
        } else {
            // atomic
            printVisitors.print(" atomic");

            // [volatile]
            if (load.isVolatile()) {
                printVisitors.print(" volatile");
            }

            // <ty>* <pointer>
            printVisitors.print(String.format(" %s %s", load.getSource().getType(), getSymbolName(load.getSource())));

            // [singlethread]
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                printVisitors.print(" singlethread");
            }

            // <ordering>, align <alignment>
            printVisitors.print(String.format(" %s, align %d", load.getAtomicOrdering(), 1 << (load.getAlign() - 1)));
        }

        printVisitors.println();
    }

    private static final String LLVMIR_LABEL_PHI = "phi";

    @Override
    public void visit(PhiInstruction phi) {
        printVisitors.print(INDENTATION);
        // <result> = phi <ty>
        printVisitors.print(String.format("%s = %s %s", phi.getName(), LLVMIR_LABEL_PHI, phi.getType()));

        // [ <val0>, <label0>], ...
        // @formatter:off
        printVisitors.print(IntStream.range(0, phi.getSize()).mapToObj(i ->
                            String.format(" [ %s, %s ]", getSymbolName(phi.getValue(i)), phi.getBlock(i).getName())
                        ).collect(Collectors.joining(",")));
        // @formatter:on

        printVisitors.println();
    }

    private static final String LLVMIR_LABEL_RETURN = "ret";

    @Override
    public void visit(ReturnInstruction ret) {
        printVisitors.print(INDENTATION);
        if (ret.getValue() == null) {
            // ret void
            printVisitors.println(String.format("%s void", LLVMIR_LABEL_RETURN));
        } else {
            // ret <type> <value>
            printVisitors.println(String.format("%s %s %s", LLVMIR_LABEL_RETURN, ret.getValue().getType(), getSymbolName(ret.getValue())));
        }
    }

    private static final String LLVMIR_LABEL_SELECT = "select";

    @Override
    public void visit(SelectInstruction select) {
        printVisitors.print(INDENTATION);
        // <result> = select selty <cond>, <ty> <val1>, <ty> <val2>
        printVisitors.println(String.format("%s = %s %s %s, %s %s, %s %s", select.getName(), LLVMIR_LABEL_SELECT,
                        select.getCondition().getType(), getSymbolName(select.getCondition()),
                        select.getTrueValue().getType(), getSymbolName(select.getTrueValue()),
                        select.getFalseValue().getType(), getSymbolName(select.getFalseValue())));
    }

    private static final String LLVMIR_LABEL_SHUFFLE_VECTOR = "shufflevector";

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        printVisitors.print(INDENTATION);
        // <result> = shufflevector <n x <ty>> <v1>, <n x <ty>> <v2>, <m x i32> <mask>
        printVisitors.println(String.format("%s = %s %s %s, %s %s, %s %s", shuffle.getName(), LLVMIR_LABEL_SHUFFLE_VECTOR,
                        shuffle.getVector1().getType(), getSymbolName(shuffle.getVector1()),
                        shuffle.getVector2().getType(), getSymbolName(shuffle.getVector2()),
                        shuffle.getMask().getType(), getSymbolName(shuffle.getMask())));
    }

    private static final String LLVMIR_LABEL_STORE = "store";

    @Override
    public void visit(StoreInstruction store) {
        printVisitors.print(INDENTATION);

        printVisitors.print(String.format("%s ", LLVMIR_LABEL_STORE));

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            printVisitors.print("atomic ");
        }

        if (store.isVolatile()) {
            printVisitors.print("volatile ");
        }

        ((PointerType) store.getDestination().getType()).getPointeeType().accept(printVisitors.getTypeVisitor());
        printVisitors.print(" ");
        printVisitors.getIRWriterUtil().printInnerSymbolValue(store.getSource());
        printVisitors.print(", ");
        store.getDestination().getType().accept(printVisitors.getTypeVisitor());
        printVisitors.print(" ");
        printVisitors.getIRWriterUtil().printInnerSymbolValue(store.getDestination());

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (store.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                printVisitors.print(" singlethread ");
            }

            printVisitors.print(store.getAtomicOrdering());
        }

        printVisitors.println(String.format(", align %d", 1 << (store.getAlign() - 1)));
    }

    public static final String LLVMIR_LABEL_SWITCH = "switch";

    @Override
    public void visit(SwitchInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        String mainStr = INDENTATION;
        mainStr += String.format("%s %s %s, label %s", LLVMIR_LABEL_SWITCH, select.getCondition().getType(), getSymbolName(select.getCondition()), select.getDefaultBlock().getName());
        printVisitors.print(mainStr);
        // [ <intty> <val>, label <dest> ... ]
        printVisitors.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        printVisitors.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                        final Symbol val = select.getCaseValue(i);
                        final Symbol blk = select.getCaseBlock(i);
                        return String.format("%s %s, label %s", val.getType(), getSymbolName(val), getSymbolName(blk));
                    }).collect(Collectors.joining(indent)));
        // @formatter:on
        printVisitors.print("]");

        printVisitors.println();
    }

    private static String buildIndent(int length) {
        final StringBuilder builder = new StringBuilder(length + 1);
        builder.append('\n');
        for (int i = 0; i < length; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static final String LLVMIR_LABEL_SWITCH_OLD = "switch";

    @Override
    public void visit(SwitchOldInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        String mainStr = INDENTATION;
        mainStr += String.format("%s %s %s, label %s", LLVMIR_LABEL_SWITCH_OLD, select.getCondition().getType(), getSymbolName(select.getCondition()),
                        select.getDefaultBlock().getName());
        printVisitors.print(mainStr);

        // [ <intty> <val>, label <dest> ... ]
        printVisitors.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        printVisitors.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                    final long val = select.getCaseValue(i);
                    final Symbol blk = select.getCaseBlock(i);
                    return String.format("%s %d, label %s", select.getCondition().getType(), val, getSymbolName(blk));
                }).collect(Collectors.joining(indent)));
        // @formatter:on
        printVisitors.print("]");

        printVisitors.println();
    }

    private static final String LLVMIR_LABEL_UNREACHABLE = "unreachable";

    @Override
    public void visit(UnreachableInstruction unreachable) {
        printVisitors.print(INDENTATION);
        printVisitors.println(LLVMIR_LABEL_UNREACHABLE);
    }

    private static final String LLVMIR_LABEL_VOID_CALL = "call";

    @Override
    public void visit(VoidCallInstruction call) {
        printVisitors.print(INDENTATION);
        // [tail] call
        printVisitors.print(LLVMIR_LABEL_VOID_CALL); // TODO: [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            FunctionType decl = (FunctionType) call.getCallTarget();
            printVisitors.print(String.format(" %s", decl.getReturnType()));

            // [<fnty>*]
            Stream<String> argumentStream = Arrays.stream(decl.getArgumentTypes()).map(Type::toString);
            if (decl.isVarArg()) {
                argumentStream = Stream.concat(argumentStream, Stream.of("..."));
            }
            printVisitors.print(String.format(" (%s)*", argumentStream.collect(Collectors.joining(", "))));
        } else if (call.getCallTarget() instanceof FunctionParameter) {
            printVisitors.print(String.format(" %s", call.getCallTarget().getType()));
        } else {
            throw new AssertionError("unexpected target type");
        }

        // <fnptrval>(<function args>)
        printVisitors.print(" " + getSymbolName(call.getCallTarget()));

        List<Symbol> arguments = new ArrayList<>(call.getArgumentCount());
        for (int i = 0; i < call.getArgumentCount(); i++) {
            arguments.add(call.getArgument(i));
        }
        printVisitors.print(argsToString(arguments));

        // [fn attrs]
        // TODO: implement

        printVisitors.println();
    }

}
