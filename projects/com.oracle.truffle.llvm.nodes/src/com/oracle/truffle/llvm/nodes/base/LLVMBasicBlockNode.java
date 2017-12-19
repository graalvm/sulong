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
package com.oracle.truffle.llvm.nodes.base;

import java.io.PrintStream;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.llvm.nodes.func.LLVMFunctionStartNode;
import com.oracle.truffle.llvm.runtime.GuestLanguageRuntimeException;
import com.oracle.truffle.llvm.runtime.LLVMContext.FrameSnapshot;
import com.oracle.truffle.llvm.runtime.LLVMLongjmpException;
import com.oracle.truffle.llvm.runtime.LLVMLongjmpTarget;
import com.oracle.truffle.llvm.runtime.LLVMSetjmpException;
import com.oracle.truffle.llvm.runtime.SulongRuntimeException;
import com.oracle.truffle.llvm.runtime.SulongStackTrace;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;

/**
 * This node represents a basic block in LLVM. The node contains both sequential statements which do
 * not change the control flow and terminator instructions which let the function return or continue
 * with another basic block.
 *
 * @see <a href="http://llvm.org/docs/LangRef.html#functions">basic blocks in LLVM IR</a>
 */
public class LLVMBasicBlockNode extends LLVMExpressionNode {

    public static final int RETURN_FROM_FUNCTION = -1;

    @Children private final LLVMExpressionNode[] statements;
    @Child public LLVMControlFlowNode termInstruction;

    private final int blockId;
    private final String blockName;

    private final BranchProfile controlFlowExceptionProfile = BranchProfile.create();
    private final BranchProfile blockEntered = BranchProfile.create();

    @CompilationFinal(dimensions = 1) private final long[] successorExecutionCount;

    @CompilationFinal private FrameSlot programCounter;
    @CompilationFinal private FrameSlot setjmpReturnValue;

    @CompilationFinal private String uniqueID;
    @CompilationFinal private long id;

