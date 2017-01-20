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
package com.oracle.truffle.llvm.parser.bc.irwriter;

import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.runtime.LLVMLogger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public final class LLVMIRPrinter {

    interface PrintTarget {

        void print(String s);

        default void println(String s) {
            print(s);
            println();
        }

        void println();

    }

    private static void printLLVM(Model model, String llvmVersion, PrintTarget printer) {
        LLVMPrintVersion version;

        if ("3.2".equals(llvmVersion)) {
            version = LLVMPrintVersion.LLVM_3_2;

        } else {
            LLVMLogger.info(String.format("No explicit LLVMIR-Printer for version %s, falling back to 3.2!", llvmVersion));
            version = LLVMPrintVersion.LLVM_3_2;
        }

        final LLVMPrintVersion.LLVMPrintVisitors visitors = version.createPrintVisitors(printer);
        model.accept(visitors.getModelVisitor());

    }

    public static void printLLVMToStream(Model model, String llvmVersion, PrintWriter targetWriter) {
        printLLVM(model, llvmVersion, new PrintTarget() {
            @Override
            public void print(String s) {
                targetWriter.print(s);
            }

            @Override
            public void println() {
                targetWriter.println();
                targetWriter.flush();
            }
        });
    }

    public static void printLLVMToFile(Model model, String llvmVersion, String filename) {
        PrintWriter fileWriter;
        try {
            fileWriter = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot print LLVMIR to this file: " + filename, e);
        }
        printLLVMToStream(model, llvmVersion, fileWriter);
    }

}
