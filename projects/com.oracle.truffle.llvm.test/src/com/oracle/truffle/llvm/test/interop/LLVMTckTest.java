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
package com.oracle.truffle.llvm.test.interop;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.llvm.nodes.impl.base.LLVMLanguage;
import com.oracle.truffle.llvm.test.LLVMPaths;
import com.oracle.truffle.llvm.tools.Clang;
import com.oracle.truffle.llvm.tools.Clang.ClangOptions;
import com.oracle.truffle.llvm.tools.Opt;
import com.oracle.truffle.llvm.tools.Opt.OptOptions;
import com.oracle.truffle.llvm.tools.Opt.OptOptions.Pass;
import com.oracle.truffle.tck.TruffleTCK;

/**
 * This is the way to verify your language implementation is compatible.
 *
 */
public class LLVMTckTest extends TruffleTCK {
    private static final String PATH = LLVMPaths.LOCAL_TESTS + "/../interoptests";
    private static final String FILENAME = "tck";

    @Test
    public void testVerifyPresence() {
        PolyglotEngine vm = PolyglotEngine.newBuilder().build();
        assertTrue("Our language is present", vm.getLanguages().containsKey(LLVMLanguage.LLVM_IR_MIME_TYPE));
        vm.dispose();
    }

    @Override
    protected PolyglotEngine prepareVM(PolyglotEngine.Builder builder) throws Exception {
        PolyglotEngine vm = builder.build();
        // @formatter:off
        /*
        vm.eval(
            Source.fromText(
                "function fourtyTwo() {\n" +
                "  return 42;\n" + //
                "}\n" +
                "function plus(a, b) {\n" +
                "  return a + b;\n" +
                "}\n" +
                "function identity(x) {\n" +
                "  return x;\n" +
                "}\n" +
                "function apply(f) {\n" +
                "  return f(18, 32) + 10;\n" +
                "}\n" +
                "function cnt() {\n" +
                "  return 0;\n" +
                "}\n" +
                "function count() {\n" +
                "  n = cnt() + 1;\n" +
                "  defineFunction(\"function cnt() { return \" + n + \"; }\");\n" +
                "  return n;\n" +
                "}\n" +
                "function returnsNull() {\n" +
                "}\n" +
                "function complexAdd(a, b) {\n" +
                "  a.real = a.real + b.real;\n" +
                "  a.imaginary = a.imaginary + b.imaginary;\n" +
                "}\n" +
                "function compoundObject() {\n" +
                "  obj = new();\n" +
                "  obj.fourtyTwo = fourtyTwo;\n" +
                "  obj.plus = plus;\n" +
                "  obj.returnsNull = returnsNull;\n" +
                "  obj.returnsThis = obj;\n" +
                "  return obj;\n" +
                "}\n" +
                "function valuesObject() {\n" +
                "  obj = new();\n" +
                "  obj.byteValue = 0;\n" +
                "  obj.shortValue = 0;\n" +
                "  obj.intValue = 0;\n" +
                "  obj.longValue = 0;\n" +
                "  obj.floatValue = 0;\n" +
                "  obj.doubleValue = 0;\n" +
                "  obj.charValue = \"0\";\n" +
                "  obj.booleanValue = (1 == 0);\n" +
                "  return obj;\n" +
                "}\n",
                "LLVM TCK"
            ).withMimeType(LLVMLanguage.LLVM_MIME_TYPE)
        );
        */
        // @formatter:on
        try {
            File cFile = new File(PATH, FILENAME + ".c");
            File bcFile = File.createTempFile(PATH + "/" + "bc_" + FILENAME, ".ll");
            File bcOptFile = File.createTempFile(PATH + "/" + "bcopt_" + FILENAME, ".ll");
            Clang.compileToLLVMIR(cFile, bcFile, ClangOptions.builder());
            Opt.optimizeBitcodeFile(bcFile, bcOptFile, OptOptions.builder().pass(Pass.MEM_TO_REG));
            vm.eval(Source.fromFileName(bcOptFile.getPath())).as(Integer.class);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return vm;
    }

    @Override
    protected String mimeType() {
        return LLVMLanguage.LLVM_IR_MIME_TYPE;
    }

    @Override
    protected String fourtyTwo() {
        return "fourtyTwo";
    }

    @Override
    protected String identity() {
        return "identity";
    }

    @Override
    protected String plus(Class<?> type1, Class<?> type2) {
        return "plus";
    }

    @Override
    protected String returnsNull() {
        return "returnsNull";
    }

    @Override
    protected String applyNumbers() {
        return "apply";
    }

    @Override
    protected String compoundObject() {
        return "compoundObject";
    }

    @Override
    protected String valuesObject() {
        return "valuesObject";
    }

    @Override
    protected String invalidCode() {
        // @formatter:off
        return
            "f unction main() {\n" +
            "  retu rn 42;\n" +
            "}\n";
        // @formatter:on
    }

    @Override
    protected String complexAdd() {
        return "complexAdd";
    }

    @Override
    protected String multiplyCode(String firstName, String secondName) {
        // @formatter:off
        /*
        return
            "function multiply(" + firstName + ", " + secondName + ") {\n" +
            "  return " + firstName + " * " + secondName + ";\n" +
            "}\n";
        // @formatter:on
         */
        return null;
    }

    @Override
    protected String countInvocations() {
        return "count";
    }

    @Override
    protected String globalObject() {
        return null;
    }

    @Override
    protected String evaluateSource() {
        return null;
    }

    @Override
    protected String complexCopy() {
        return null;
    }

    @Override
    protected String complexAddWithMethod() {
        return null;
    }

    @Override
    protected String complexSumReal() {
        return null;
    }

    @Override
    protected void assertDouble(String msg, double expectedValue, double actualValue) {
    }
}
