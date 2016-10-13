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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.options.LLVMBaseOptionFacade;
import com.oracle.truffle.llvm.tools.util.ProcessUtil;

public final class ToolLookup {

    private static final int SEARCH_DEPTH = 999;

    public static Path findTool(List<String> toolNames, Predicate<String> versionPredicate) {
        String[] pathFolders = System.getenv("PATH").split(":");
        for (String folder : pathFolders) {
            try {
                Optional<Path> findAny = Files.find(Paths.get(folder), SEARCH_DEPTH, (p, f) -> toolNames.contains(p.getFileName().toFile().getName())).filter(p -> p.toFile().canExecute()).filter(
                                p -> versionPredicate.test(getVersion(p.toString()))).findAny();
                if (findAny.isPresent()) {
                    return findAny.get();
                }
            } catch (IOException | UncheckedIOException ex) {
                LLVMLogger.info(ex.getMessage());
            }
        }
        return null;
    }

    private static final String URL = "https://lafo.ssw.uni-linz.ac.at/pub/sulong-deps/";
    private static final String LLVM_LINUX_x86_64 = "clang+llvm-3.2-x86_64-linux-ubuntu-12.04";
    private static final String LLVM_DARWIN = "clang+llvm-3.2-x86_64-apple-darwin11";

    private static final File PROJECT_ROOT = new File(LLVMBaseOptionFacade.getProjectRoot() + File.separator + LLVMTools.class.getPackage().getName());
    private static final File TOOLS_ROOT = new File(PROJECT_ROOT, "tools");

    public static void downloadLLVM32() {
        String binaryName;
        if (isUnix() && is64bit()) {
            binaryName = LLVM_LINUX_x86_64;
        } else if (isMac()) {
            binaryName = LLVM_DARWIN;
        } else {
            throw new IllegalStateException("Your operating system is not supported by Sulong.");
        }
        File zip = downloadLLVM32(binaryName);
        unpack(zip, binaryName);

    }

    private static File downloadLLVM32(String binaryName) {
        File zip = new File(TOOLS_ROOT.getAbsolutePath() + "/llvm.zip");
        URL website = null;
        try {
            website = new URL(URL + binaryName + ".zip");
        } catch (MalformedURLException e1) {
            throw new IllegalStateException(e1);
        }
        try (InputStream in = website.openStream()) {
            Files.copy(in, Paths.get(zip.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            throw new IllegalStateException(e1);
        }
        return zip;
    }

    private static void unpack(File zip, String binaryName) {
        File target = new File((TOOLS_ROOT + "/llvm"));
        if (target.exists()) {
            deleteDirectory(target);
        }

        try {
            unzip(zip.getAbsolutePath(), TOOLS_ROOT);
        } catch (IOException e) {
            throw new IllegalStateException("Could not unpack LLVM binary", e);
        }

        new File(TOOLS_ROOT.getAbsolutePath() + "/" + binaryName).renameTo(target);
    }

    private static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    private static final int BUFFER_SIZE = 4096;

    private static void unzip(String zipFile, File destination) throws ZipException, IOException {
        destination.mkdir();

        File file = new File(zipFile);
        ZipFile zip = new ZipFile(file);
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> zipFileEntries = (Enumeration<ZipEntry>) zip.entries();

        while (zipFileEntries.hasMoreElements()) {
            ZipEntry entry = zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(destination, currentEntry);
            File destinationParent = destFile.getParentFile();
            destinationParent.mkdirs();

            if (!entry.isDirectory()) {
                unzipFile(zip, entry, destFile);
            }
        }
    }

    private static void unzipFile(ZipFile zip, ZipEntry entry, File destFile) throws IOException, FileNotFoundException {
        BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
        int currentByte;
        byte[] data = new byte[BUFFER_SIZE];

        FileOutputStream fos = new FileOutputStream(destFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos,
                        BUFFER_SIZE);

        while ((currentByte = is.read(data, 0, BUFFER_SIZE)) != -1) {
            dest.write(data, 0, currentByte);
        }
        dest.flush();
        dest.close();
        is.close();
    }

    private static boolean is64bit() {
        return System.getProperty("os.arch", "generic").contains("64");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "generic").toLowerCase().contains("mac");
    }

    private static boolean isUnix() {
        String os = System.getProperty("os.name", "generic").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0);
    }

    private static String getVersion(String tool) {
        try {
            return ProcessUtil.executeNativeCommand(tool + " --version").getStdOutput();
        } catch (RuntimeException e) {
            LLVMLogger.info(e.getMessage());
            return "";
        }
    }
}
