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
import com.oracle.truffle.llvm.tools.util.PathUtil;
import com.oracle.truffle.llvm.tools.util.ProcessUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class LLVMTools {

    private static final File PROJECT_ROOT = new File(LLVMBaseOptionFacade.getProjectRoot() + File.separator + LLVMTools.class.getPackage().getName());
    private static final File TOOLS_ROOT = new File(PROJECT_ROOT, "tools");
    private static final File LLVM_ROOT = new File(TOOLS_ROOT, "llvm");
    private static final File LLVM_ROOT_BIN = new File(LLVM_ROOT, "bin");
    private static final String[] LLVM_SUPPORTED_VERSIONS = {"3.2"};

    public enum Tool {
        ASSEMBLER("llvm-as"),
        COMPILER("llc"),
        CLANG("clang"),
        CLANG_PP("clang++"),
        OPT("opt");

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
            Path path = ToolLookup.findTool(generateToolNames(toolName, LLVM_SUPPORTED_VERSIONS), s -> Arrays.stream(LLVM_SUPPORTED_VERSIONS).filter(v -> s.contains(v)).findAny().isPresent());
            if (path == null) {
                String downloadedClang = LLVM_ROOT_BIN.getAbsolutePath() + toolName;
                File clang = new File(downloadedClang);
                if (!clang.exists()) {
                    ToolLookup.downloadLLVM32();
                }
                if (!clang.exists() || !clang.canExecute()) {
                    throw new IllegalStateException("Sorry, we could not find a suitable " + toolName + " version. Supported versions " + Arrays.toString(LLVM_SUPPORTED_VERSIONS));
                }
                return Paths.get(clang.getAbsolutePath());
            }
            return path;
        }

    }

    public static final class Assembler {

        private Assembler() {
        }

        public static void assembleToBitcodeFile(File irFile) {
            String compilationCommand = LLVMTools.Tool.ASSEMBLER.toolPath.toString() + " " + irFile.getAbsolutePath();
            ProcessUtil.executeNativeCommandZeroReturn(compilationCommand);
        }

        public static void assembleToBitcodeFile(File irFile, File destFile) {
            if (!irFile.getAbsolutePath().endsWith(".ll")) {
                throw new IllegalArgumentException("Can only assemble .ll files!");
            }
            final String args = " -o=" + destFile.getAbsolutePath() + " " + irFile.getAbsolutePath();
            final String compilationCommand = LLVMTools.Tool.ASSEMBLER.toolPath.toString() + args;
            ProcessUtil.executeNativeCommandZeroReturn(compilationCommand);
        }

    }

    public static final class Opt {

        public enum Pass {
            MEM_TO_REG("mem2reg"),
            SIMPLIFY_CFG("simplifycfg"),
            BASIC_BLOCK_VECTORIZE("bb-vectorize"),
            INST_COMBINE("instcombine"),
            FUNC_ATTRS("functionattrs"),
            JUMP_THREADING("jump-threading"),
            SCALAR_REPLACEMENT_AGGREGATES("scalarrepl"),
            ALWAYS_INLINE("always-inline"),
            GVN("gvn"),
            LOWER_INVOKE("lowerinvoke"),
            PRUNE_EH("prune-eh");

            private final String option;

            Pass(String option) {
                this.option = option;
            }

            public String getOption() {
                return "-" + option;
            }

        }

        public static void optimizeBitcodeFile(File bitCodeFile, File destinationFile) {
            optimizeBitcodeFile(bitCodeFile, destinationFile, new ArrayList<>());
        }

        public static void optimizeBitcodeFile(File bitCodeFile, File destinationFile, Pass... passes) {
            optimizeBitcodeFile(bitCodeFile, destinationFile, Arrays.asList(passes));
        }

        public static void optimizeBitcodeFile(File bitCodeFile, File destinationFile, List<Pass> options) {
            String passes = options.stream().map(p -> p.getOption()).collect(Collectors.joining(" "));
            String clangCompileCommand = LLVMTools.Tool.OPT.toolPath.toString() + " -S " + passes + " " + bitCodeFile.getAbsolutePath() + " -o " + destinationFile;
            ProcessUtil.executeNativeCommandZeroReturn(clangCompileCommand);
        }
    }

    public static final class LLC {

        private LLC() {
        }

        public static void compileBitCodeToObjectFile(File bitcodeFile, File objectFile) {
            String compilationCommand = LLVMTools.Tool.COMPILER.toolPath.toString() + " -filetype=obj " + bitcodeFile.getAbsolutePath() + " -o " + objectFile.getAbsolutePath();
            ProcessUtil.executeNativeCommandZeroReturn(compilationCommand);
        }

    }

    public static final class Clang {

        private Clang() {
        }

        public enum OptimizationLevel {
            NONE(""),
            O1("O1"),
            O2("O2"),
            O3("O3");

            private final String level;

            OptimizationLevel(String level) {
                this.level = level;
            }

            public String getLevel() {
                return "-" + level;
            }
        }

        public static void compileToLLVMIR(File path, File destinationFile) {
            compileToLLVMIR(path, destinationFile);
        }

        public static void compileToLLVMIR(File path, File destinationFile, OptimizationLevel level) {
            File tool = getCompileToolFromExtension(path);
            String[] command = new String[]{tool.getAbsolutePath(), "-I " + LLVMBaseOptionFacade.getProjectRoot() + "/../include", emitLLVMIRTo(destinationFile), level.getLevel(),
                            "-c ", path.getAbsolutePath()};
            ProcessUtil.executeNativeCommandZeroReturn(command);
        }

        private static String emitLLVMIRTo(File destinationFile) {
            if (destinationFile.getName().endsWith(".bc")) {
                return " -emit-llvm -o " + destinationFile;
            }
            return " -S -emit-llvm -o " + destinationFile;
        }

        private static File getCompileToolFromExtension(File path) {
            String fileExtension = PathUtil.getExtension(path.getName());
            File tool;
            if (ProgrammingLanguage.C.isFile(path)) {
                tool = LLVMTools.Tool.CLANG.toolPath.toFile();
            } else if (ProgrammingLanguage.C_PLUS_PLUS.isFile(path)) {
                tool = LLVMTools.Tool.CLANG_PP.toolPath.toFile();
            } else if (ProgrammingLanguage.OBJECTIVE_C.isFile(path)) {
                tool = LLVMTools.Tool.CLANG.toolPath.toFile();
            } else {
                throw new IllegalArgumentException(fileExtension);
            }
            return tool;
        }

        public static File compileToExecutable(File path, OptimizationLevel level) {
            try {
                File outputFile = File.createTempFile(path.getName(), ".out");
                outputFile.setExecutable(true);
                compileToExecutable(path, outputFile, level);
                return outputFile;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public static void compileToExecutable(File path, File destinationFile, OptimizationLevel level) {
            File tool = getCompileToolFromExtension(path);
            String[] command = new String[]{tool.getAbsolutePath(), "-I " + LLVMBaseOptionFacade.getProjectRoot() + "/../include", level.getLevel(), path.getAbsolutePath(),
                            "-o " + destinationFile, "-lm"};
            ProcessUtil.executeNativeCommandZeroReturn(command);
        }

        public static ProgrammingLanguage[] getSupportedLanguages() {
            return new ProgrammingLanguage[]{ProgrammingLanguage.C, ProgrammingLanguage.C_PLUS_PLUS, ProgrammingLanguage.OBJECTIVE_C};
        }

    }

}
