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
package com.oracle.truffle.llvm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.LLVMParserResult;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.NodeFactory;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;

public final class Runner {

    private final NodeFactory nodeFactory;

    public Runner(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public CallTarget parse(LLVMLanguage language, LLVMContext context, Source code) throws IOException {
        try {
            /*
             * TODO: currently, we need to load the bitcode libraries first. Otherwise, sulong is
             * not able to link external variables which were defined in those libraries.
             */
            parseDynamicBitcodeLibraries(language, context);

            CallTarget mainFunction = null;
            if (code.getMimeType().equals(Sulong.LLVM_BITCODE_MIME_TYPE) || code.getMimeType().equals(Sulong.LLVM_BITCODE_BASE64_MIME_TYPE) || code.getMimeType().equals("x-unknown")) {
                LLVMParserResult parserResult = parseBitcodeFile(code, language, context);
                mainFunction = parserResult.getMainCallTarget();
                handleParserResult(context, parserResult);
            } else if (code.getMimeType().equals(Sulong.SULONG_LIBRARY_MIME_TYPE)) {
                final Library library = new Library(new File(code.getPath()));
                List<Source> sourceFiles = new ArrayList<>();
                library.readContents(dependentLibrary -> {
                    context.addLibraryToNativeLookup(dependentLibrary);
                }, source -> {
                    sourceFiles.add(source);
                });
                for (Source source : sourceFiles) {
                    String mimeType = source.getMimeType();
                    try {
                        LLVMParserResult parserResult;
                        if (mimeType.equals(Sulong.LLVM_BITCODE_MIME_TYPE) || mimeType.equals(Sulong.LLVM_BITCODE_BASE64_MIME_TYPE)) {
                            parserResult = parseBitcodeFile(source, language, context);
                        } else {
                            throw new UnsupportedOperationException(mimeType);
                        }
                        handleParserResult(context, parserResult);
                        if (parserResult.getMainCallTarget() != null) {
                            mainFunction = parserResult.getMainCallTarget();
                        }
                    } catch (Throwable t) {
                        throw new IOException("Error while trying to parse " + source.getName(), t);
                    }
                }
                if (mainFunction == null) {
                    mainFunction = Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(null));
                }
            } else {
                throw new IllegalArgumentException("undeclared mime type: " + code.getMimeType());
            }
            if (context.getEnv().getOptions().get(SulongEngineOption.PARSE_ONLY)) {
                return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(0));
            } else {
                return mainFunction;
            }
        } catch (Throwable t) {
            throw new IOException("Error while trying to parse " + code.getPath(), t);
        }
    }

    private static void visitBitcodeLibraries(LLVMContext context, Consumer<Source> sharedLibraryConsumer) throws IOException {
        String[] dynamicLibraryPaths = SulongEngineOption.getBitcodeLibraries(context.getEnv());
        if (dynamicLibraryPaths != null && dynamicLibraryPaths.length != 0) {
            for (String s : dynamicLibraryPaths) {
                addLibrary(s, sharedLibraryConsumer);
            }
        }
    }

    private static void addLibrary(String s, Consumer<Source> sharedLibraryConsumer) throws IOException {
        File lib = Paths.get(s).toFile();
        Source source = Source.newBuilder(lib).build();
        sharedLibraryConsumer.accept(source);
    }

    private void parseDynamicBitcodeLibraries(LLVMLanguage language, LLVMContext context) throws IOException {
        if (!context.bcLibrariesLoaded()) {
            context.setBcLibrariesLoaded();
            visitBitcodeLibraries(context, source -> {
                try {
                    new Runner(nodeFactory).parse(language, context, source);
                } catch (Throwable t) {
                    throw new RuntimeException("Error while trying to parse dynamic library " + source.getName(), t);
                }
            });
        }
    }

    private static void handleParserResult(LLVMContext context, LLVMParserResult result) {
        context.registerGlobalVarInit(result.getGlobalVarInit());
        context.registerGlobalVarDealloc(result.getGlobalVarDealloc());
        if (result.getConstructorFunction() != null) {
            context.registerConstructorFunction(result.getConstructorFunction());
        }
        if (result.getDestructorFunction() != null) {
            context.registerDestructorFunction(result.getDestructorFunction());
        }
        if (!context.getEnv().getOptions().get(SulongEngineOption.PARSE_ONLY)) {
            context.getThreadingStack().checkThread();
            long stackPointer = context.getThreadingStack().getStack().getStackPointer();
            result.getGlobalVarInit().call(stackPointer);
            context.getThreadingStack().getStack().setStackPointer(stackPointer);
            if (result.getConstructorFunction() != null) {
                stackPointer = context.getThreadingStack().getStack().getStackPointer();
                result.getConstructorFunction().call(stackPointer);
                context.getThreadingStack().getStack().setStackPointer(stackPointer);
            }
        }
    }

    public static void disposeContext(LLVMContext context) {
        context.getThreadingStack().checkThread();
        for (RootCallTarget destructorFunction : context.getDestructorFunctions()) {
            long stackPointer = context.getThreadingStack().getStack().getStackPointer();
            destructorFunction.call(stackPointer);
            context.getThreadingStack().getStack().setStackPointer(stackPointer);
        }
        for (RootCallTarget destructor : context.getGlobalVarDeallocs()) {
            long stackPointer = context.getThreadingStack().getStack().getStackPointer();
            destructor.call(stackPointer);
            context.getThreadingStack().getStack().setStackPointer(stackPointer);
        }
        context.getThreadingStack().freeStacks();
    }

    private LLVMParserResult parseBitcodeFile(Source source, LLVMLanguage language, LLVMContext context) {
        context.setNativeIntrinsicsFactory(nodeFactory.getNativeIntrinsicsFactory(language, context));
        return LLVMParserRuntime.parse(source, language, context, nodeFactory);
    }
}
