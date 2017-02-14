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

import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

final class IRWriterUtil {

    private final LLVMPrintVersion.LLVMPrintVisitors printVisitors;

    private final LLVMIRPrinter.PrintTarget out;

    IRWriterUtil(LLVMPrintVersion.LLVMPrintVisitors printVisitors, LLVMIRPrinter.PrintTarget target) {
        this.printVisitors = printVisitors;
        this.out = target;
    }

    void printInnerSymbolValue(Symbol symbol) {
        if (symbol instanceof ValueSymbol) {
            out.print(((ValueSymbol) symbol).getName());

        } else if (symbol instanceof Constant) {
            ((Constant) symbol).accept(printVisitors.getConstantVisitor());

        } else {
            throw new IllegalStateException("Cannot print this value: " + symbol);
        }
    }

    void printBlockName(InstructionBlock block) {
        final String name = block.getName();
        if (LLVMIdentifier.isImplicitBlockName(name)) {
            out.print("%");
            out.print(LLVMIdentifier.extractLabelFromImplicitBlockName(name));
        } else {
            out.print(name);
        }
    }
}
