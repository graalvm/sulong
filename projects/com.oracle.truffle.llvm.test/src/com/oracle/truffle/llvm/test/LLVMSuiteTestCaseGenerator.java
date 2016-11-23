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
package com.oracle.truffle.llvm.test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.llvm.test.TestSuiteBase.TestCaseGenerator;
import com.oracle.truffle.llvm.test.spec.SpecificationEntry;
import com.oracle.truffle.llvm.tools.Clang;
import com.oracle.truffle.llvm.tools.ProgrammingLanguage;
import com.oracle.truffle.llvm.tools.util.PathUtil;

public class LLVMSuiteTestCaseGenerator implements TestCaseGenerator {

    private static final String LLVM_REFERENCE_OUTPUT_EXTENSION = ".reference_output";
    private boolean compileToLLVMIR;

    public LLVMSuiteTestCaseGenerator(boolean compileToLLVMIR) {
        this.compileToLLVMIR = compileToLLVMIR; // by default, compile to .bc files
    }

    @Override
    public List<TestCaseFile> getCompiledTestCaseFiles(SpecificationEntry toBeCompiled) {
        File toBeCompiledFile = toBeCompiled.getFile();
        String expectedOutputName = PathUtil.replaceExtension(toBeCompiledFile.getAbsolutePath(), LLVM_REFERENCE_OUTPUT_EXTENSION);
        File expectedOutputFile = new File(expectedOutputName);
        File dest;
        if (compileToLLVMIR) {
            dest = TestHelper.getTempLLFile(toBeCompiledFile, "_main");
        } else {
            dest = TestHelper.getTempBCFile(toBeCompiledFile, "_main");
        }

        try {
            TestCaseFile result = TestHelper.compileToLLVMIRWithClang(toBeCompiledFile, dest, expectedOutputFile, toBeCompiled.getFlags());
            return Arrays.asList(result);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public TestCaseFile getBitCodeTestCaseFiles(SpecificationEntry bitCode) {
        File bitCodeFile = bitCode.getFile();
        String expectedOutputName = PathUtil.replaceExtension(bitCodeFile.getAbsolutePath(), LLVM_REFERENCE_OUTPUT_EXTENSION);
        File expectedOutputFile = new File(expectedOutputName);
        TestCaseFile testCaseFiles;
        if (expectedOutputFile.exists()) {
            testCaseFiles = TestCaseFile.createFromBitCodeFile(bitCodeFile, expectedOutputFile, bitCode.getFlags());
        } else {
            testCaseFiles = TestCaseFile.createFromBitCodeFile(bitCodeFile, bitCode.getFlags());
        }
        return testCaseFiles;
    }

    @Override
    public ProgrammingLanguage[] getSupportedLanguages() {
        return Clang.getSupportedLanguages();
    }
}
