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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.oracle.truffle.llvm.parser.api.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.api.model.enums.Flag;
import com.oracle.truffle.llvm.parser.api.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Call;
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

final class InstructionV32PrintVisitor implements InstructionVisitor {

    private final LLVMPrintVersion.LLVMPrintVisitors visitors;

    private final LLVMIRPrinter.PrintTarget out;

    InstructionV32PrintVisitor(LLVMPrintVersion.LLVMPrintVisitors visitors, LLVMIRPrinter.PrintTarget target) {
        this.visitors = visitors;
        this.out = target;
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
        out.print(INDENTATION);
        // <result> = alloca <type>
        out.print(String.format("%s = %s %s", allocate.getName(), LLVMIR_LABEL_ALLOCATE, allocate.getPointeeType()));

        // [, <ty> <NumElements>]
        if (!(allocate.getCount() instanceof IntegerConstant && ((IntegerConstant) allocate.getCount()).getValue() == 1)) {
            out.print(String.format(", %s %s", allocate.getCount().getType(), allocate.getCount()));
        }

        // [, align <alignment>]
        if (allocate.getAlign() != 0) {
            out.print(String.format(", align %d", 1 << (allocate.getAlign() - 1)));
        }

        out.println();
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        out.print(INDENTATION);
        // <result> = <op>
        out.print(String.format("%s = %s", operation.getName(), operation.getOperator()));

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            out.print(" " + flag);
        }

        // <ty> <op1>, <op2>
        out.print(String.format(" %s %s, %s", operation.getType(),
                        getSymbolName(operation.getLHS()),
                        getSymbolName(operation.getRHS())));

