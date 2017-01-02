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
package com.oracle.truffle.llvm.parser.api.model.symbols.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.truffle.llvm.parser.api.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.api.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.types.FunctionType;
import com.oracle.truffle.llvm.parser.api.model.types.PointerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class CallInstruction extends ValueInstruction implements Call {

    public static final String LLVMIR_LABEL = "call";

    private final Linkage linkage;

    private final Visibility visibility;

    private Symbol target;

    private final List<Symbol> arguments = new ArrayList<>();

    private CallInstruction(Type type, Linkage linkage, Visibility visibility) {
        super(type);
        this.linkage = linkage;
        this.visibility = visibility;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Symbol getArgument(int index) {
        return arguments.get(index);
    }

    @Override
    public int getArgumentCount() {
        return arguments.size();
    }

    @Override
    public Symbol getCallTarget() {
        return target;
    }

    @Override
    public Linkage getLinkage() {
        return linkage;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public void replace(Symbol original, Symbol replacement) {
        if (target == original) {
            target = replacement;
        }
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) == original) {
                arguments.set(i, replacement);
            }
        }
    }

    public static CallInstruction fromSymbols(Symbols symbols, Type type, int targetIndex, int[] arguments, long visibility, long linkage) {
        final CallInstruction inst = new CallInstruction(type, Linkage.decode(linkage), Visibility.decode(visibility));
        inst.target = symbols.getSymbol(targetIndex, inst);
        for (int argument : arguments) {
            inst.arguments.add(symbols.getSymbol(argument, inst));
        }
        return inst;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // <result> = [tail] call
        sb.append(String.format("%s = %s", getName(), LLVMIR_LABEL)); // TODO: [tail]

        // [cconv] [ret attrs]
        // TODO: implement

        if (target instanceof FunctionType) {
            // <ty>
            FunctionType decl = (FunctionType) target;
            sb.append(String.format(" %s", decl.getReturnType()));

            // [<fnty>*]
            Stream<String> argumentStream = Arrays.stream(decl.getArgumentTypes()).map(Type::toString);
            if (decl.isVarArg()) {
                argumentStream = Stream.concat(argumentStream, Stream.of("..."));
            }
            sb.append(String.format(" (%s)*", argumentStream.collect(Collectors.joining(", "))));
        } else if (target instanceof LoadInstruction) {
            Type targetType = ((LoadInstruction) target).getSource().getType();
            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }
            if (targetType instanceof FunctionType) {
                sb.append(String.format(" %s", ((FunctionType) targetType).getReturnType()));
            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }
        } else if (target instanceof FunctionParameter) {
            sb.append(String.format(" %s", target.getType()));
        } else {
            throw new AssertionError("unexpected target type: " + target.getClass().getName());
        }

        // <fnptrval>(<function args>)
        sb.append(" " + target.getName());
        sb.append('(');
        // @formatter:off
        sb.append(arguments.stream().map(s ->
            String.format("%s %s", s.getType(), s.getName())
        ).collect(Collectors.joining(", ")));
        // @formatter:on
        sb.append(')');

        // [fn attrs]
        // TODO: implement

        return sb.toString();
    }
}
