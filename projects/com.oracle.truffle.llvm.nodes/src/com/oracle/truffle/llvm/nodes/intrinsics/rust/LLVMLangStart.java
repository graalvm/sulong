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
package com.oracle.truffle.llvm.nodes.intrinsics.rust;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.nodes.func.LLVMDispatchNode;
import com.oracle.truffle.llvm.nodes.func.LLVMDispatchNodeGen;
import com.oracle.truffle.llvm.nodes.func.LLVMLookupDispatchNode;
import com.oracle.truffle.llvm.nodes.func.LLVMLookupDispatchNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.nodes.intrinsics.rust.LLVMLangStartNodeGen.RustContextDestructorNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.StackPointer;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.FunctionType;

@NodeChildren({@NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class), @NodeChild(type = LLVMExpressionNode.class)})
public abstract class LLVMLangStart extends LLVMIntrinsic {

    private final RustContext rustContext;
    private final RootCallTarget rustContextDestructorCallTarget;

    public LLVMLangStart(RustContext rustContext, TruffleLanguage<?> language) {
        this.rustContext = rustContext;
        this.rustContextDestructorCallTarget = Truffle.getRuntime().createCallTarget(RustContextDestructorNodeGen.create(language, rustContext));
    }

    @Specialization(guards = "main.getVal() == cachedMain.getVal()")
    @SuppressWarnings("unused")
    protected long doIntrinsic(VirtualFrame frame, StackPointer stackPointer, LLVMAddress main, long argc, LLVMAddress argv,
                    @Cached("main") LLVMAddress cachedMain,
                    @Cached("getMainDescriptor(cachedMain)") LLVMFunctionDescriptor mainDescriptor,
                    @Cached("getDispatchNode(mainDescriptor)") LLVMDispatchNode dispatchNode,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        sysInit(memory);
        dispatchNode.executeDispatch(frame, mainDescriptor, new Object[]{stackPointer});
        return 0;
    }

    @Specialization
    @SuppressWarnings("unused")
    public long executeGeneric(VirtualFrame frame, StackPointer stackPointer, LLVMAddress main, long argc, LLVMAddress argv,
                    @Cached("getLookupDispatchNode(main)") LLVMLookupDispatchNode dispatchNode,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        sysInit(memory);
        dispatchNode.executeDispatch(frame, main, new Object[]{stackPointer});
        return 0;
    }

    @Specialization(guards = "main == cachedMain")
    @SuppressWarnings("unused")
    protected long doIntrinsic(VirtualFrame frame, StackPointer stackPointer, LLVMFunctionDescriptor main, long argc, LLVMAddress argv,
                    @Cached("main") LLVMFunctionDescriptor cachedMain,
                    @Cached("getDispatchNode(main)") LLVMDispatchNode dispatchNode,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        sysInit(memory);
        dispatchNode.executeDispatch(frame, main, new Object[]{stackPointer});
        return 0;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected long doGeneric(VirtualFrame frame, StackPointer stackPointer, LLVMFunctionDescriptor main, long argc, LLVMAddress argv,
                    @Cached("getDispatchNode(main)") LLVMDispatchNode dispatchNode,
                    @Cached("getLLVMMemory()") LLVMMemory memory) {
        sysInit(memory);
        dispatchNode.executeDispatch(frame, main, new Object[]{stackPointer});
        return 0;
    }

    @TruffleBoundary
    private void sysInit(LLVMMemory memory) {
        LLVMContext context = getContextReference().get();
        Object[] args = context.getMainArguments();
        Object[] sysArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, sysArgs, 1, args.length);
        sysArgs[0] = context.getMainSourceFile().getPath();
        rustContext.sysInit(memory, context.getDataSpecConverter(), sysArgs);
        context.registerDestructorFunction(rustContextDestructorCallTarget);
    }

    @TruffleBoundary
    protected LLVMFunctionDescriptor getMainDescriptor(LLVMAddress main) {
        return getContextReference().get().getFunctionDescriptor(main);
    }

    protected LLVMDispatchNode getDispatchNode(LLVMFunctionDescriptor mainDescriptor) {
        CompilerAsserts.neverPartOfCompilation();
        return LLVMDispatchNodeGen.create(mainDescriptor.getType());
    }

    protected LLVMLookupDispatchNode getLookupDispatchNode(LLVMAddress main) {
        CompilerAsserts.neverPartOfCompilation();
        FunctionType functionType = getContextReference().get().getFunctionDescriptor(main).getType();
        return LLVMLookupDispatchNodeGen.create(functionType);
    }

    abstract static class RustContextDestructorNode extends RootNode {

        private final RustContext rustContext;

        RustContextDestructorNode(TruffleLanguage<?> language, RustContext rustContext) {
            super(language, new FrameDescriptor());
            this.rustContext = rustContext;
        }

        @Specialization
        public Object execute() {
            rustContext.dispose(getLanguage(LLVMLanguage.class).getCapability(LLVMMemory.class));
            return null;
        }

    }

}