        out.println();
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    @Override
    public void visit(BranchInstruction branch) {
        out.print(INDENTATION);
        out.println(String.format("%s label %s", LLVMIR_LABEL_BRANCH, branch.getSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_CALL = "call";

    @Override
    public void visit(CallInstruction call) {
        out.print(INDENTATION);
        // <result> = [tail] call
        out.print(String.format("%s = %s", call.getName(), LLVMIR_LABEL_CALL)); // TODO:
                                                                                // [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            final FunctionType decl = (FunctionType) call.getCallTarget();

            out.print(" ");
            decl.getReturnType().accept(visitors.getTypeVisitor());

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                out.print(" (");
                final Type[] argTypes = decl.getArgumentTypes();
                for (int i = 0; i < argTypes.length; i++) {
                    if (i != 0) {
                        out.print(", ");
                    }
                    argTypes[i].accept(visitors.getTypeVisitor());
                }
                if (decl.isVarArg()) {
                    if (argTypes.length != 0) {
                        out.print(", ");
                    }
                    out.print("...");
                }
                out.print(")*");
            }
            out.print(String.format(" %s", decl.getName()));

        } else if (call.getCallTarget() instanceof LoadInstruction) {
            Type targetType = ((LoadInstruction) call.getCallTarget()).getSource().getType();
            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }
            if (targetType instanceof FunctionType) {
                out.print(String.format(" %s %s ", ((FunctionType) targetType).getReturnType(), getSymbolName(call.getCallTarget())));
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }

        } else if (call.getCallTarget() instanceof FunctionParameter) {
            out.print(String.format(" %s ", call.getCallTarget().getType().toString()));

        } else if (call.getCallTarget() instanceof InlineAsmConstant) {
            out.print(" ");
            ((InlineAsmConstant) call.getCallTarget()).accept(visitors.getConstantVisitor());

        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        printActualArgs(call);

        // [fn attrs]
        // TODO: implement
        out.println();
    }

    private void printActualArgs(Call call) {
        out.print("(");
        for (int i = 0; i < call.getArgumentCount(); i++) {
            final Symbol arg = call.getArgument(i);

            if (i != 0) {
                out.print(", ");
            }

            arg.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(arg);
        }
        out.print(")");
    }

    @Override
    public void visit(CastInstruction cast) {
        out.print(INDENTATION);
        out.println(String.format("%s = %s %s %s to %s", cast.getName(), cast.getOperator(), cast.getValue().getType(), getSymbolName(cast.getValue()), cast.getType()));
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareInstruction operation) {
        out.print(INDENTATION);
        if (operation.getOperator().isFloatingPoint()) {
            // <result> = fcmp <cond> <ty> <op1>, <op2>
            out.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE_FP, operation.getOperator(), operation.getBaseType(),
                            getSymbolName(operation.getLHS()), getSymbolName(operation.getRHS())));
        } else {
            // <result> = icmp <cond> <ty> <op1>, <op2>
            out.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE, operation.getOperator(), operation.getBaseType(),
                            getSymbolName(operation.getLHS()), getSymbolName(operation.getRHS())));
        }
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        out.print(INDENTATION);
        // br i1 <cond>, label <iftrue>, label <iffalse>
        out.println(String.format("%s %s %s, label %s, label %s", LLVMIR_LABEL_CONDITIONAL_BRANCH,
                        branch.getCondition().getType(), getSymbolName(branch.getCondition()),
                        branch.getTrueSuccessor().getName(),
                        branch.getFalseSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    @Override
    public void visit(ExtractElementInstruction extract) {
        out.print(INDENTATION);
        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        out.println(String.format("%s = %s %s %s, %s %s", extract.getName(), LLVMIR_LABEL_EXTRACT_ELEMENT,
                        extract.getVector().getType(), getSymbolName(extract.getVector()),
                        extract.getIndex().getType(), getSymbolName(extract.getIndex())));
    }

    private static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    @Override
    public void visit(ExtractValueInstruction extract) {
        out.print(INDENTATION);
        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        out.println(String.format("%s = %s %s %s, %d", extract.getName(), LLVMIR_LABEL_EXTRACT_VALUE,
                        extract.getAggregate().getType(), getSymbolName(extract.getAggregate()),
                        extract.getIndex()));
    }

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    @Override
    public void visit(GetElementPointerInstruction gep) {
        out.print(INDENTATION);
        // <result> = getelementptr
        out.print(String.format("%s = %s", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER));

        // [inbounds]
        if (gep.isInbounds()) {
            out.print(" inbounds");
        }

        // <pty>* <ptrval>
        out.print(String.format(" %s %s", gep.getBasePointer().getType(), getSymbolName(gep.getBasePointer())));

        // {, <ty> <idx>}*
        for (Symbol sym : gep.getIndices()) {
            if (sym instanceof Constant) {
                out.print(String.format(", %s", sym.toString()));
            } else {
                out.print(String.format(", %s %s", sym.getType().toString(), getSymbolName(sym)));
            }
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    @Override
    public void visit(IndirectBranchInstruction branch) {
        out.print(INDENTATION);
        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        // @formatter:off
        out.println(String.format("%s %s %s, [ %s ]", LLVMIR_LABEL_INDIRECT_BRANCH,
                        branch.getAddress().getType(), getSymbolName(branch.getAddress()),
                        branch.getSuccessors().stream().map(s ->
                             String.format("label %s", s.getName())
                        ).collect(Collectors.joining(", "))));
        // @formatter:on
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    @Override
    public void visit(InsertElementInstruction insert) {
        out.print(INDENTATION);
        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", insert.getName(), LLVMIR_LABEL_INSERT_ELEMENT,
                        insert.getVector().getType(), getSymbolName(insert.getVector()),
                        insert.getValue().getType(), getSymbolName(insert.getValue()),
                        insert.getIndex().getType(), getSymbolName(insert.getIndex())));
    }

    private static final String LLVMIR_LABEL_INSERT_VALUE = "insertvalue";

    @Override
    public void visit(InsertValueInstruction insert) {
        out.print(INDENTATION);
        // <result> = insertvalue <aggregate type> <val>, <ty> <elt>, <idx>{, <idx>}*
        out.println(String.format("%s = %s %s %s, %s %s, %d", insert.getName(), LLVMIR_LABEL_INSERT_VALUE,
                        insert.getAggregate().getType(), getSymbolName(insert.getAggregate()),
                        insert.getValue().getType(), getSymbolName(insert.getValue()),
                        insert.getIndex()));
    }

    private static final String LLVMIR_LABEL_LOAD = "load";

    @Override
    public void visit(LoadInstruction load) {
        out.print(INDENTATION);
        // <result> = load
        out.print(String.format("%s = %s", load.getName(), LLVMIR_LABEL_LOAD));

        if (load.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            // [volatile]
            if (load.isVolatile()) {
                out.print(" volatile");
            }

            // <ty>* <pointer>
            out.print(String.format(" %s %s", load.getSource().getType(), getSymbolName(load.getSource())));

            // [, align <alignment>]
            if (load.getAlign() != 0) {
                out.print(String.format(", align %d", 1 << (load.getAlign() - 1)));
            }

            // [, !nontemporal !<index>][, !invariant.load !<index>]
            // TODO: implement
        } else {
            // atomic
            out.print(" atomic");

            // [volatile]
            if (load.isVolatile()) {
                out.print(" volatile");
            }

            // <ty>* <pointer>
            out.print(String.format(" %s %s", load.getSource().getType(), getSymbolName(load.getSource())));

            // [singlethread]
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                out.print(" singlethread");
            }

            // <ordering>, align <alignment>
            out.print(String.format(" %s, align %d", load.getAtomicOrdering(), 1 << (load.getAlign() - 1)));
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_PHI = "phi";

    @Override
    public void visit(PhiInstruction phi) {
        out.print(INDENTATION);
        // <result> = phi <ty>
        out.print(String.format("%s = %s %s", phi.getName(), LLVMIR_LABEL_PHI, phi.getType()));

        // [ <val0>, <label0>], ...
        // @formatter:off
        out.print(IntStream.range(0, phi.getSize()).mapToObj(i ->
                            String.format(" [ %s, %s ]", getSymbolName(phi.getValue(i)), phi.getBlock(i).getName())
                        ).collect(Collectors.joining(",")));
        // @formatter:on

        out.println();
    }

    private static final String LLVMIR_LABEL_RETURN = "ret";

    @Override
    public void visit(ReturnInstruction ret) {
        out.print(INDENTATION);
        if (ret.getValue() == null) {
            // ret void
            out.println(String.format("%s void", LLVMIR_LABEL_RETURN));
        } else {
            // ret <type> <value>
            out.println(String.format("%s %s %s", LLVMIR_LABEL_RETURN, ret.getValue().getType(), getSymbolName(ret.getValue())));
        }
    }

    private static final String LLVMIR_LABEL_SELECT = "select";

    @Override
    public void visit(SelectInstruction select) {
        out.print(INDENTATION);
        // <result> = select selty <cond>, <ty> <val1>, <ty> <val2>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", select.getName(), LLVMIR_LABEL_SELECT,
                        select.getCondition().getType(), getSymbolName(select.getCondition()),
                        select.getTrueValue().getType(), getSymbolName(select.getTrueValue()),
                        select.getFalseValue().getType(), getSymbolName(select.getFalseValue())));
    }

    private static final String LLVMIR_LABEL_SHUFFLE_VECTOR = "shufflevector";

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        out.print(INDENTATION);
        // <result> = shufflevector <n x <ty>> <v1>, <n x <ty>> <v2>, <m x i32> <mask>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", shuffle.getName(), LLVMIR_LABEL_SHUFFLE_VECTOR,
                        shuffle.getVector1().getType(), getSymbolName(shuffle.getVector1()),
                        shuffle.getVector2().getType(), getSymbolName(shuffle.getVector2()),
                        shuffle.getMask().getType(), getSymbolName(shuffle.getMask())));
    }

    private static final String LLVMIR_LABEL_STORE = "store";

    @Override
    public void visit(StoreInstruction store) {
        out.print(INDENTATION);

        out.print(String.format("%s ", LLVMIR_LABEL_STORE));

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            out.print("atomic ");
        }

        if (store.isVolatile()) {
            out.print("volatile ");
        }

        ((PointerType) store.getDestination().getType()).getPointeeType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(store.getSource());
        out.print(", ");
        store.getDestination().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(store.getDestination());

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (store.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                out.print(" singlethread ");
            }

            out.print(store.getAtomicOrdering().toString());
        }

        out.println(String.format(", align %d", 1 << (store.getAlign() - 1)));
    }

    public static final String LLVMIR_LABEL_SWITCH = "switch";

    @Override
    public void visit(SwitchInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        String mainStr = INDENTATION;
        mainStr += String.format("%s %s %s, label %s", LLVMIR_LABEL_SWITCH, select.getCondition().getType(), getSymbolName(select.getCondition()), select.getDefaultBlock().getName());
        out.print(mainStr);
        // [ <intty> <val>, label <dest> ... ]
        out.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        out.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                        final Symbol val = select.getCaseValue(i);
                        final Symbol blk = select.getCaseBlock(i);
                        return String.format("%s %s, label %s", val.getType(), getSymbolName(val), getSymbolName(blk));
                    }).collect(Collectors.joining(indent)));
        // @formatter:on
        out.print("]");

        out.println();
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
        out.print(mainStr);

        // [ <intty> <val>, label <dest> ... ]
        out.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        out.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                    final long val = select.getCaseValue(i);
                    final Symbol blk = select.getCaseBlock(i);
                    return String.format("%s %d, label %s", select.getCondition().getType(), val, getSymbolName(blk));
                }).collect(Collectors.joining(indent)));
        // @formatter:on
        out.print("]");

        out.println();
    }

    private static final String LLVMIR_LABEL_UNREACHABLE = "unreachable";

    @Override
    public void visit(UnreachableInstruction unreachable) {
        out.print(INDENTATION);
        out.println(LLVMIR_LABEL_UNREACHABLE);
    }

    private static final String LLVMIR_LABEL_VOID_CALL = "call";

    @Override
    public void visit(VoidCallInstruction call) {
        out.print(INDENTATION);
        // [tail] call
        out.print(LLVMIR_LABEL_VOID_CALL); // TODO: [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            final FunctionType decl = (FunctionType) call.getCallTarget();

            out.print(" ");
            decl.getReturnType().accept(visitors.getTypeVisitor());
            out.print(" ");

            out.print(decl.getName());

        } else if (call.getCallTarget() instanceof FunctionParameter) {
            out.print(String.format(" %s", call.getCallTarget().getType()));

        } else {
            throw new AssertionError("unexpected target type");
        }

        printActualArgs(call);
        out.println();

        // [fn attrs]
        // TODO: implement
    }

}