    @CompilationFinal private int start = 0;

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        throw new UnsupportedOperationException("Must not be called.");
    }

    public LLVMBasicBlockNode(LLVMExpressionNode[] statements, LLVMControlFlowNode termInstruction, int blockId, String blockName) {
        this.statements = statements;
        this.termInstruction = termInstruction;
        this.blockId = blockId;
        this.blockName = blockName;
        successorExecutionCount = termInstruction.needsBranchProfiling() ? new long[termInstruction.getSuccessorCount()] : null;
    }

    private FrameSlot getPCSlot() {
        if (programCounter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            programCounter = getRootNode().getFrameDescriptor().findFrameSlot(LLVMLongjmpException.CURRENT_INSTRUCTION_FRAME_SLOT_ID);
        }
        return programCounter;
    }

    private FrameSlot getSetjmpReturnValueSlot() {
        if (setjmpReturnValue == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setjmpReturnValue = getRootNode().getFrameDescriptor().findFrameSlot(LLVMLongjmpException.SETJMP_RETURN_VALUE_FRAME_SLOT_ID);
        }
        return setjmpReturnValue;
    }

    public long getNodeId() {
        getUniqueID();
        return id;
    }

    public void setStart(int pc) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        start = pc;
    }

    private String getUniqueID() {
        if (uniqueID == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            LLVMFunctionStartNode f = NodeUtil.findParent(this, LLVMFunctionStartNode.class);
            uniqueID = f.getName() + ":" + getBlockId();
            id = getContextReference().get().getBlockID(uniqueID);
        }
        return uniqueID;
    }

    @ExplodeLoop
    public void executeStatements(VirtualFrame frame) {
        blockEntered.enter();
        CompilerAsserts.partialEvaluationConstant(start);
        for (int i = start; i < statements.length; i++) {
            LLVMExpressionNode statement = statements[i];
            try {
                if (traceEnabled()) {
                    trace(statement);
                }
                statement.executeGeneric(frame);
            } catch (LLVMSetjmpException e) {
                // restart, this time with target info
                frame.setObject(getPCSlot(), new LLVMLongjmpTarget(getNodeId(), i));
                try {
                    statement.executeGeneric(frame);
                } finally {
                    frame.setObject(getPCSlot(), null);
                }
            } catch (LLVMLongjmpException e) {
                LLVMLongjmpTarget target = e.getTarget();

                // restore old frame
                CompilerDirectives.transferToInterpreter();
                FrameSnapshot oldFrame = getContextReference().get().getSetjmpEnvironment(target.getHash());
                Object[] values = oldFrame.getValues();
                FrameDescriptor fields = frame.getFrameDescriptor();
                for (FrameSlot slot : fields.getSlots()) {
                    FrameSlot oldSlot = oldFrame.getFrameDescriptor().findFrameSlot(slot.getIdentifier());
                    if (oldSlot == null) { // slot did not exist
                        continue;
                    }
                    if (slot.getKind() != oldSlot.getKind()) { // slot changed type
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        slot.setKind(oldSlot.getKind());
                    }
                    int slotId = oldSlot.getIndex();
                    switch (slot.getKind()) {
                        case Boolean:
                            frame.setBoolean(slot, (boolean) values[slotId]);
                            break;
                        case Byte:
                            frame.setByte(slot, (byte) values[slotId]);
                            break;
                        case Int:
                            frame.setInt(slot, (int) values[slotId]);
                            break;
                        case Long:
                            frame.setLong(slot, (long) values[slotId]);
                            break;
                        case Float:
                            frame.setFloat(slot, (float) values[slotId]);
                            break;
                        case Double:
                            frame.setDouble(slot, (double) values[slotId]);
                            break;
                        case Object:
                            frame.setObject(slot, values[slotId]);
                            break;
                    }
                }

                if (target.is(getNodeId())) {
                    int val = e.getValue();
                    if (val == 0) {
                        val = 1;
                    }
                    frame.setInt(getSetjmpReturnValueSlot(), val);
                    i = target.getPC();
                    statement = statements[i];
                    statement.executeGeneric(frame);
                } else {
                    throw e;
                }
            } catch (ControlFlowException e) {
                controlFlowExceptionProfile.enter();
                throw e;
            } catch (GuestLanguageRuntimeException e) {
                CompilerDirectives.transferToInterpreter();
                throw e;
            } catch (SulongRuntimeException e) {
                CompilerDirectives.transferToInterpreter();
                fillStackTrace(e.getCStackTrace(), i);
                throw e;
            } catch (Throwable t) {
                CompilerDirectives.transferToInterpreter();
                final SulongStackTrace stackTrace = new SulongStackTrace(t.getMessage());
                fillStackTrace(stackTrace, i);
                throw new SulongRuntimeException(t, stackTrace);
            } finally {
                start = 0;
            }
        }
    }

    private void fillStackTrace(SulongStackTrace stackTrace, int errorIndex) {
        final LLVMSourceLocation loc = getLastAvailableSourceLocation(errorIndex);
        final LLVMFunctionStartNode f = NodeUtil.findParent(this, LLVMFunctionStartNode.class);
        stackTrace.addStackTraceElement(f.getOriginalName(), loc, f.getName(), f.getBcSource().getName(), blockName());
    }

    private LLVMSourceLocation getLastAvailableSourceLocation(int i) {
        CompilerAsserts.neverPartOfCompilation();
        for (int j = i; j >= 0; j--) {
            LLVMExpressionNode node = statements[j];
            if (node instanceof InstrumentableFactory.WrapperNode) {
                node = (LLVMExpressionNode) ((InstrumentableFactory.WrapperNode) node).getDelegateNode();
            }

            LLVMSourceLocation location = node.getSourceLocation();
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    public int getBlockId() {
        return blockId;
    }

    public String getBlockName() {
        return blockName;
    }

    @CompilationFinal private boolean traceEnabledFlag;
    @CompilationFinal private PrintStream traceStream;

    private void cacheTrace() {
        if (traceStream == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            traceStream = SulongEngineOption.getStream(getContextReference().get().getEnv().getOptions().get(SulongEngineOption.DEBUG));
            traceEnabledFlag = SulongEngineOption.isTrue(getContextReference().get().getEnv().getOptions().get(SulongEngineOption.DEBUG));
        }
    }

    private boolean traceEnabled() {
        cacheTrace();
        return traceEnabledFlag;
    }

    private PrintStream traceStream() {
        cacheTrace();
        return traceStream;
    }

    @TruffleBoundary
    private void trace(LLVMExpressionNode statement) {
        traceStream().println(("[sulong] " + statement.getSourceDescription()));
    }

    @Override
    public String getSourceDescription() {
        LLVMFunctionStartNode functionStartNode = NodeUtil.findParent(this, LLVMFunctionStartNode.class);
        assert functionStartNode != null : getParent().getClass();
        return String.format("Function: %s - Block: %s", functionStartNode.getBcName(), blockName());
    }

    private String blockName() {
        return String.format("id: %d name: %s", blockId, blockName == null ? "N/A" : blockName);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return String.format("basic block %s (#statements: %s, terminating instruction: %s)", blockId, statements.length, termInstruction);
    }

    /**
     * Gets the branch probability of the given successor.
     *
     * @param successorIndex
     * @return the probability between 0 and 1
     */
    @ExplodeLoop
    public double getBranchProbability(int successorIndex) {
        assert termInstruction.needsBranchProfiling();
        double successorBranchProbability;

        /*
         * It is possible to get race conditions (compiler and AST interpeter thread). This avoids a
         * probability > 1.
         *
         * We make sure that we read each element only once. We also make sure that the compiler reduces the
         * conditions to constants.
         */
        long succCount = 0;
        long totalExecutionCount = 0;
        for (int i = 0; i < successorExecutionCount.length; i++) {
            long v = successorExecutionCount[i];
            if (successorIndex == i) {
                succCount = v;
            }
            totalExecutionCount += v;
        }
        if (succCount == 0) {
            successorBranchProbability = 0;
        } else {
            assert totalExecutionCount > 0;
            successorBranchProbability = (double) succCount / totalExecutionCount;
        }
        assert !Double.isNaN(successorBranchProbability) && successorBranchProbability >= 0 && successorBranchProbability <= 1;
        return successorBranchProbability;
    }

    public void increaseBranchProbability(int successorIndex) {
        CompilerAsserts.neverPartOfCompilation();
        if (termInstruction.needsBranchProfiling()) {
            incrementCountAtIndex(successorIndex);
        }
    }

    private void incrementCountAtIndex(int successorIndex) {
        assert termInstruction.needsBranchProfiling();
        successorExecutionCount[successorIndex]++;
    }
}
