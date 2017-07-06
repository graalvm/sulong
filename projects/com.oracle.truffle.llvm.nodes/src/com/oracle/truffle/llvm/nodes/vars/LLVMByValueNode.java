package com.oracle.truffle.llvm.nodes.vars;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public class LLVMByValueNode extends LLVMExpressionNode {

    @Child private LLVMExpressionNode address;

    final int size;
    final int alignment;

    public LLVMByValueNode(LLVMExpressionNode address, int size, int alignment) {
        this.address = address;
        this.size = size + 8; // TODO: the size of the stack frame is to small?
        this.alignment = alignment;
    }

    @Override
    @Specialization
    public LLVMAddress executeLLVMAddress(VirtualFrame frame) {
        try {
            LLVMAddress addr = address.executeLLVMAddress(frame);

            FrameSlot stackPointer = getRootNode().getFrameDescriptor().findFrameSlot(LLVMStack.FRAME_ID);
            LLVMAddress newAddr = LLVMAddress.fromLong(LLVMStack.allocateStackMemory(frame, stackPointer, size, alignment));
            LLVMMemory.copyMemory(addr.getVal(), newAddr.getVal(), size);

            return newAddr;
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeLLVMAddress(frame);
    }
}
