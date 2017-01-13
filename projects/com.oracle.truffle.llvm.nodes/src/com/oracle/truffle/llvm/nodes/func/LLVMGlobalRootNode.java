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
package com.oracle.truffle.llvm.nodes.func;

import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.context.LLVMContext;
import com.oracle.truffle.llvm.context.LLVMLanguage;
import com.oracle.truffle.llvm.nodes.base.LLVMFrameUtil;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMAbort;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMSignal;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMExitException;
import com.oracle.truffle.llvm.runtime.LLVMFunction;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;

/**
 * The global entry point initializes the global scope and starts execution with the main function.
 */
public class LLVMGlobalRootNode extends RootNode {

    protected final DirectCallNode main;
    @CompilationFinal(dimensions = 1) protected final Object[] arguments;
    protected final LLVMContext context;
    // FIXME instead make the option system "PE safe"
    protected final boolean printNativeStats = LLVMOptions.DEBUG.printNativeCallStatistics();
    protected final int executionCount = LLVMOptions.ENGINE.executionCount();
    protected final boolean printExecutionTime = LLVMOptions.DEBUG.printExecutionTime();
    protected final FrameSlot stackPointerSlot;
    protected long startExecutionTime;
    protected long endExecutionTime;

    public LLVMGlobalRootNode(FrameSlot stackSlot, FrameDescriptor descriptor, LLVMContext context, CallTarget main, Object... arguments) {
        super(LLVMLanguage.class, null, descriptor);
        this.stackPointerSlot = stackSlot;
        this.context = context;
        this.main = Truffle.getRuntime().createDirectCallNode(main);
        this.arguments = arguments;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        LLVMAddress stackPointer = context.getStack().getUpperBounds();
        try {
            Object result = null;
            for (int i = 0; i < executionCount; i++) {
                assert LLVMSignal.getNumberOfRegisteredSignals() == 0;

                frame.setObject(stackPointerSlot, stackPointer);
                Object[] realArgs = new Object[arguments.length + LLVMCallNode.ARG_START_INDEX];
                realArgs[0] = LLVMFrameUtil.getAddress(frame, stackPointerSlot);
                for (int j = LLVMCallNode.ARG_START_INDEX; j < arguments.length + LLVMCallNode.ARG_START_INDEX; j++) {
                    realArgs[j] = arguments[j - LLVMCallNode.ARG_START_INDEX];
                }
                result = executeIteration(frame, i, realArgs);

                context.awaitThreadTermination();
                assert LLVMSignal.getNumberOfRegisteredSignals() == 0;
            }
            return result;
        } catch (LLVMExitException e) {
            context.awaitThreadTermination();
            assert LLVMSignal.getNumberOfRegisteredSignals() == 0;
            return e.getReturnCode();
        } finally {
            // if not done already, we want at least call a shutdown command
            context.shutdownThreads();
            if (printNativeStats) {
                printNativeCallStats(context);
            }
        }
    }

    protected Object executeIteration(VirtualFrame frame, int iteration, Object[] args) {
        Object result;

        if (iteration != 0) {
            executeStaticInits();
            executeConstructorFunctions();
        }

        if (printExecutionTime) {
            startExecutionTime = getTime();
        }

        int returnCode = 0;

        try {
            result = main.call(frame, args);
        } catch (LLVMExitException e) {
            returnCode = e.getReturnCode();
            throw e;
        } finally {
            // We shouldn't execute atexit, when there was an abort
            if (returnCode != LLVMAbort.UNIX_SIGABORT) {
                executeAtExitFunctions();
            }
        }

        if (printExecutionTime) {
            endExecutionTime = getTime();
            printExecutionTime();
        }

        if (iteration != executionCount - 1) {
            executeDestructorFunctions();
        }
        return result;
    }

    @TruffleBoundary
    private static long getTime() {
        return System.currentTimeMillis();
    }

    @TruffleBoundary
    protected void printExecutionTime() {
        long executionTime = endExecutionTime - startExecutionTime;
        LLVMLogger.unconditionalInfo("execution time: " + executionTime + " ms");
    }

    @TruffleBoundary
    protected void executeStaticInits() {
        List<RootCallTarget> globalVarInits = context.getGlobalVarInits();
        for (RootCallTarget callTarget : globalVarInits) {
            callTarget.call(globalVarInits);
        }
    }

    @TruffleBoundary
    private void executeConstructorFunctions() {
        List<RootCallTarget> constructorFunctions = context.getConstructorFunctions();
        for (RootCallTarget callTarget : constructorFunctions) {
            callTarget.call(constructorFunctions);
        }
    }

    @TruffleBoundary
    protected void executeDestructorFunctions() {
        List<RootCallTarget> destructorFunctions = context.getDestructorFunctions();
        for (RootCallTarget callTarget : destructorFunctions) {
            callTarget.call(destructorFunctions);
        }
    }

    @TruffleBoundary
    protected void executeAtExitFunctions() {
        Deque<RootCallTarget> atExitFunctions = context.getAtExitFunctions();
        LLVMExitException lastExitException = null;
        while (!atExitFunctions.isEmpty()) {
            try {
                atExitFunctions.pop().call(atExitFunctions);
            } catch (LLVMExitException e) {
                lastExitException = e;
            }
        }
        if (lastExitException != null) {
            throw lastExitException;
        }
    }

    @TruffleBoundary
    protected static void printNativeCallStats(LLVMContext context) {
        Map<LLVMFunction, Integer> nativeFunctionCallSites = context.getNativeFunctionLookupStats();
        // Checkstyle: stop
        if (!nativeFunctionCallSites.isEmpty()) {
            System.out.println("==========================");
            System.out.println("native function sites:");
            System.out.println("==========================");
            for (LLVMFunction function : nativeFunctionCallSites.keySet()) {
                String output = String.format("%15s: %3d", function.getName(), nativeFunctionCallSites.get(function));
                System.out.println(output);
            }
            System.out.println("==========================");
        }
        // Checkstyle: resume
    }

}
