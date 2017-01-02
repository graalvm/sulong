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
package com.oracle.truffle.llvm.parser.api.model.functions;

import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.types.FunctionType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FunctionDeclaration extends FunctionType implements Constant {

    public FunctionDeclaration(FunctionType type) {
        super(type.getReturnType(), type.getArgumentTypes(), type.isVarArg());
        if (type.getName().startsWith("@")) {
            setName(type.getName().substring(1));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FunctionDeclaration && super.equals(obj);
    }

    @Override
    public String getStringValue() {
        Stream<String> argumentStream = Arrays.stream(getArgumentTypes()).map(Type::toString);
        if (isVarArg()) {
            argumentStream = Stream.concat(argumentStream, Stream.of("..."));
        }
        return String.format("declare %s %s(%s)", getReturnType().toString(), getName(),
                        argumentStream.collect(Collectors.joining(", ")));
    }

    @Override
    public String toString() {
        return getStringValue();
    }
}
