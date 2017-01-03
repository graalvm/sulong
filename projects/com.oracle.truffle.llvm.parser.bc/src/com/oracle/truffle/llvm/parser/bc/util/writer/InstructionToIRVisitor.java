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
import com.oracle.truffle.llvm.parser.api.model.visitors.InstructionVisitor;

public final class InstructionToIRVisitor implements InstructionVisitor {

    private static final String INDENTATION = "    ";

    private static final char NEWLINE = '\n';

    private final StringBuilder builder;

    private InstructionToIRVisitor(StringBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void visit(AllocateInstruction allocate) {
        builder.append(INDENTATION).append(allocate.toString()).append(NEWLINE);

    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        builder.append(INDENTATION).append(operation.toString()).append(NEWLINE);
    }

    @Override
    public void visit(BranchInstruction branch) {
        builder.append(INDENTATION).append(branch.toString()).append(NEWLINE);
    }

    @Override
    public void visit(CallInstruction call) {
        builder.append(INDENTATION).append(call.toString()).append(NEWLINE);
    }

    @Override
    public void visit(CastInstruction cast) {
        builder.append(INDENTATION).append(cast.toString()).append(NEWLINE);
    }

    @Override
    public void visit(CompareInstruction operation) {
        builder.append(INDENTATION).append(operation.toString()).append(NEWLINE);
    }

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        builder.append(INDENTATION).append(branch.toString()).append(NEWLINE);
    }

    @Override
    public void visit(ExtractElementInstruction extract) {
        builder.append(INDENTATION).append(extract.toString()).append(NEWLINE);
    }

    @Override
    public void visit(ExtractValueInstruction extract) {
        builder.append(INDENTATION).append(extract.toString()).append(NEWLINE);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        builder.append(INDENTATION).append(gep.toString()).append(NEWLINE);
    }

    @Override
    public void visit(IndirectBranchInstruction branch) {
        builder.append(INDENTATION).append(branch.toString()).append(NEWLINE);
    }

    @Override
    public void visit(InsertElementInstruction insert) {
        builder.append(INDENTATION).append(insert.toString()).append(NEWLINE);
    }

    @Override
    public void visit(InsertValueInstruction insert) {
        builder.append(INDENTATION).append(insert.toString()).append(NEWLINE);
    }

    @Override
    public void visit(LoadInstruction load) {
        builder.append(INDENTATION).append(load.toString()).append(NEWLINE);
    }

    @Override
    public void visit(PhiInstruction phi) {
        builder.append(INDENTATION).append(phi.toString()).append(NEWLINE);
    }

    @Override
    public void visit(ReturnInstruction ret) {
        builder.append(INDENTATION).append(ret.toString()).append(NEWLINE);
    }

    @Override
    public void visit(SelectInstruction select) {
        builder.append(INDENTATION).append(select.toString()).append(NEWLINE);
    }

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        builder.append(INDENTATION).append(shuffle.toString()).append(NEWLINE);
    }

    @Override
    public void visit(StoreInstruction store) {
        builder.append(INDENTATION).append(store.toString()).append(NEWLINE);
    }

    @Override
    public void visit(SwitchInstruction select) {
        builder.append(INDENTATION).append(select.toString()).append(NEWLINE);
    }

    @Override
    public void visit(SwitchOldInstruction select) {
        builder.append(INDENTATION).append(select.toString()).append(NEWLINE);
    }

    @Override
    public void visit(UnreachableInstruction unreachable) {
        builder.append(INDENTATION).append(unreachable.toString()).append(NEWLINE);
    }

    @Override
    public void visit(VoidCallInstruction call) {
        builder.append(INDENTATION).append(call.toString()).append(NEWLINE);
    }

    static InstructionVisitor create(StringBuilder builder) {
        return new InstructionToIRVisitor(builder);
    }
}
