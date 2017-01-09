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
package com.oracle.truffle.llvm.writer;

import java.io.OutputStream;
import java.io.PrintWriter;
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
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
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
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public class InstructionV32PrintVisitor implements InstructionVisitor {

    private final PrintWriter out;

    public InstructionV32PrintVisitor(PrintWriter out) {
        this.out = out;
    }

    public InstructionV32PrintVisitor(OutputStream out) {
        this(new PrintWriter(out));
    }

    private static final String LLVMIR_LABEL_ALLOCATE = "alloca";

    @Override
    public void visit(AllocateInstruction allocate) {
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
        // <result> = <op>
        out.print(String.format("%s = %s", operation.getName(), operation.getOperator()));

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            out.print(" " + flag);
        }

        // <ty> <op1>, <op2>
        out.print(String.format(" %s %s, %s", operation.getType(),
                        ((ValueSymbol) operation.getLHS()).getName(),
                        ((ValueSymbol) operation.getRHS()).getName()));

        out.println();
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    @Override
    public void visit(BranchInstruction branch) {
        out.println(String.format("%s label %s", LLVMIR_LABEL_BRANCH, branch.getSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_CALL = "call";

    @Override
    public void visit(CallInstruction call) {
        // <result> = [tail] call
        out.print(String.format("%s = %s", call.getName(), LLVMIR_LABEL_CALL)); // TODO: [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            FunctionType decl = (FunctionType) call.getCallTarget();

            out.print(String.format(" %s", decl.getReturnType().toString()));

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                out.print(String.format(" %s*", decl)); // TODO: implement getTypeSignature()
            }
            out.print(String.format(" %s", decl.getName()));
        } else if (call.getCallTarget() instanceof LoadInstruction) {
            Type targetType = ((LoadInstruction) call.getCallTarget()).getSource().getType();
            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }
            if (targetType instanceof FunctionType) {
                out.print(String.format(" %s %s", ((FunctionType) targetType).getReturnType(), ((ValueSymbol) call.getCallTarget()).getName()));
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (call.getCallTarget() instanceof FunctionParameter) {
            out.print(String.format(" %s", call.getCallTarget().getType().toString()));

        } else if (call.getCallTarget() instanceof InlineAsmConstant) {
            out.print(String.format(" %s", call.getCallTarget().toString()));

        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        List<Symbol> arguments = new ArrayList<>(call.getArgumentCount());
        for (int i = 0; i < call.getArgumentCount(); i++) {
            arguments.add(call.getArgument(i));
        }
        out.print(argsToString(arguments));

        // [fn attrs]
        // TODO: implement
        out.println();
    }

    private static String argsToString(List<Symbol> arguments) {
        return arguments.stream().map(s -> {
            if (s instanceof Constant) {
                return s.toString();
            } else {
                return String.format("%s %s", s.getType().toString(), ((ValueSymbol) s).getName());
            }
        }).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public void visit(CastInstruction cast) {
        out.println(String.format("%s = %s %s %s to %s", cast.getName(), cast.getOperator(), cast.getValue().getType(), ((ValueSymbol) cast.getValue()).getName(), cast.getType()));
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareInstruction operation) {
        if (operation.getOperator().isFloatingPoint()) {
            // <result> = fcmp <cond> <ty> <op1>, <op2>
            out.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE_FP, operation.getOperator(), operation.getBaseType(),
                            ((ValueSymbol) operation.getLHS()).getName(), ((ValueSymbol) operation.getRHS()).getName()));
        } else {
            // <result> = icmp <cond> <ty> <op1>, <op2>
            out.println(String.format("%s = %s %s %s %s, %s", operation.getName(), LLVMIR_LABEL_COMPARE, operation.getOperator(), operation.getBaseType(),
                            ((ValueSymbol) operation.getLHS()).getName(), ((ValueSymbol) operation.getRHS()).getName()));
        }
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        // br i1 <cond>, label <iftrue>, label <iffalse>
        out.println(String.format("%s %s %s, label %s, label %s", LLVMIR_LABEL_CONDITIONAL_BRANCH,
                        branch.getCondition().getType(), ((ValueSymbol) branch.getCondition()).getName(),
                        branch.getTrueSuccessor().getName(),
                        branch.getFalseSuccessor().getName()));
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    @Override
    public void visit(ExtractElementInstruction extract) {
        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        out.println(String.format("%s = %s %s %s, %s %s", extract.getName(), LLVMIR_LABEL_EXTRACT_ELEMENT,
                        extract.getVector().getType(), ((ValueSymbol) extract.getVector()).getName(),
                        extract.getIndex().getType(), ((ValueSymbol) extract.getIndex()).getName()));
    }

    private static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    @Override
    public void visit(ExtractValueInstruction extract) {
        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        out.println(String.format("%s = %s %s %s, %d", extract.getName(), LLVMIR_LABEL_EXTRACT_VALUE,
                        extract.getAggregate().getType(), ((ValueSymbol) extract.getAggregate()).getName(),
                        extract.getIndex()));
    }

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    @Override
    public void visit(GetElementPointerInstruction gep) {
        // <result> = getelementptr
        out.print(String.format("%s = %s", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER));

        // [inbounds]
        if (gep.isInbounds()) {
            out.print(" inbounds");
        }

        // <pty>* <ptrval>
        out.print(String.format(" %s %s", gep.getBasePointer().getType(), ((ValueSymbol) gep.getBasePointer()).getName()));

        // {, <ty> <idx>}*
        for (Symbol sym : gep.getIndices()) {
            if (sym instanceof Constant) {
                out.print(String.format(", %s", sym.toString()));
            } else {
                out.print(String.format(", %s %s", sym.getType().toString(), ((ValueSymbol) sym).getName()));
            }
        }

        out.println();
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    @Override
    public void visit(IndirectBranchInstruction branch) {
        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        // @formatter:off
        out.println(String.format("%s %s %s, [ %s ]", LLVMIR_LABEL_INDIRECT_BRANCH,
                        branch.getAddress().getType(), ((ValueSymbol) branch.getAddress()).getName(),
                        branch.getSuccessors().stream().map(s ->
                             String.format("label %s", s.getName())
                        ).collect(Collectors.joining(", "))));
        // @formatter:on
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    @Override
    public void visit(InsertElementInstruction insert) {
        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", insert.getName(), LLVMIR_LABEL_INSERT_ELEMENT,
                        insert.getVector().getType(), ((ValueSymbol) insert.getVector()).getName(),
                        insert.getValue().getType(), ((ValueSymbol) insert.getValue()).getName(),
                        insert.getIndex().getType(), ((ValueSymbol) insert.getIndex()).getName()));
    }

    private static final String LLVMIR_LABEL_INSERT_VALUE = "insertvalue";

    @Override
    public void visit(InsertValueInstruction insert) {
        // <result> = insertvalue <aggregate type> <val>, <ty> <elt>, <idx>{, <idx>}*
        out.println(String.format("%s = %s %s %s, %s %s, %d", insert.getName(), LLVMIR_LABEL_INSERT_VALUE,
                        insert.getAggregate().getType(), ((ValueSymbol) insert.getAggregate()).getName(),
                        insert.getValue().getType(), ((ValueSymbol) insert.getValue()).getName(),
                        insert.getIndex()));
    }

    private static final String LLVMIR_LABEL_LOAD = "load";

    @Override
    public void visit(LoadInstruction load) {
        // <result> = load
        out.print(String.format("%s = %s", load.getName(), LLVMIR_LABEL_LOAD));

        if (load.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            // [volatile]
            if (load.isVolatile()) {
                out.print(" volatile");
            }

            // <ty>* <pointer>
            out.print(String.format(" %s %s", load.getSource().getType(), ((ValueSymbol) load.getSource()).getName()));

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
            out.print(String.format(" %s %s", load.getSource().getType(), ((ValueSymbol) load.getSource()).getName()));

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
        // <result> = phi <ty>
        out.print(String.format("%s = %s %s", phi.getName(), LLVMIR_LABEL_PHI, phi.getType()));

        // [ <val0>, <label0>], ...
        // @formatter:off
        out.print(IntStream.range(0, phi.getSize()).mapToObj(i ->
                            String.format(" [ %s, %s ]", ((ValueSymbol) phi.getValue(i)).getName(), phi.getBlock(i).getName())
                        ).collect(Collectors.joining(",")));
        // @formatter:on

        out.println();
    }

    private static final String LLVMIR_LABEL_RETURN = "ret";

    @Override
    public void visit(ReturnInstruction ret) {
        if (ret.getValue() == null) {
            // ret void
            out.println(String.format("%s void", LLVMIR_LABEL_RETURN));
        } else {
            // ret <type> <value>
            out.println(String.format("%s %s %s", LLVMIR_LABEL_RETURN, ret.getValue().getType(), ((ValueSymbol) ret.getValue()).getName()));
        }
    }

    private static final String LLVMIR_LABEL_SELECT = "select";

    @Override
    public void visit(SelectInstruction select) {
        // <result> = select selty <cond>, <ty> <val1>, <ty> <val2>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", select.getName(), LLVMIR_LABEL_SELECT,
                        select.getCondition().getType(), ((ValueSymbol) select.getCondition()).getName(),
                        select.getTrueValue().getType(), ((ValueSymbol) select.getTrueValue()).getName(),
                        select.getFalseValue().getType(), ((ValueSymbol) select.getFalseValue()).getName()));
    }

    private static final String LLVMIR_LABEL_SHUFFLE_VECTOR = "shufflevector";

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        // <result> = shufflevector <n x <ty>> <v1>, <n x <ty>> <v2>, <m x i32> <mask>
        out.println(String.format("%s = %s %s %s, %s %s, %s %s", shuffle.getName(), LLVMIR_LABEL_SHUFFLE_VECTOR,
                        shuffle.getVector1().getType(), ((ValueSymbol) shuffle.getVector1()).getName(),
                        shuffle.getVector2().getType(), ((ValueSymbol) shuffle.getVector2()).getName(),
                        shuffle.getMask().getType(), ((ValueSymbol) shuffle.getMask()).getName()));
    }

    private static final String LLVMIR_LABEL_STORE = "store";

    @Override
    public void visit(StoreInstruction store) {
        // <result> = load
        out.print(String.format("%s", LLVMIR_LABEL_STORE));

        if (store.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            // [volatile]
            if (store.isVolatile()) {
                out.print(" volatile");
            }

            // <ty> <value>, <ty>* <pointer>
            out.print(String.format(" %s %s, %s %s", ((PointerType) store.getDestination().getType()).getPointeeType(), ((ValueSymbol) store.getSource()).getName(),
                            store.getDestination().getType(), ((ValueSymbol) store.getDestination()).getName()));

            // [, align <alignment>]
            if (store.getAlign() != 0) {
                out.print(String.format(", align %d", 1 << (store.getAlign() - 1)));
            }

            // [, !nontemporal !<index>][, !invariant.load !<index>]
            // TODO: implement
        } else {
            // atomic
            out.print(" atomic");

            // [volatile]
            if (store.isVolatile()) {
                out.print(" volatile");
            }

            // <ty> <value>, <ty>* <pointer>
            out.print(String.format(" %s %s, %s %s", ((PointerType) store.getDestination().getType()).getPointeeType(), ((ValueSymbol) store.getSource()).getName(),
                            store.getDestination().getType(), ((ValueSymbol) store.getDestination()).getName()));

            // [singlethread]
            if (store.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                out.print(" singlethread");
            }

            // <ordering>, align <alignment>
            out.print(String.format(" %s, align %d", store.getAtomicOrdering(), 1 << (store.getAlign() - 1)));
        }

        out.println();
    }

    public static final String LLVMIR_LABEL_SWITCH = "switch";

    @Override
    public void visit(SwitchInstruction select) {
        // switch <intty> <value>, label <defaultdest>
        String mainStr = String.format("%s %s %s, label %s", LLVMIR_LABEL_SWITCH, select.getCondition().getType(), ((ValueSymbol) select.getCondition()).getName(), select.getDefaultBlock().getName());
        out.print(mainStr);
        // [ <intty> <val>, label <dest> ... ]
        out.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        out.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                        final Symbol val = select.getCaseValue(i);
                        final Symbol blk = select.getCaseBlock(i);
                        return String.format("%s %s, label %s", val.getType(), ((ValueSymbol) val).getName(), ((ValueSymbol) blk).getName());
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
        String mainStr = String.format("%s %s %s, label %s", LLVMIR_LABEL_SWITCH_OLD, select.getCondition().getType(), ((ValueSymbol) select.getCondition()).getName(),
                        select.getDefaultBlock().getName());
        out.print(mainStr);

        // [ <intty> <val>, label <dest> ... ]
        out.print(" [");
        // @formatter:off
        final String indent = buildIndent(mainStr.length() + 2);
        out.print(IntStream.range(0, select.getCaseCount()).mapToObj(i -> {
                    final long val = select.getCaseValue(i);
                    final Symbol blk = select.getCaseBlock(i);
                    return String.format("%s %d, label %s", select.getCondition().getType(), val, ((ValueSymbol) blk).getName());
                }).collect(Collectors.joining(indent)));
        // @formatter:on
        out.print("]");

        out.println();
    }

    private static final String LLVMIR_LABEL_UNREACHABLE = "unreachable";

    @Override
    public void visit(UnreachableInstruction unreachable) {
        out.println(LLVMIR_LABEL_UNREACHABLE);
    }

    private static final String LLVMIR_LABEL_VOID_CALL = "call";

    @Override
    public void visit(VoidCallInstruction call) {
        // [tail] call
        out.print(LLVMIR_LABEL_VOID_CALL); // TODO: [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (call.getCallTarget() instanceof FunctionType) {
            // <ty>
            FunctionType decl = (FunctionType) call.getCallTarget();
            out.print(String.format(" %s", decl.getReturnType()));

            // [<fnty>*]
            Stream<String> argumentStream = Arrays.stream(decl.getArgumentTypes()).map(Type::toString);
            if (decl.isVarArg()) {
                argumentStream = Stream.concat(argumentStream, Stream.of("..."));
            }
            out.print(String.format(" (%s)*", argumentStream.collect(Collectors.joining(", "))));
        } else if (call.getCallTarget() instanceof FunctionParameter) {
            out.print(String.format(" %s", call.getCallTarget().getType()));
        } else {
            throw new AssertionError("unexpected target type");
        }

        // <fnptrval>(<function args>)
        out.print(" " + ((ValueSymbol) call.getCallTarget()).getName());

        List<Symbol> arguments = new ArrayList<>(call.getArgumentCount());
        for (int i = 0; i < call.getArgumentCount(); i++) {
            arguments.add(call.getArgument(i));
        }
        out.print(argsToString(arguments));

        // [fn attrs]
        // TODO: implement

        out.println();
    }

}
