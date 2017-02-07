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
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

class InstructionPrintVisitor implements InstructionVisitor {

    protected final LLVMPrintVersion.LLVMPrintVisitors visitors;

    protected final LLVMIRPrinter.PrintTarget out;

    InstructionPrintVisitor(LLVMPrintVersion.LLVMPrintVisitors visitors, LLVMIRPrinter.PrintTarget target) {
        this.visitors = visitors;
        this.out = target;
    }

    static final String INDENTATION = "    ";

    private static final String LLVMIR_LABEL_ALLOCATE = "alloca";

    static final String LLVMIR_LABEL_ALIGN = "align";

    @Override
    public void visit(AllocateInstruction allocate) {
        out.print(INDENTATION);
        // <result> = alloca <type>

        out.print(String.format("%s = %s ", allocate.getName(), LLVMIR_LABEL_ALLOCATE));
        allocate.getPointeeType().accept(visitors.getTypeVisitor());

        // [, <ty> <NumElements>]
        if (!(allocate.getCount() instanceof IntegerConstant && ((IntegerConstant) allocate.getCount()).getValue() == 1)) {
            out.print(", ");
            allocate.getCount().getType().accept(visitors.getTypeVisitor());
            out.print(String.format(" %s", allocate.getCount()));
        }

        // [, align <alignment>]
        if (allocate.getAlign() != 0) {
            out.print(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (allocate.getAlign() - 1)));
        }

