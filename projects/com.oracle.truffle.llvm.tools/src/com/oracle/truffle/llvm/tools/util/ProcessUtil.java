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
package com.oracle.truffle.llvm.tools.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProcessUtil {

    private static final int PROCESS_WAIT_TIMEOUT = 30000;

    public static final class ProcessResult {

        private final String originalCommand;
        private final String stdErr;
        private final String stdInput;
        private final int returnValue;

        private ProcessResult(String originalCommand, int returnValue, String stdErr, String stdInput) {
            this.originalCommand = originalCommand;
            this.returnValue = returnValue;
            this.stdErr = stdErr;
            this.stdInput = stdInput;
        }

        public String getStdErr() {
            return stdErr;
        }

        public String getStdInput() {
            return stdInput;
        }

        public int getReturnValue() {
            return returnValue;
        }

        @Override
        public String toString() {
            return originalCommand + ": " + returnValue;
        }

    }

    public static ProcessResult executeNativeCommandZeroReturn(String command) {
        ProcessResult result = executeNativeCommand(command);
        checkNoError(result);
        return result;
    }

    /**
     * Executes a native command and checks that the return value of the process is 0.
     */
    public static ProcessResult executeNativeCommandZeroReturn(String... command) {
        ProcessResult result;
        if (command.length == 1) {
            result = executeNativeCommand(command[0]);
        } else {
            result = executeNativeCommand(concatCommand(command));
        }
        checkNoError(result);
        return result;
    }

    /**
     * Concats a command by introducing whitespaces between the array elements.
     */
    static String concatCommand(String[] command) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(command[i]);
        }
        return sb.toString();
    }

    public static ProcessResult executeNativeCommand(String command) {
        if (command == null) {
            throw new IllegalArgumentException("command is null!");
        }
        try {
            Process process = Runtime.getRuntime().exec(command);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> inputStream = executor.submit(() -> readStreamAndClose(process.getInputStream()));
            Future<String> errorStream = executor.submit(() -> readStreamAndClose(process.getErrorStream()));
            process.waitFor(PROCESS_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
            int llvmResult = process.exitValue();
            return new ProcessResult(command, llvmResult, errorStream.get(), inputStream.get());
        } catch (Exception e) {
            throw new RuntimeException(command + " ", e);
        }
    }

    public static void checkNoError(ProcessResult processResult) {
        if (processResult.getReturnValue() != 0) {
            throw new IllegalStateException(processResult.originalCommand + " exited with value " + processResult.getReturnValue() + " " + processResult.getStdErr());
        }
    }

    public static String readStreamAndClose(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public static String readStream(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

}
