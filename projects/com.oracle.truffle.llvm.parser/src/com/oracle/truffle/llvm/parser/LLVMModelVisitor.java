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
package com.oracle.truffle.llvm.parser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.runtime.types.Type;

final class LLVMModelVisitor implements ModelVisitor {

    private final LLVMParserRuntime visitor;
    private final LLVMFunctionRegistry registry;

    LLVMModelVisitor(LLVMParserRuntime visitor, LLVMFunctionRegistry registry) {
        this.visitor = visitor;
        this.registry = registry;
    }

    @Override
    public void visit(GlobalAlias alias) {
        visitor.getAliases().put(alias, alias.getValue());
    }

    @Override
    public void visit(GlobalConstant constant) {
        visitor.getGlobals().put(constant, null);
    }

    @Override
    public void visit(GlobalVariable variable) {
        visitor.getGlobals().put(variable, null);
    }

    @Override
    public void visit(FunctionDeclaration method) {
    }

    @Override
    public void visit(FunctionDefinition method) {
        FrameDescriptor frame = visitor.getStack().getFrame(method.getName());

        List<LLVMExpressionNode> parameters = visitor.createParameters(frame, method);

        final LLVMLifetimeAnalysis lifetimes = LLVMLifetimeAnalysis.getResult(method, frame, visitor.getPhis().getPhiMap(method.getName()));

        LLVMExpressionNode body = visitor.createFunction(method, lifetimes);

        LLVMExpressionNode[] beforeFunction = parameters.toArray(new LLVMExpressionNode[parameters.size()]);
        LLVMExpressionNode[] afterFunction = new LLVMExpressionNode[0];

        final String sourceText = String.format("%s:%s", visitor.getSource().getName(), method.getName());
        final Source irSource = Source.newBuilder(sourceText).mimeType("text/plain").name(sourceText).build();
        final SourceSection sourceSection = irSource.createSection(1);
        RootNode rootNode = visitor.getNodeFactoryFacade().createFunctionStartNode(visitor, body, beforeFunction, afterFunction, sourceSection, frame, method);

        final String astPrintTarget = LLVMOptions.DEBUG.printFunctionASTs();
        if (LLVMLogger.TARGET_STDOUT.equals(astPrintTarget) || LLVMLogger.TARGET_ANY.equals(astPrintTarget)) {
            // Checkstyle: stop
            NodeUtil.printTree(System.out, rootNode);
            System.out.flush();
            // Checkstyle: resume

        } else if (LLVMLogger.TARGET_STDERR.equals(astPrintTarget)) {
            // Checkstyle: stop
            NodeUtil.printTree(System.err, rootNode);
            System.err.flush();
            // Checkstyle: resume

        } else if (!LLVMLogger.TARGET_NONE.equals(astPrintTarget)) {
            try (PrintStream out = new PrintStream(new FileOutputStream(astPrintTarget, true))) {
                NodeUtil.printTree(out, rootNode);
                out.flush();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot write to file: " + astPrintTarget);
            }
        }

        LLVMFunctionDescriptor function = registry.lookupFunctionDescriptor(method.getName(), method.getType());
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        function.setCallTarget(callTarget);
        visitor.addFunction(function);

        visitor.exitFunction();
    }

    @Override
    public void visit(Type type) {
    }

}