        out.println();
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        out.print(INDENTATION);
        // <result> = <op>
        // sulong specific toString
        out.print(String.format("%s = %s ", operation.getName(), operation.getOperator()));

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            out.print(flag.toString());
            out.print(" ");
        }

        // <ty> <op1>, <op2>
        operation.getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(operation.getLHS());
        out.print(", ");
        visitors.getIRWriterUtil().printInnerSymbolValue(operation.getRHS());
        out.println();
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    private static final String LLVMIR_LABEL_BRANCH_LABEL = "label";

    @Override
    public void visit(BranchInstruction branch) {
        out.print(INDENTATION);
        out.println(String.format("%s %s %s", LLVMIR_LABEL_BRANCH, LLVMIR_LABEL_BRANCH_LABEL, branch.getSuccessor().getName()));
    }

    static final String LLVMIR_LABEL_CALL = "call";

    @Override
    public void visit(CallInstruction call) {
        out.print(INDENTATION);
        // <result> = [tail] call
        out.print(String.format("%s = ", call.getName()));

        printFunctionCall(call);
        out.println();
    }

    protected void printActualArgs(Call call) {
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
        // sulong specific toString
        out.print(String.format("%s = %s ", cast.getName(), cast.getOperator()));
        cast.getValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(cast.getValue());
        out.print(" to ");
        cast.getType().accept(visitors.getTypeVisitor());
        out.println();
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareInstruction operation) {
        out.print(INDENTATION);
        // <result> = <icmp|fcmp> <cond> <ty> <op1>, <op2>
        out.print(operation.getName());
        out.print(" = ");

        if (operation.getOperator().isFloatingPoint()) {
            out.print(LLVMIR_LABEL_COMPARE_FP);
        } else {
            out.print(LLVMIR_LABEL_COMPARE);
        }

        out.print(" ");
        out.print(operation.getOperator().toString()); // sulong specific toString
        out.print(" ");
        operation.getLHS().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(operation.getLHS());
        out.print(", ");
        visitors.getIRWriterUtil().printInnerSymbolValue(operation.getRHS());
        out.println();
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        out.print(INDENTATION);
        // br i1 <cond>, label <iftrue>, label <iffalse>
        out.print(LLVMIR_LABEL_CONDITIONAL_BRANCH);
        out.print(" ");
        branch.getCondition().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(branch.getCondition());
        out.print(", ");
        out.print(LLVMIR_LABEL_BRANCH_LABEL);
        out.print(" ");
        out.print(branch.getTrueSuccessor().getName());
        out.print(", ");
        out.print(LLVMIR_LABEL_BRANCH_LABEL);
        out.print(" ");
        out.println(branch.getFalseSuccessor().getName());
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    @Override
    public void visit(ExtractElementInstruction extract) {
        out.print(INDENTATION);
        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        out.print(extract.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_EXTRACT_ELEMENT);
        out.print(" ");
        extract.getVector().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(extract.getVector());
        out.print(", ");
        extract.getIndex().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(extract.getIndex());
        out.println();
    }

    private static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    @Override
    public void visit(ExtractValueInstruction extract) {
        out.print(INDENTATION);
        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        out.print(extract.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_EXTRACT_VALUE);
        out.print(" ");
        extract.getAggregate().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(extract.getAggregate());
        out.println(String.format(", %d", extract.getIndex()));
    }

    static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    static final String LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS = "inbounds";

    @Override
    public void visit(GetElementPointerInstruction gep) {
        out.print(INDENTATION);
        // <result> = getelementptr
        out.print(String.format("%s = %s ", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER));

        // [inbounds]
        if (gep.isInbounds()) {
            out.print(LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS);
            out.print(" ");
        }

        // <pty>* <ptrval>
        gep.getBasePointer().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(gep.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : gep.getIndices()) {
            out.print(", ");
            sym.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(sym);
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    @Override
    public void visit(IndirectBranchInstruction branch) {
        out.print(INDENTATION);
        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        out.print(LLVMIR_LABEL_INDIRECT_BRANCH);
        out.print(" ");
        branch.getAddress().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(branch.getAddress());

        // @formatter:off
        out.println(String.format(", [ %s ]",
                        branch.getSuccessors().stream().map(s ->
                             String.format("%s %s", LLVMIR_LABEL_BRANCH_LABEL, s.getName())
                        ).collect(Collectors.joining(", "))));
        // @formatter:on
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    @Override
    public void visit(InsertElementInstruction insert) {
        out.print(INDENTATION);
        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        out.print(insert.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_INSERT_ELEMENT);
        out.print(" ");
        insert.getVector().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(insert.getVector());
        out.print(", ");
        insert.getValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(insert.getValue());
        out.print(", ");
        insert.getIndex().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(insert.getIndex());
        out.println();
    }

    private static final String LLVMIR_LABEL_INSERT_VALUE = "insertvalue";

    @Override
    public void visit(InsertValueInstruction insert) {
        out.print(INDENTATION);
        // <result> = insertvalue <aggregate type> <val>, <ty> <elt>, <idx>{, <idx>}*
        out.print(insert.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_INSERT_VALUE);
        out.print(" ");
        insert.getAggregate().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(insert.getAggregate());
        out.print(", ");
        insert.getValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(insert.getValue());
        out.println(String.format(", %d", insert.getIndex()));
    }

    static final String LLVMIR_LABEL_LOAD = "load";

    static final String LLVMIR_LABEL_ATOMIC = "atomic";

    static final String LLVMIR_LABEL_VOLATILE = "volatile";

    static final String LLVMIR_LABEL_SINGLETHREAD = "singlethread";

    @Override
    public void visit(LoadInstruction load) {
        out.print(INDENTATION);
        out.print(String.format("%s = %s", load.getName(), LLVMIR_LABEL_LOAD));

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            out.print(" ");
            out.print(LLVMIR_LABEL_ATOMIC);
        }

        if (load.isVolatile()) {
            out.print(" ");
            out.print(LLVMIR_LABEL_VOLATILE);
        }

        out.print(" ");
        load.getSource().getType().accept(visitors.getTypeVisitor());

        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(load.getSource());

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                out.print(" ");
                out.print(LLVMIR_LABEL_SINGLETHREAD);
            }

            out.print(" ");
            out.print(load.getAtomicOrdering().toString());
        }

        if (load.getAlign() != 0) {
            out.print(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (load.getAlign() - 1)));
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_PHI = "phi";

    @Override
    public void visit(PhiInstruction phi) {
        out.print(INDENTATION);
        // <result> = phi <ty>
        out.print(String.format("%s = %s ", phi.getName(), LLVMIR_LABEL_PHI));
        phi.getType().accept(visitors.getTypeVisitor());
        out.print(" ");

        // [ <val0>, <label0>], ...
        for (int i = 0; i < phi.getSize(); i++) {
            if (i != 0) {
                out.print(", ");
            }

            out.print("[ ");
            visitors.getIRWriterUtil().printInnerSymbolValue(phi.getValue(i));
            out.print(", ");
            out.print(phi.getBlock(i).getName());
            out.print(" ]");
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_RETURN = "ret";

    @Override
    public void visit(ReturnInstruction ret) {
        out.print(INDENTATION);
        out.print(LLVMIR_LABEL_RETURN);
        out.print(" ");

        final Symbol value = ret.getValue();
        if (value == null) {
            MetaType.VOID.accept(visitors.getTypeVisitor());

        } else {
            value.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(value);
        }
        out.println();
    }

    private static final String LLVMIR_LABEL_SELECT = "select";

    @Override
    public void visit(SelectInstruction select) {
        out.print(INDENTATION);
        // <result> = select selty <cond>, <ty> <val1>, <ty> <val2>
        out.print(select.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_SELECT);
        out.print(" ");
        select.getCondition().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(select.getCondition());
        out.print(", ");
        select.getTrueValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(select.getTrueValue());
        out.print(", ");
        select.getFalseValue().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(select.getFalseValue());
        out.println();
    }

    private static final String LLVMIR_LABEL_SHUFFLE_VECTOR = "shufflevector";

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        out.print(INDENTATION);
        // <result> = shufflevector <n x <ty>> <v1>, <n x <ty>> <v2>, <m x i32> <mask>
        out.print(shuffle.getName());
        out.print(" = ");
        out.print(LLVMIR_LABEL_SHUFFLE_VECTOR);
        out.print(" ");
        shuffle.getVector1().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(shuffle.getVector1());
        out.print(", ");
        shuffle.getVector2().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(shuffle.getVector2());
        out.print(", ");
        shuffle.getMask().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(shuffle.getMask());
        out.println();
    }

    private static final String LLVMIR_LABEL_STORE = "store";

    @Override
    public void visit(StoreInstruction store) {
        out.print(INDENTATION);

        out.print(String.format("%s ", LLVMIR_LABEL_STORE));

        if (store.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            out.print(LLVMIR_LABEL_ATOMIC);
            out.print(" ");
        }

        if (store.isVolatile()) {
            out.print(LLVMIR_LABEL_VOLATILE);
            out.print(" ");
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
                out.print(" ");
                out.print(LLVMIR_LABEL_SINGLETHREAD);
            }

            out.print(" ");
            out.print(store.getAtomicOrdering().toString()); // sulong specific toString
        }

        if (store.getAlign() != 0) {
            out.println(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (store.getAlign() - 1)));
        }
    }

    private static final String LLVMIR_LABEL_SWITCH = "switch";

    @Override
    public void visit(SwitchInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        out.print(INDENTATION);

        out.print(LLVMIR_LABEL_SWITCH);
        out.print(" ");
        select.getCondition().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(select.getCondition());
        out.print(", ");
        out.print(LLVMIR_LABEL_BRANCH_LABEL);
        out.print(" ");
        out.print(select.getDefaultBlock().getName());

        out.print(" [ ");
        for (int i = 0; i < select.getCaseCount(); i++) {
            if (i != 0) {
                out.println();
                out.print(INDENTATION);
                out.print(INDENTATION);
            }

            final Symbol val = select.getCaseValue(i);
            final Symbol blk = select.getCaseBlock(i);
            val.getType().accept(visitors.getTypeVisitor());
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(val);
            out.print(", ");
            out.print(LLVMIR_LABEL_BRANCH_LABEL);
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(blk);
        }
        out.println(" ]");
    }

    private static final String LLVMIR_LABEL_SWITCH_OLD = "switch";

    @Override
    public void visit(SwitchOldInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        out.print(INDENTATION);

        out.print(LLVMIR_LABEL_SWITCH_OLD);
        out.print(" ");
        select.getCondition().getType().accept(visitors.getTypeVisitor());
        out.print(" ");
        visitors.getIRWriterUtil().printInnerSymbolValue(select.getCondition());
        out.print(", ");
        out.print(LLVMIR_LABEL_BRANCH_LABEL);
        out.print(" ");
        out.print(select.getDefaultBlock().getName());

        out.print(" [ ");
        for (int i = 0; i < select.getCaseCount(); i++) {
            if (i != 0) {
                out.println();
                out.print(INDENTATION);
                out.print(INDENTATION);
            }

            select.getCondition().getType().accept(visitors.getTypeVisitor());
            out.print(String.format(" %d, ", select.getCaseValue(i)));
            out.print(LLVMIR_LABEL_BRANCH_LABEL);
            out.print(" ");
            visitors.getIRWriterUtil().printInnerSymbolValue(select.getCaseBlock(i));
        }
        out.println(" ]");
    }

    private static final String LLVMIR_LABEL_UNREACHABLE = "unreachable";

    @Override
    public void visit(UnreachableInstruction unreachable) {
        out.print(INDENTATION);
        out.println(LLVMIR_LABEL_UNREACHABLE);
    }

    @Override
    public void visit(VoidCallInstruction call) {
        out.print(INDENTATION);
        printFunctionCall(call);
        out.println();
    }

    protected void printFunctionCall(Call call) {
        out.print(LLVMIR_LABEL_CALL);
        out.print(" ");
        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            final FunctionType decl = (FunctionType) call.getCallTarget();

            decl.getReturnType().accept(visitors.getTypeVisitor());

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {

                out.print(" ");
                visitors.getTypeVisitor().printFormalArguments(decl);
                out.print("*");
            }
            out.print(String.format(" %s", decl.getName()));

        } else if (call.getCallTarget() instanceof CallInstruction) {
            final FunctionType decl = ((CallInstruction) call.getCallTarget()).getCallType();

            decl.getReturnType().accept(visitors.getTypeVisitor());

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {

                out.print(" ");
                visitors.getTypeVisitor().printFormalArguments(decl);
                out.print("*");
            }
            out.print(String.format(" %s", decl.getName()));

        } else if (call.getCallTarget() instanceof FunctionParameter) {
            call.getCallTarget().getType().accept(visitors.getTypeVisitor());
            out.print(String.format(" %s ", ((FunctionParameter) call.getCallTarget()).getName()));

        } else if (call.getCallTarget() instanceof ValueSymbol) {
            Type targetType;
            if (call.getCallTarget() instanceof LoadInstruction) {
                targetType = ((LoadInstruction) call.getCallTarget()).getSource().getType();
            } else {
                targetType = ((ValueSymbol) call.getCallTarget()).getType();
            }

            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }

            if (targetType instanceof FunctionType) {
                ((FunctionType) targetType).getReturnType().accept(visitors.getTypeVisitor());
                out.print(" ");
                visitors.getIRWriterUtil().printInnerSymbolValue(call.getCallTarget());

            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (call.getCallTarget() instanceof Constant) {
            ((Constant) call.getCallTarget()).accept(visitors.getConstantVisitor());
        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        printActualArgs(call);
    }

}
