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
package com.oracle.truffle.llvm.writer.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.LLVM;
import com.oracle.truffle.llvm.context.LLVMContext;
import com.oracle.truffle.llvm.context.LLVMLanguage;
import com.oracle.truffle.llvm.context.LLVMLanguage.LLVMLanguageProvider;
import com.oracle.truffle.llvm.parser.api.LLVMParserResult;
import com.oracle.truffle.llvm.parser.api.facade.NodeFactoryFacade;
import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.parser.bc.util.writer.ModelToIRVisitor;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.writer.facades.ModelModuleFacade;

public class TestExecutor {
    public void testModel(ModelModuleFacade modelModule, int expectedRetVal) {
        testModel(modelModule.getModel(), expectedRetVal);
    }

    public void testModel(Model model, int expectedRetVal) {
        LLVMLogger.error("\n\033[92m######################################");
        LLVMLogger.error(ModelToIRVisitor.getIRString(model));
        LLVMLogger.error("######################################\033[0m");

        // TODO: execute
        // NodeFactoryFacade factoryFacade = NodeFactoryFacadeProviderImpl.getNodeFactoryFacade();
        // TODO: NodeFactoryFacadeProviderImpl.get;
        // LLVMLanguage.INSTANCE = new LLVMTestLanguage(); // .createContext(null);
        LLVMLanguage.provider = new LLVMTestLanguageProvider();
        // LLVMLanguage.context = LLVMLanguage.provider.createContext(null);
        // LLVMLanguage.INSTANCE.createContext(null);
        LLVMContext context = LLVMLanguage.INSTANCE.findContext0();

        // vm.eval(fileSource).as(Integer.class);

        // LLVMContext context = new LLVMContext(LLVM.getNodeFactoryFacade());

        // LLVMContext context = new LLVMContext(new NodeFactoryFacadeImpl());
        //
        LLVMParserResult parserResult = LLVM.parseModel(model, context);

        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(parserResult.getMainFunction().getRootNode());

        Object result = callTarget.call();
        System.out.println("result: " + result);
        assertEquals(expectedRetVal, result);
    }

    static final class LLVMTestLanguageProvider implements LLVMLanguageProvider {
        @Override
        public LLVMContext createContext(Env env) {
            NodeFactoryFacade facade = LLVM.getNodeFactoryFacade();
            LLVMContext context = new LLVMContext(facade);
            context.getStack().allocate();
            return context;
        }

        @Override
        public CallTarget parse(Source code, Node context, String... argumentNames) throws IOException {
            return null;
        }

        @Override
        public void disposeContext(LLVMContext context) {
        }

    }
}
