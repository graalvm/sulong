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
package com.oracle.truffle.llvm.tools;

import java.io.File;

import com.oracle.truffle.llvm.runtime.options.LLVMBaseOptionFacade;
import com.oracle.truffle.llvm.tools.util.ProcessUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GCC {

    private static final File PROJECT_ROOT = new File(LLVMBaseOptionFacade.getProjectRoot() + File.separator + LLVMTools.class.getPackage().getName());
    private static final File TOOLS_ROOT = new File(PROJECT_ROOT, "tools");

    private static final File LLVM_DRAGONEGG = new File(TOOLS_ROOT, "/dragonegg/dragonegg-3.2.src/dragonegg.so");
    private static final String[] GCC_SUPPORTED_VERSIONS = {"4.6", "4.7", "4.8"};

    public enum Tool {
        GPP_PATH("g++"),
        GFORTRAN_PATH("gfortran"),
        GCC_PATH("gcc");

        private final Path toolPath;

        Tool(String programName) {
            this.toolPath = findTool(programName);
        }

        private static List<String> generateToolNames(String name, String[] versions) {
            List<String> toolNames = new ArrayList<>();
            toolNames.add(name);
            for (String s : versions) {
                toolNames.add(name + "-" + s); // e.g. clang-3.2
                toolNames.add(name + "-mp-" + s); // e.g. clang-mp-3.2 for mac
                toolNames.add(name + s.replace(".", "")); // e.g. clang32
            }
            return toolNames;
        }

        private static Path findTool(String toolName) {
            Path path = ToolLookup.findTool(generateToolNames(toolName, GCC_SUPPORTED_VERSIONS), s -> Arrays.stream(GCC_SUPPORTED_VERSIONS).filter(v -> s.contains(v)).findAny().isPresent());
            if (path == null) {
                throw new IllegalStateException("Sorry, we could not find a suitable " + toolName + " version. Supported versions " + Arrays.toString(GCC_SUPPORTED_VERSIONS));
            }
            return path;
        }
    }

    private GCC() {
    }

    public static void compileObjectToMachineCode(File objectFile, File executable) {
        String linkCommand = Tool.GCC_PATH.toolPath.toString() + " " + objectFile.getAbsolutePath() + " -o " + executable.getAbsolutePath() + " -lm -lgfortran -lgmp";
        ProcessUtil.executeNativeCommandZeroReturn(linkCommand);
        executable.setExecutable(true);
    }

    public static void compileToLLVMIR(File toBeCompiled, File destinationFile) {
        String tool;
        if (ProgrammingLanguage.C.isFile(toBeCompiled)) {
            tool = Tool.GCC_PATH.toolPath.toString() + " -std=gnu99";
        } else if (ProgrammingLanguage.FORTRAN.isFile(toBeCompiled)) {
            tool = Tool.GFORTRAN_PATH.toolPath.toString();
        } else if (ProgrammingLanguage.C_PLUS_PLUS.isFile(toBeCompiled)) {
            tool = Tool.GCC_PATH.toolPath.toString();
        } else {
            throw new AssertionError(toBeCompiled);
        }

        String destinationLLFileName = destinationFile.getAbsolutePath();
        File interimFile;
        try {
            interimFile = File.createTempFile("interim", ".ll");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        if (destinationFile.getName().endsWith(".bc")) {
            destinationLLFileName = interimFile.getAbsolutePath();
        }

        String[] llFileGenerationCommand = new String[]{tool, "-I " + LLVMBaseOptionFacade.getProjectRoot() + "/../include", "-S", dragonEggOption(),
            "-fplugin-arg-dragonegg-emit-ir", "-o " + destinationLLFileName, toBeCompiled.getAbsolutePath()};
        ProcessUtil.executeNativeCommandZeroReturn(llFileGenerationCommand);

        // Converting interim .ll file to .bc file
        if (destinationFile.getName().endsWith(".bc")) {
            LLVMTools.Assembler.assembleToBitcodeFile(interimFile, destinationFile);
        }
    }

    private static String dragonEggOption() {
        return "-fplugin=" + LLVM_DRAGONEGG;
    }

    public static ProgrammingLanguage[] getSupportedLanguages() {
        return new ProgrammingLanguage[]{ProgrammingLanguage.C, ProgrammingLanguage.C_PLUS_PLUS, ProgrammingLanguage.FORTRAN};
    }
}
