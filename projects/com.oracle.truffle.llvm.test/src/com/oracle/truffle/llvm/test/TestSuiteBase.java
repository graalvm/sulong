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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.oracle.truffle.llvm.LLVM;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.LLVMParserException;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException.UnsupportedReason;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.test.options.SulongTestOptions;
import com.oracle.truffle.llvm.test.spec.SpecificationEntry;
import com.oracle.truffle.llvm.test.spec.SpecificationFileReader;
import com.oracle.truffle.llvm.test.spec.TestSpecification;
import com.oracle.truffle.llvm.tools.Clang.ClangOptions;
import com.oracle.truffle.llvm.tools.Clang.ClangOptions.OptimizationLevel;
import com.oracle.truffle.llvm.tools.GCC;
import com.oracle.truffle.llvm.tools.Opt;
import com.oracle.truffle.llvm.tools.Opt.OptOptions;
import com.oracle.truffle.llvm.tools.Opt.OptOptions.Pass;
import com.oracle.truffle.llvm.tools.ProgrammingLanguage;

public abstract class TestSuiteBase {

    private static List<File> failingTests;
    private static List<File> succeedingTests;
    private static List<File> parserErrorTests;
    private static Map<UnsupportedReason, List<File>> unsupportedErrorTests;
    private static final int UNSIGNED_BYTE_MAX_VALUE = 0xff;

    protected void recordTestCase(TestCaseFile tuple, boolean pass) {
        if (pass) {
            if (!succeedingTests.contains(tuple.getOriginalFile()) && !failingTests.contains(tuple.getOriginalFile())) {
                succeedingTests.add(tuple.getOriginalFile());
            }
        } else {
            if (!failingTests.contains(tuple.getOriginalFile())) {
                failingTests.add(tuple.getOriginalFile());
            }
        }
    }

    protected void recordError(TestCaseFile tuple, Throwable error) {
        Throwable currentError = error;
        if (!failingTests.contains(tuple.getOriginalFile())) {
            failingTests.add(tuple.getOriginalFile());
        }
        while (currentError != null) {
            if (currentError instanceof LLVMParserException) {
                if (!parserErrorTests.contains(tuple.getOriginalFile())) {
                    parserErrorTests.add(tuple.getOriginalFile());
                }
                break;
            } else if (currentError instanceof LLVMUnsupportedException) {
                List<File> list = unsupportedErrorTests.get(((LLVMUnsupportedException) currentError).getReason());
                if (!list.contains(tuple.getOriginalFile())) {
                    list.add(tuple.getOriginalFile());
                }
                break;
            }
            currentError = currentError.getCause();
        }
    }

    private static final int LIST_MIN_SIZE = 1000;

    @BeforeClass
    public static void beforeClass() {
        succeedingTests = new ArrayList<>(LIST_MIN_SIZE);
        failingTests = new ArrayList<>(LIST_MIN_SIZE);
        parserErrorTests = new ArrayList<>(LIST_MIN_SIZE);
        unsupportedErrorTests = new HashMap<>(LIST_MIN_SIZE);
        for (UnsupportedReason reason : UnsupportedReason.values()) {
            unsupportedErrorTests.put(reason, new ArrayList<>(LIST_MIN_SIZE));
        }
    }

    protected static void printList(String header, List<File> files) {
        if (files.size() != 0) {
            LLVMLogger.info(header + " (" + files.size() + "):");
            files.stream().forEach(t -> LLVMLogger.info(t.toString()));
        }
    }

    @After
    public void displaySummary() {
        if (LLVMOptions.DEBUG.debug()) {
            if (SulongTestOptions.TEST.testDiscoveryPath() != null) {
                printList("succeeding tests:", succeedingTests);
            } else {
                printList("failing tests:", failingTests);
            }
            printList("parser error tests", parserErrorTests);
            for (UnsupportedReason reason : UnsupportedReason.values()) {
                printList("unsupported test " + reason, unsupportedErrorTests.get(reason));
            }
        }
    }

    @AfterClass
    public static void displayEndSummary() {
        if (SulongTestOptions.TEST.testDiscoveryPath() == null) {
            printList("failing tests:", failingTests);
        }
    }

    public interface TestCaseGenerator {

        ProgrammingLanguage[] getSupportedLanguages();

        TestCaseFile getBitCodeTestCaseFiles(SpecificationEntry bitCodeFile);

        List<TestCaseFile> getCompiledTestCaseFiles(SpecificationEntry toBeCompiled);
    }

    public static class TestCaseGeneratorAdapter implements TestCaseGenerator {

        @Override
        public ProgrammingLanguage[] getSupportedLanguages() {
            return new ProgrammingLanguage[0];
        }

        @Override
        public TestCaseFile getBitCodeTestCaseFiles(SpecificationEntry bitCodeFile) {
            return null;
        }

        @Override
        public List<TestCaseFile> getCompiledTestCaseFiles(SpecificationEntry toBeCompiled) {
            return Collections.emptyList();
        }

    }

