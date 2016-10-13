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
package com.oracle.truffle.llvm.test.alpha;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.llvm.test.LLVMPaths;
import com.oracle.truffle.llvm.tools.ProgrammingLanguage;
import com.oracle.truffle.llvm.tools.ToolLookup;

public class TestHarness {

    @Test
    public void test01() {
        List<Path> files = collectFiles(LLVMPaths.SIMPLE_TESTS, ProgrammingLanguage.C);
        assertEquals(files.size(), 2);
        Assert.assertTrue(files.stream().filter(s -> s.endsWith("simple1.c")).findAny().isPresent());
        Assert.assertTrue(files.stream().filter(s -> s.endsWith("simple2.c")).findAny().isPresent());
    }

    // TODO: remove
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

    @Test
    public void test02() {
        Assert.assertNotNull(ToolLookup.findTool(generateToolNames("clang", new String[]{"3.8"}), s -> Stream.of("3.8").filter(v -> s.contains(v)).findAny().isPresent()));
    }

    @Test
    public void test03() {
        ToolLookup.downloadLLVM32();
    }

    private static List<Path> collectFiles(File folder, ProgrammingLanguage... languages) {
        List<String> fileEndings = new ArrayList<>();
        for (ProgrammingLanguage l : languages) {
            for (String extension : l.getFileExtensions()) {
                fileEndings.add("." + extension);
            }
        }
        try {
            return Files.find(Paths.get(folder.getAbsolutePath()), 999, (p, bfa) -> bfa.isRegularFile()).filter(d -> {
                for (String extension : fileEndings) {
                    if (d.toFile().getName().endsWith(extension)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalArgumentException(folder.getAbsolutePath() + " cannot be processed by TestHarness", ex);
        }
    }
}
