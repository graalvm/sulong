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
package com.oracle.truffle.llvm.parser.bc.util.writer;

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.symbols.ValueSymbol;
import com.oracle.truffle.llvm.parser.api.model.visitors.FunctionVisitor;
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

final class FunctionToIRVisitor implements FunctionVisitor {

    private static final char NEWLINE = '\n';

    private final StringBuilder builder;

    private final InstructionVisitor instructionVisitor;

    private FunctionToIRVisitor(StringBuilder builder) {
        this.builder = builder;
        this.instructionVisitor = InstructionToIRVisitor.create(builder);
    }

    private static final String LABEL_BEGIN = "; <label>:";

    @Override
    public void visit(InstructionBlock block) {
        if (!block.getName().equals(ValueSymbol.UNKNOWN)) {
            if (isNamedLabel(block.getName())) {
                builder.append(block.getName().substring(1)).append(":").append(NEWLINE);
            } else {
                builder.append(LABEL_BEGIN).append(block.getName().substring(1)).append(NEWLINE);
            }
        }
        block.accept(instructionVisitor);
        builder.append(NEWLINE);
    }

    static FunctionVisitor create(StringBuilder builder) {
        return new FunctionToIRVisitor(builder);
    }

    private static boolean isNamedLabel(String label) {
        return !label.substring(1).matches("^[0-9]+$");
    }
}