    public static class TestCaseGeneratorImpl implements TestCaseGenerator {

        private boolean withOptimizations;
        private boolean compileToLLVMIR; // by default, compile to .bc files

        public TestCaseGeneratorImpl(boolean withOptimizations, boolean compileToLLLVMIR) {
            this.withOptimizations = withOptimizations;
            this.compileToLLVMIR = compileToLLLVMIR;
        }

        public TestCaseGeneratorImpl(boolean isLLFileTestGenerator) {
            withOptimizations = true;
            this.compileToLLVMIR = isLLFileTestGenerator;
        }

        @Override
        public TestCaseFile getBitCodeTestCaseFiles(SpecificationEntry bitCodeFile) {
            return TestCaseFile.createFromBitCodeFile(bitCodeFile.getFile(), bitCodeFile.getFlags());
        }

        /**
         * Compile File with given specifications
         */
        @Override
        public List<TestCaseFile> getCompiledTestCaseFiles(SpecificationEntry toBeCompiled) {
            List<TestCaseFile> files = new ArrayList<>();
            File sourceFile = toBeCompiled.getFile();
            final File dest;
            if (compileToLLVMIR) {
                dest = TestHelper.getTempLLFile(sourceFile, "_main");
            } else {
                dest = TestHelper.getTempBCFile(sourceFile, "_main");
            }
            try {
                final TestCaseFile compiledFile;
                if (ProgrammingLanguage.FORTRAN.isFile(sourceFile)) {
                    compiledFile = getCompiledFortranFile(sourceFile, dest, toBeCompiled.getFlags());
                } else if (ProgrammingLanguage.C_PLUS_PLUS.isFile(sourceFile)) {
                    compiledFile = getCompiledCppFile(sourceFile, dest, toBeCompiled.getFlags());
                } else if (ProgrammingLanguage.C.isFile(sourceFile)) {
                    compiledFile = getCompiledCFile(sourceFile, dest, toBeCompiled.getFlags());
                } else {
                    throw new RuntimeException("unknow programming language of file: " + sourceFile);
                }
                files.add(compiledFile);
                if (withOptimizations) {
                    files.add(getOptimizedTestCase(compiledFile));
                }
            } catch (Exception e) {
                LLVMLogger.error("Exception thrown: " + e.getMessage());
                return Collections.emptyList();
            }
            return files;
        }

        private static TestCaseFile getCompiledFortranFile(File sourceFile, File dest, Set<TestCaseFlag> flags) {
            return TestHelper.compileToLLVMIRWithGCC(sourceFile, dest, flags);
        }

        private static TestCaseFile getCompiledCppFile(File sourceFile, File dest, Set<TestCaseFlag> flags) {
            ClangOptions builder = ClangOptions.builder().optimizationLevel(OptimizationLevel.NONE);
            OptOptions options = OptOptions.builder().pass(Pass.LOWER_INVOKE).pass(Pass.PRUNE_EH).pass(Pass.SIMPLIFY_CFG);
            TestCaseFile compiledFiles = TestHelper.compileToLLVMIRWithClang(sourceFile, dest, flags, builder);
            return optimize(compiledFiles, options, "opt");
        }

        private static TestCaseFile getCompiledCFile(File sourceFile, File dest, Set<TestCaseFlag> flags) {
            ClangOptions builder = ClangOptions.builder().optimizationLevel(OptimizationLevel.NONE);
            return TestHelper.compileToLLVMIRWithClang(sourceFile, dest, flags, builder);
        }

        private static TestCaseFile getOptimizedTestCase(TestCaseFile compiledFiles) {
            OptOptions options = OptOptions.builder().pass(Pass.MEM_TO_REG).pass(Pass.ALWAYS_INLINE).pass(Pass.JUMP_THREADING).pass(Pass.SIMPLIFY_CFG);
            return optimize(compiledFiles, options, "opt");
        }

        @Override
        public ProgrammingLanguage[] getSupportedLanguages() {
            return GCC.getSupportedLanguages();
        }

    }

    protected static List<TestCaseFile[]> getTestCasesFromConfigFile(File configFile, File testSuite, TestCaseGenerator gen) throws IOException, AssertionError {
        TestSpecification testSpecification = SpecificationFileReader.readSpecificationFolder(configFile, testSuite);
        List<SpecificationEntry> includedFiles = testSpecification.getIncludedFiles();
        List<TestCaseFile[]> testCaseFiles;
        if (SulongTestOptions.TEST.testDiscoveryPath() != null) {
            List<SpecificationEntry> excludedFiles = testSpecification.getExcludedFiles();
            File absoluteDiscoveryPath = new File(testSuite.getAbsolutePath(), SulongTestOptions.TEST.testDiscoveryPath());
            assert absoluteDiscoveryPath.exists() : absoluteDiscoveryPath.toString();
            LLVMLogger.info("\tcollect files");
            List<File> filesToRun = getFilesRecursively(absoluteDiscoveryPath, gen);
            for (SpecificationEntry alreadyCanExecute : includedFiles) {
                filesToRun.remove(alreadyCanExecute.getFile());
            }
            for (SpecificationEntry excludedFile : excludedFiles) {
                filesToRun.remove(excludedFile.getFile());
            }
            List<TestCaseFile[]> discoveryTestCases = new ArrayList<>();
            for (File f : filesToRun) {
                if (ProgrammingLanguage.LLVM.isFile(f)) {
                    TestCaseFile testCase = gen.getBitCodeTestCaseFiles(new SpecificationEntry(f));
                    discoveryTestCases.add(new TestCaseFile[]{testCase});
                } else {
                    List<TestCaseFile> testCases = gen.getCompiledTestCaseFiles(new SpecificationEntry(f));
                    for (TestCaseFile testCase : testCases) {
                        discoveryTestCases.add(new TestCaseFile[]{testCase});
                    }
                }
            }
            LLVMLogger.info("\tfinished collecting files");
            testCaseFiles = discoveryTestCases;
        } else {
            List<TestCaseFile[]> includedFileTestCases = collectIncludedFiles(includedFiles, gen);
            testCaseFiles = includedFileTestCases;
        }
        return testCaseFiles;
    }

    protected static List<TestCaseFile[]> collectIncludedFiles(List<SpecificationEntry> specificationEntries, TestCaseGenerator gen) throws AssertionError {
        List<TestCaseFile[]> files = new ArrayList<>();
        for (SpecificationEntry e : specificationEntries) {
            File f = e.getFile();
            if (f.isFile()) {
                if (ProgrammingLanguage.LLVM.isFile(f)) {
                    files.add(new TestCaseFile[]{gen.getBitCodeTestCaseFiles(e)});
                } else {
                    for (TestCaseFile testCaseFile : gen.getCompiledTestCaseFiles(e)) {
                        files.add(new TestCaseFile[]{testCaseFile});
                    }
                }
            } else {
                throw new AssertionError("could not find specified test file " + f);
            }
        }
        return files;
    }

    public static List<File> getFilesRecursively(File currentFolder, TestCaseGenerator gen) {
        List<File> allBitcodeFiles = new ArrayList<>(1000);
        List<File> cFiles = TestHelper.collectFilesWithExtension(currentFolder, gen.getSupportedLanguages());
        allBitcodeFiles.addAll(cFiles);
        return allBitcodeFiles;
    }

    protected static List<TestCaseFile> applyOpt(List<TestCaseFile> allBitcodeFiles, OptOptions pass, String name) {
        return getFilteredOptStream(allBitcodeFiles).map(f -> optimize(f, pass, name)).collect(Collectors.toList());
    }

    protected static Stream<TestCaseFile> getFilteredOptStream(List<TestCaseFile> allBitcodeFiles) {
        return allBitcodeFiles.parallelStream().filter(f -> !f.getOriginalFile().getParent().endsWith(LLVMPaths.NO_OPTIMIZATIONS_FOLDER_NAME));
    }

    protected static TestCaseFile optimize(TestCaseFile toBeOptimized, OptOptions optOptions, String name) {
        final File destinationFile;
        if (ProgrammingLanguage.LLVM.isFile(toBeOptimized.getBitCodeFile())) {
            destinationFile = TestHelper.getTempLLFile(toBeOptimized.getOriginalFile(), "_" + name);
            Opt.optimizeLLVMIRFile(toBeOptimized.getBitCodeFile(), destinationFile, optOptions);
        } else {
            destinationFile = TestHelper.getTempBCFile(toBeOptimized.getOriginalFile(), "_" + name);
            Opt.optimizeBitcodeFile(toBeOptimized.getBitCodeFile(), destinationFile, optOptions);
        }

        return TestCaseFile.createFromCompiledFile(toBeOptimized.getOriginalFile(), destinationFile, toBeOptimized.getFlags());
    }

    public void executeLLVMBitCodeFileTest(TestCaseFile tuple) {
        try {
            LLVMLogger.info("original file: " + tuple.getOriginalFile());
            File bitCodeFile = tuple.getBitCodeFile();
            int expectedResult;
            try {
                expectedResult = TestHelper.executeLLVMBinary(bitCodeFile).getReturnValue();
            } catch (Throwable t) {
                t.printStackTrace();
                throw new LLVMUnsupportedException(UnsupportedReason.CLANG_ERROR);
            }
            int truffleResult = truncate(LLVM.executeMain(bitCodeFile));
            boolean undefinedReturnCode = tuple.hasFlag(TestCaseFlag.UNDEFINED_RETURN_CODE);
            boolean pass = true;
            if (!undefinedReturnCode) {
                pass &= expectedResult == truffleResult;
            }
            recordTestCase(tuple, pass);
            if (!undefinedReturnCode) {
                Assert.assertEquals(bitCodeFile.getAbsolutePath(), expectedResult, truffleResult);
            }
        } catch (Throwable e) {
            recordError(tuple, e);
            throw e;
        }
    }

    private static int truncate(int retValue) {
        return retValue & UNSIGNED_BYTE_MAX_VALUE;
    }
}
