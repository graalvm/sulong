/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.factories;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.asm.amd64.Parser;
import com.oracle.truffle.llvm.nodes.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.base.LLVMFrameNuller;
import com.oracle.truffle.llvm.nodes.base.LLVMMainFunctionReturnValueRootNode;
import com.oracle.truffle.llvm.nodes.control.LLVMBrUnconditionalNode;
import com.oracle.truffle.llvm.nodes.control.LLVMConditionalBranchNode;
import com.oracle.truffle.llvm.nodes.control.LLVMDispatchBasicBlockNode;
import com.oracle.truffle.llvm.nodes.control.LLVMIndirectBranchNode;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVM80BitFloatRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMAddressRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMDoubleRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMFloatRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMFunctionRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI16RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI1RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI32RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI64RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI8RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMIVarBitRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMStructRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMVectorRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMVoidReturnNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMSwitchNode.LLVMSwitchNodeImpl;
import com.oracle.truffle.llvm.nodes.control.LLVMWritePhisNode;
import com.oracle.truffle.llvm.nodes.func.LLVMArgNodeGen;
import com.oracle.truffle.llvm.nodes.func.LLVMCallNode;
import com.oracle.truffle.llvm.nodes.func.LLVMFunctionStartNode;
import com.oracle.truffle.llvm.nodes.func.LLVMGlobalRootNode;
import com.oracle.truffle.llvm.nodes.func.LLVMInlineAssemblyRootNode;
import com.oracle.truffle.llvm.nodes.func.LLVMInvokeNode;
import com.oracle.truffle.llvm.nodes.func.LLVMLandingpadNode;
import com.oracle.truffle.llvm.nodes.func.LLVMResumeNode;
import com.oracle.truffle.llvm.nodes.func.LLVMTypeIdForExceptionNode;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory.LLVMFAbsNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory.LLVMPowNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMTruffleGetArgNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMAssumeNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapV2I64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapV4I32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapV8I16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI1NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMFrameAddressNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMI64ObjectSizeNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMInvariantEndNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMInvariantStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMLifetimeEndNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMLifetimeStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemCopyNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemMoveFactory.LLVMMemMoveI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemSetNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMNoOpNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMPrefetchNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMReturnAddressNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMStackRestoreNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMStackSaveNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMTrapNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmetic;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.GCCArithmeticNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.LLVMArithmeticWithOverflowAndCarryNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.LLVMArithmeticWithOverflowNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMComplexDivSC;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI8NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountSetBitsNodeFactory.CountSetBitsI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountSetBitsNodeFactory.CountSetBitsI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI8NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugBuilder;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugFrameWriteNodeFactory;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMToDebugValueNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64BitVACopyNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64BitVAEnd;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64VAStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_ConversionNodeFactory.LLVMX86_ConversionDoubleToIntNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_ConversionNodeFactory.LLVMX86_ConversionFloatToIntNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMFunctionLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMFunctionLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVM80BitFloatLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMAddressLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMDoubleLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMFloatLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI16LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI1LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI32LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI64LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI8LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMIVarBitLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMTruffleObjectLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorAddressLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorDoubleLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorFloatLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorFunctionLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI16LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI1LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI32LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI64LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI8LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMAddressGetElementPtrNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMAllocInstruction.LLVMAllocaConstInstruction;
import com.oracle.truffle.llvm.nodes.memory.LLVMAllocInstructionFactory.LLVMAllocaConstInstructionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMAllocInstructionFactory.LLVMAllocaInstructionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMCompareExchangeNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMFenceNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMInsertValueNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMNativeStackAllocationNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMStructByValueNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMVarArgCompoundAddressNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeAllocateStringNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeMemSetNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeProfiledMemMoveNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVM80BitFloatArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMAddressArrayLiteralNode;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMAddressArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMDoubleArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMFloatArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMFunctionArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI16ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI32ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI64ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI8ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMStructArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVM80BitFloatLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNode.LLVMGlobalDirectLoadNode;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVM80BitFloatDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMAddressDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMFunctionDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMIVarBitDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMStructDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDoubleLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMFloatLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI16LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI1LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI32LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI64LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI8LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadExpressionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadNode;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadAddressVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadDoubleVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadFloatVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI16VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI1VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI32VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI64VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI8VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI16RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI1RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI32RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI64RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI8RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.store.LLVM80BitFloatStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMAddressStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMDoubleStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMFloatStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMFunctionStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMGlobalVariableStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI16StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI1StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI32StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI64StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI8StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMIVarBitStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStoreExpressionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStoreNode;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStoreVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStructStoreNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMAddressCompareNode;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMAddNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMDivNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMMulNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMRemNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMSubNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMUDivNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMURemNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMEqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMNqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOeqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOneNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOrdNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUeqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUneNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUnoNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMXorNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMAccessGlobalVariableStorageNode;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVM80BitFloatSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMAddressSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMDoubleSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMFloatSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMFunctionSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI16SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI1SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI32SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI64SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI8SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMStaticInitsBlockNode;
import com.oracle.truffle.llvm.nodes.others.LLVMUnreachableNode;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMAddressProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMDoubleProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMFloatProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI16ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI1ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI32ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI64ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI8ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMAddressVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMDoubleVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMFloatVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI16VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI1VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI32VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI64VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI8VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVM80BitFloatReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMAddressReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMDoubleReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMFloatReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMFunctionReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI16ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI1ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI32ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI64ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI8ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMIReadVarBitNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMAddressVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMDoubleVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMFloatVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI16VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI1VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI32VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI64VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI8VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMSetInteropTypeNode;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWrite80BitFloatingNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteAddressNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteDoubleNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteFloatNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteFunctionNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI16NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI1NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI32NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI64NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI8NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteIVarBitNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vars.StructLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMDoubleExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMFloatExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI16ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI1ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI32ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI64ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI8ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMDoubleInsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMFloatInsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI16InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI1InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI32InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI64InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI8InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleDoubleVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleFloatVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI32VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI64VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI8VectorNodeGen;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.NodeFactory;
import com.oracle.truffle.llvm.parser.instructions.LLVMArithmeticInstructionType;
import com.oracle.truffle.llvm.parser.instructions.LLVMConversionType;
import com.oracle.truffle.llvm.parser.instructions.LLVMLogicalInstructionKind;
import com.oracle.truffle.llvm.parser.metadata.debuginfo.DebugInfoGenerator;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.KnownAttribute;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.enums.Flag;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.ReadModifyWriteOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalValueSymbol;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalVariable;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException.UnsupportedReason;
import com.oracle.truffle.llvm.runtime.NFIContextExtension;
import com.oracle.truffle.llvm.runtime.debug.LLVMDebugValue;
import com.oracle.truffle.llvm.runtime.debug.LLVMDebugValueProvider;
import com.oracle.truffle.llvm.runtime.debug.LLVMSourceType;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobal;
import com.oracle.truffle.llvm.runtime.memory.LLVMAllocateStringNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemMoveNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemSetNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMStackAllocationNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public class BasicNodeFactory implements NodeFactory {

    @Override
    public LLVMExpressionNode createInsertElement(LLVMParserRuntime runtime, Type resultType, LLVMExpressionNode vector, LLVMExpressionNode element,
                    LLVMExpressionNode index) {
        VectorType resultType1 = (VectorType) resultType;
        if (!(resultType1.getElementType() instanceof PrimitiveType)) {
            throw new AssertionError(resultType1);
        }
        switch (((PrimitiveType) resultType1.getElementType()).getPrimitiveKind()) {
            case I1:
                return LLVMI1InsertElementNodeGen.create(vector, element, index);
            case I8:
                return LLVMI8InsertElementNodeGen.create(vector, element, index);
            case I16:
                return LLVMI16InsertElementNodeGen.create(vector, element, index);
            case I32:
                return LLVMI32InsertElementNodeGen.create(vector, element, index);
            case I64:
                return LLVMI64InsertElementNodeGen.create(vector, element, index);
            case FLOAT:
                return LLVMFloatInsertElementNodeGen.create(vector, element, index);
            case DOUBLE:
                return LLVMDoubleInsertElementNodeGen.create(vector, element, index);
            default:
                throw new AssertionError("vector type " + resultType1 + "  not supported!");
        }
    }

    @Override
    public LLVMExpressionNode createExtractElement(LLVMParserRuntime runtime, Type resultType, LLVMExpressionNode vector, LLVMExpressionNode index) {
        PrimitiveType resultType1 = (PrimitiveType) resultType;
        switch (resultType1.getPrimitiveKind()) {
            case I1:
                return LLVMI1ExtractElementNodeGen.create(vector, index);
            case I8:
                return LLVMI8ExtractElementNodeGen.create(vector, index);
            case I16:
                return LLVMI16ExtractElementNodeGen.create(vector, index);
            case I32:
                return LLVMI32ExtractElementNodeGen.create(vector, index);
            case I64:
                return LLVMI64ExtractElementNodeGen.create(vector, index);
            case FLOAT:
                return LLVMFloatExtractElementNodeGen.create(vector, index);
            case DOUBLE:
                return LLVMDoubleExtractElementNodeGen.create(vector, index);
            default:
                throw new AssertionError(resultType1 + " not supported!");
        }
    }

    @Override
    public LLVMExpressionNode createShuffleVector(LLVMParserRuntime runtime, Type llvmType, LLVMExpressionNode vector1, LLVMExpressionNode vector2,
                    LLVMExpressionNode mask) {
        VectorType resultType = (VectorType) llvmType;
        if (!(resultType.getElementType() instanceof PrimitiveType)) {
            throw new AssertionError(resultType);
        }
        switch (((PrimitiveType) resultType.getElementType()).getPrimitiveKind()) {
            case I8:
                return LLVMShuffleI8VectorNodeGen.create(vector1, vector2, mask);
            case I32:
                return LLVMShuffleI32VectorNodeGen.create(vector1, vector2, mask);
            case I64:
                return LLVMShuffleI64VectorNodeGen.create(vector1, vector2, mask);
            case FLOAT:
                return LLVMShuffleFloatVectorNodeGen.create(vector1, vector2, mask);
            case DOUBLE:
                return LLVMShuffleDoubleVectorNodeGen.create(vector1, vector2, mask);
            default:
                throw new AssertionError(resultType);
        }
    }

    @Override
    public LLVMExpressionNode createLoad(LLVMParserRuntime runtime, Type resolvedResultType, LLVMExpressionNode loadTarget) {
        if (resolvedResultType instanceof VectorType) {
            return createLoadVector((VectorType) resolvedResultType, loadTarget, ((VectorType) resolvedResultType).getNumberOfElements());
        } else {
            int bits = resolvedResultType instanceof VariableBitWidthType
                            ? resolvedResultType.getBitSize()
                            : 0;
            return createLoad(resolvedResultType, loadTarget, bits);
        }
    }

    private static LLVMExpressionNode createLoadVector(VectorType resultType, LLVMExpressionNode loadTarget, int size) {
        Type elemType = resultType.getElementType();
        LLVMLoadNode load;
        if (elemType instanceof PrimitiveType) {

            switch (((PrimitiveType) elemType).getPrimitiveKind()) {
                case I1:
                    load = LLVMLoadI1VectorNodeGen.create(size);
                    break;
                case I8:
                    load = LLVMLoadI8VectorNodeGen.create(size);
                    break;
                case I16:
                    load = LLVMLoadI16VectorNodeGen.create(size);
                    break;
                case I32:
                    load = LLVMLoadI32VectorNodeGen.create(size);
                    break;
                case I64:
                    load = LLVMLoadI64VectorNodeGen.create(size);
                    break;
                case FLOAT:
                    load = LLVMLoadFloatVectorNodeGen.create(size);
                    break;
                case DOUBLE:
                    load = LLVMLoadDoubleVectorNodeGen.create(size);
                    break;
                default:
                    throw new AssertionError(elemType + " vectors not supported");
            }
        } else if (elemType instanceof PointerType) {
            load = LLVMLoadAddressVectorNodeGen.create(size);
        } else {
            throw new AssertionError(elemType + " vectors not supported");
        }
        return LLVMLoadExpressionNodeGen.create(load, loadTarget);
    }

    @Override
    public LLVMExpressionNode createStore(LLVMParserRuntime runtime, LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type, SourceSection source) {
        return createStore(pointerNode, valueNode, type, runtime.getContext().getByteSize(type), source);
    }

    @Override
    public LLVMExpressionNode createReadModifyWrite(LLVMParserRuntime runtime, ReadModifyWriteOperator operator, LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type) {
        if (type instanceof PrimitiveType) {
            switch (operator) {
                case XCHG:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWXchgNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case ADD:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWAddNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWAddNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWAddNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWAddNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWAddNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case SUB:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWSubNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWSubNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWSubNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWSubNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWSubNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case AND:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWAndNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWAndNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWAndNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWAndNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWAndNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case NAND:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWNandNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWNandNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWNandNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWNandNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWNandNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case OR:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWOrNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWOrNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWOrNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWOrNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWOrNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case XOR:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWXorNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWXorNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWXorNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWXorNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWXorNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case MAX:
                case MIN:
                case UMAX:
                case UMIN:
                default:
                    throw new AssertionError(operator);
            }
        } else {
            throw new AssertionError(type);
        }
    }

    @Override
    public LLVMExpressionNode createFence(LLVMParserRuntime runtime) {
        return LLVMFenceNodeGen.create();
    }

    @Override
    public LLVMExpressionNode createLogicalOperation(LLVMParserRuntime runtime, LLVMExpressionNode left, LLVMExpressionNode right, LLVMLogicalInstructionKind type, Type llvmType, Flag[] flags) {
        switch (type) {
            case AND:
                return LLVMAndNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMAshrNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMLshrNodeGen.create(left, right);
            case OR:
                return LLVMOrNodeGen.create(left, right);
            case SHIFT_LEFT:
                return LLVMShlNodeGen.create(left, right);
            case XOR:
                return LLVMXorNodeGen.create(left, right);
            default:
                throw new AssertionError(llvmType);
        }
    }

    @Override
    public LLVMExpressionNode createSimpleConstantNoArray(LLVMParserRuntime runtime, Object constant, Type type) {
        if (Type.isFunctionOrFunctionPointer(type)) {
            if (constant == null) {
                LLVMFunctionDescriptor functionDescriptor = runtime.getContext().getFunctionDescriptor(LLVMAddress.fromLong(0));
                return LLVMFunctionLiteralNodeGen.create(functionDescriptor);
            } else {
                throw new AssertionError("Not a Simple Constant: " + constant);
            }
        } else if (type instanceof VariableBitWidthType) {
            Number c = (Number) constant;
            if (type.getBitSize() <= Long.SIZE) {
                return new LLVMIVarBitLiteralNode(LLVMIVarBit.fromLong(type.getBitSize(), c.longValue()));
            } else {
                return new LLVMIVarBitLiteralNode(LLVMIVarBit.fromBigInteger(type.getBitSize(), (BigInteger) c));
            }
        } else if (type instanceof PointerType) {
            if (constant == null) {
                return new LLVMAddressLiteralNode(LLVMAddress.fromLong(0));
            } else {
                throw new AssertionError("Not a Simple Constant: " + constant);
            }
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return new LLVMI1LiteralNode((boolean) constant);
                case I8:
                    return new LLVMI8LiteralNode((byte) constant);
                case I16:
                    return new LLVMI16LiteralNode((short) constant);
                case I32:
                    return new LLVMI32LiteralNode((int) constant);
                case FLOAT:
                    return new LLVMFloatLiteralNode((float) constant);
                case DOUBLE:
                    return new LLVMDoubleLiteralNode((double) constant);
                case X86_FP80:
                    if (constant == null) {
                        return new LLVM80BitFloatLiteralNode(LLVM80BitFloat.fromLong(0));
                    } else {
                        return new LLVM80BitFloatLiteralNode(LLVM80BitFloat.fromBytesBigEndian((byte[]) constant));
                    }
                case I64:
                    return new LLVMI64LiteralNode((long) constant);
                default:
                    throw new AssertionError(type);
            }
        } else if (type == MetaType.DEBUG) {
            return new LLVMAddressLiteralNode(LLVMAddress.nullPointer());
        } else {
            throw new AssertionError(type);
        }
    }

    @Override
    public LLVMExpressionNode createVectorLiteralNode(LLVMParserRuntime runtime, List<LLVMExpressionNode> listValues, Type type) {
        LLVMExpressionNode[] vals = listValues.toArray(new LLVMExpressionNode[listValues.size()]);
        Type llvmType = ((VectorType) type).getElementType();
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMVectorI1LiteralNodeGen.create(vals);
                case I8:
                    return LLVMVectorI8LiteralNodeGen.create(vals);
                case I16:
                    return LLVMVectorI16LiteralNodeGen.create(vals);
                case I32:
                    return LLVMVectorI32LiteralNodeGen.create(vals);
                case I64:
                    return LLVMVectorI64LiteralNodeGen.create(vals);
                case FLOAT:
                    return LLVMVectorFloatLiteralNodeGen.create(vals);
                case DOUBLE:
                    return LLVMVectorDoubleLiteralNodeGen.create(vals);
                default:
                    throw new AssertionError();
            }
        } else if (llvmType instanceof PointerType) {
            if (((PointerType) llvmType).getPointeeType() instanceof FunctionType) {
                return LLVMVectorFunctionLiteralNodeGen.create(vals);
            } else {
                return LLVMVectorAddressLiteralNodeGen.create(vals);
            }
        } else {
            throw new AssertionError(llvmType + " not yet supported");
        }
    }

    @Override
    public LLVMFrameNuller createFrameNuller(FrameSlot slot) {
        return new LLVMFrameNuller(slot);
    }

    @Override
    public LLVMControlFlowNode createRetVoid(LLVMParserRuntime runtime, SourceSection source) {
        return LLVMVoidReturnNodeGen.create(source);
    }

    @Override
    public LLVMControlFlowNode createNonVoidRet(LLVMParserRuntime runtime, LLVMExpressionNode retValue, Type type, SourceSection source) {
        if (retValue == null) {
            throw new AssertionError();
        }
        if (type instanceof VectorType) {
            return LLVMVectorRetNodeGen.create(source, retValue);
        } else if (type instanceof VariableBitWidthType) {
            return LLVMIVarBitRetNodeGen.create(source, retValue);
        } else if (Type.isFunctionOrFunctionPointer(type)) {
            return LLVMFunctionRetNodeGen.create(source, retValue);
        } else if (type instanceof PointerType) {
            return LLVMAddressRetNodeGen.create(source, retValue);
        } else if (type instanceof StructureType) {
            int size = runtime.getContext().getByteSize(type);
            return LLVMStructRetNodeGen.create(source, createMemMove(), retValue, size);
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return LLVMI1RetNodeGen.create(source, retValue);
                case I8:
                    return LLVMI8RetNodeGen.create(source, retValue);
                case I16:
                    return LLVMI16RetNodeGen.create(source, retValue);
                case I32:
                    return LLVMI32RetNodeGen.create(source, retValue);
                case I64:
                    return LLVMI64RetNodeGen.create(source, retValue);
                case FLOAT:
                    return LLVMFloatRetNodeGen.create(source, retValue);
                case DOUBLE:
                    return LLVMDoubleRetNodeGen.create(source, retValue);
                case X86_FP80:
                    return LLVM80BitFloatRetNodeGen.create(source, retValue);
                default:
                    throw new AssertionError(type);
            }
        }
        throw new AssertionError(type);
    }

    @Override
    public LLVMExpressionNode createFunctionArgNode(int argIndex, Type paramType) {
        if (argIndex < 0) {
            throw new AssertionError();
        }
        LLVMExpressionNode argNode = LLVMArgNodeGen.create(argIndex);
        if (argIndex < LLVMCallNode.USER_ARGUMENT_OFFSET) {
            // Do not profile the stackpointer
            return argNode;
        }
        if (paramType instanceof PrimitiveType) {
            switch (((PrimitiveType) paramType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ProfiledValueNodeGen.create(argNode);
                case I32:
                    return LLVMI32ProfiledValueNodeGen.create(argNode);
                case I64:
                    return LLVMI64ProfiledValueNodeGen.create(argNode);
                case FLOAT:
                    return LLVMFloatProfiledValueNodeGen.create(argNode);
                case DOUBLE:
                    return LLVMDoubleProfiledValueNodeGen.create(argNode);
                case I1:
                    return LLVMI1ProfiledValueNodeGen.create(argNode);
                case I16:
                    return LLVMI16ProfiledValueNodeGen.create(argNode);
                default:
                    return argNode;
            }
        } else if (paramType instanceof PointerType) {
            return LLVMAddressProfiledValueNodeGen.create(argNode);
        } else {
            return argNode;
        }
    }

    @Override
    public LLVMExpressionNode createFunctionCall(LLVMParserRuntime runtime, LLVMExpressionNode functionNode, LLVMExpressionNode[] argNodes, FunctionType type, SourceSection sourceSection) {
        LLVMExpressionNode callNode = new LLVMCallNode(type, functionNode, argNodes, sourceSection);
        if (type.getReturnType() instanceof PrimitiveType) {
            switch (((PrimitiveType) type.getReturnType()).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ProfiledValueNodeGen.create(callNode);
                case I32:
                    return LLVMI32ProfiledValueNodeGen.create(callNode);
                case I64:
                    return LLVMI64ProfiledValueNodeGen.create(callNode);
                case FLOAT:
                    return LLVMFloatProfiledValueNodeGen.create(callNode);
                case DOUBLE:
                    return LLVMDoubleProfiledValueNodeGen.create(callNode);
                case I1:
                    return LLVMI1ProfiledValueNodeGen.create(callNode);
                case I16:
                    return LLVMI16ProfiledValueNodeGen.create(callNode);
                default:
                    return callNode;
            }
        } else if (type.getReturnType() instanceof PointerType) {
            return LLVMAddressProfiledValueNodeGen.create(callNode);
        } else {
            return callNode;
        }
    }

    @Override
    public LLVMExpressionNode createFrameRead(LLVMParserRuntime runtime, Type llvmType, FrameSlot frameSlot) {
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMI1ReadNodeGen.create(frameSlot);
                case I8:
                    return LLVMI8ReadNodeGen.create(frameSlot);
                case I16:
                    return LLVMI16ReadNodeGen.create(frameSlot);
                case I32:
                    return LLVMI32ReadNodeGen.create(frameSlot);
                case I64:
                    return LLVMI64ReadNodeGen.create(frameSlot);
                case FLOAT:
                    return LLVMFloatReadNodeGen.create(frameSlot);
                case DOUBLE:
                    return LLVMDoubleReadNodeGen.create(frameSlot);
                case X86_FP80:
                    return LLVM80BitFloatReadNodeGen.create(frameSlot);
            }
        } else if (llvmType instanceof VectorType) {
            Type elemType = ((VectorType) llvmType).getElementType();
            if (elemType instanceof PrimitiveType) {
                switch (((PrimitiveType) elemType).getPrimitiveKind()) {
                    case I1:
                        return LLVMI1VectorReadNodeGen.create(frameSlot);
                    case I8:
                        return LLVMI8VectorReadNodeGen.create(frameSlot);
                    case I16:
                        return LLVMI16VectorReadNodeGen.create(frameSlot);
                    case I32:
                        return LLVMI32VectorReadNodeGen.create(frameSlot);
                    case I64:
                        return LLVMI64VectorReadNodeGen.create(frameSlot);
                    case FLOAT:
                        return LLVMFloatVectorReadNodeGen.create(frameSlot);
                    case DOUBLE:
                        return LLVMDoubleVectorReadNodeGen.create(frameSlot);
                }
            } else if (elemType instanceof PointerType) {
                return LLVMAddressVectorReadNodeGen.create(frameSlot);
            }
        } else if (llvmType instanceof VariableBitWidthType) {
            return LLVMIReadVarBitNodeGen.create(frameSlot);
        } else if (Type.isFunctionOrFunctionPointer(llvmType)) {
            return LLVMFunctionReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof PointerType) {
            return LLVMAddressReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof StructureType || llvmType instanceof ArrayType) {
            return LLVMAddressReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof VoidType) {
            throw new LLVMUnsupportedException(UnsupportedReason.PARSER_ERROR_VOID_SLOT);
        } else if (llvmType == MetaType.DEBUG) {
            return LLVMReadNodeFactory.LLVMDebugReadNodeGen.create(frameSlot);
        }
        throw new AssertionError(llvmType + " for " + frameSlot.getIdentifier());
    }

    @Override
    public LLVMExpressionNode createFrameWrite(LLVMParserRuntime runtime, Type llvmType, LLVMExpressionNode result, FrameSlot slot, SourceSection sourceSection) {
        if (llvmType instanceof VectorType) {
            return LLVMWriteVectorNodeGen.create(sourceSection, result, slot);
        } else if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMWriteI1NodeGen.create(result, slot, sourceSection);
                case I8:
                    return LLVMWriteI8NodeGen.create(result, slot, sourceSection);
                case I16:
                    return LLVMWriteI16NodeGen.create(result, slot, sourceSection);
                case I32:
                    return LLVMWriteI32NodeGen.create(result, slot, sourceSection);
                case I64:
                    return LLVMWriteI64NodeGen.create(result, slot, sourceSection);
                case FLOAT:
                    return LLVMWriteFloatNodeGen.create(result, slot, sourceSection);
                case DOUBLE:
                    return LLVMWriteDoubleNodeGen.create(result, slot, sourceSection);
                case X86_FP80:
                    return LLVMWrite80BitFloatingNodeGen.create(result, slot, sourceSection);
                default:
                    throw new AssertionError(llvmType);
            }
        } else if (llvmType instanceof VariableBitWidthType) {
            return LLVMWriteIVarBitNodeGen.create(result, slot, sourceSection);
        } else if (Type.isFunctionOrFunctionPointer(llvmType)) {
            return LLVMWriteFunctionNodeGen.create(result, slot, sourceSection);
        } else if (llvmType instanceof PointerType) {
            return LLVMWriteAddressNodeGen.create(result, slot, sourceSection);
        } else if (llvmType instanceof StructureType || llvmType instanceof ArrayType) {
            return LLVMWriteAddressNodeGen.create(result, slot, sourceSection);
        }
        throw new AssertionError(llvmType);
    }

    @Override
    public LLVMExpressionNode createComparison(LLVMParserRuntime runtime, CompareOperator operator, Type type, LLVMExpressionNode lhs, LLVMExpressionNode rhs) {
        if (Type.isFunctionOrFunctionPointer(type) || type instanceof PointerType) {
            switch (operator) {
                case INT_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.EQ, lhs, rhs);
                case INT_NOT_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.NEQ, lhs, rhs);
                case INT_UNSIGNED_GREATER_THAN:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.UGT, lhs, rhs);
                case INT_UNSIGNED_GREATER_OR_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.UGE, lhs, rhs);
                case INT_UNSIGNED_LESS_THAN:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.ULT, lhs, rhs);
                case INT_UNSIGNED_LESS_OR_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.ULE, lhs, rhs);
                case INT_SIGNED_GREATER_THAN:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.SGT, lhs, rhs);
                case INT_SIGNED_GREATER_OR_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.SGE, lhs, rhs);
                case INT_SIGNED_LESS_THAN:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.SLT, lhs, rhs);
                case INT_SIGNED_LESS_OR_EQUAL:
                    return LLVMAddressCompareNode.create(LLVMAddressCompareNode.Kind.SLE, lhs, rhs);
                default:
                    throw new AssertionError(operator);
            }
        }
        switch (operator) {
            case FP_FALSE:
                return new LLVMI1LiteralNode(false);
            case FP_ORDERED_EQUAL:
                return LLVMOeqNodeGen.create(lhs, rhs);
            case FP_ORDERED_GREATER_THAN:
                return LLVMOgtNodeGen.create(lhs, rhs);
            case FP_ORDERED_GREATER_OR_EQUAL:
                return LLVMOgeNodeGen.create(lhs, rhs);
            case FP_ORDERED_LESS_THAN:
                return LLVMOltNodeGen.create(lhs, rhs);
            case FP_ORDERED_LESS_OR_EQUAL:
                return LLVMOleNodeGen.create(lhs, rhs);
            case FP_ORDERED_NOT_EQUAL:
                return LLVMOneNodeGen.create(lhs, rhs);
            case FP_ORDERED:
                return LLVMOrdNodeGen.create(lhs, rhs);
            case FP_UNORDERED:
                return LLVMUnoNodeGen.create(lhs, rhs);
            case FP_UNORDERED_EQUAL:
                return LLVMUeqNodeGen.create(lhs, rhs);
            case FP_UNORDERED_GREATER_THAN:
                return LLVMUgtNodeGen.create(lhs, rhs);
            case FP_UNORDERED_GREATER_OR_EQUAL:
                return LLVMUgeNodeGen.create(lhs, rhs);
            case FP_UNORDERED_LESS_THAN:
                return LLVMUltNodeGen.create(lhs, rhs);
            case FP_UNORDERED_LESS_OR_EQUAL:
                return LLVMUleNodeGen.create(lhs, rhs);
            case FP_UNORDERED_NOT_EQUAL:
                return LLVMUneNodeGen.create(lhs, rhs);
            case FP_TRUE:
                return new LLVMI1LiteralNode(true);
            case INT_EQUAL:
                return LLVMEqNodeGen.create(lhs, rhs);
            case INT_NOT_EQUAL:
                return LLVMNqNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_GREATER_THAN:
                return LLVMUgtNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_GREATER_OR_EQUAL:
                return LLVMUgeNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_LESS_THAN:
                return LLVMUltNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_LESS_OR_EQUAL:
                return LLVMUleNodeGen.create(lhs, rhs);
            case INT_SIGNED_GREATER_THAN:
                return LLVMSgtNodeGen.create(lhs, rhs);
            case INT_SIGNED_GREATER_OR_EQUAL:
                return LLVMSgeNodeGen.create(lhs, rhs);
            case INT_SIGNED_LESS_THAN:
                return LLVMSltNodeGen.create(lhs, rhs);
            case INT_SIGNED_LESS_OR_EQUAL:
                return LLVMSleNodeGen.create(lhs, rhs);
            default:
                throw new RuntimeException("Missed a compare operator");
        }
    }

    @Override
    public LLVMExpressionNode createCast(LLVMParserRuntime runtime, LLVMExpressionNode fromNode, Type targetType, Type fromType, LLVMConversionType type) {
        return LLVMCastsFactory.cast(fromNode, targetType, fromType, type);
    }

    @Override
    public LLVMExpressionNode createArithmeticOperation(LLVMParserRuntime runtime, LLVMExpressionNode left, LLVMExpressionNode right, LLVMArithmeticInstructionType type, Type llvmType, Flag[] flags) {
        switch (type) {
            case ADDITION:
                return LLVMAddNodeGen.create(left, right);
            case SUBTRACTION:
                return LLVMSubNodeGen.create(left, right);
            case MULTIPLICATION:
                return LLVMMulNodeGen.create(left, right);
            case DIVISION:
                return LLVMDivNodeGen.create(left, right);
            case REMAINDER:
                return LLVMRemNodeGen.create(left, right);
            case UNSIGNED_REMAINDER:
                return LLVMURemNodeGen.create(left, right);
            case UNSIGNED_DIVISION:
                return LLVMUDivNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    @Override
    public LLVMExpressionNode createExtractValue(LLVMParserRuntime runtime, Type type, LLVMExpressionNode targetAddress) {
        LLVMLoadNode load;
        if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    load = LLVMI1LoadNodeGen.create();
                    break;
                case I8:
                    load = LLVMI8LoadNodeGen.create();
                    break;
                case I16:
                    load = LLVMI16LoadNodeGen.create();
                    break;
                case I32:
                    load = LLVMI32LoadNodeGen.create();
                    break;
                case I64:
                    load = LLVMI64LoadNodeGen.create();
                    break;
                case FLOAT:
                    load = LLVMFloatLoadNodeGen.create();
                    break;
                case DOUBLE:
                    load = LLVMDoubleLoadNodeGen.create();
                    break;
                case X86_FP80:
                    load = LLVM80BitFloatLoadNodeGen.create();
                    break;
                default:
                    throw new AssertionError(type);
            }
        } else if (type instanceof PointerType || type instanceof StructureType || type instanceof ArrayType) {
            load = LLVMAddressDirectLoadNodeGen.create();
        } else {
            throw new AssertionError(type);
        }
        return LLVMLoadExpressionNodeGen.create(load, targetAddress);
    }

    @Override
    public LLVMExpressionNode createTypedElementPointer(LLVMParserRuntime runtime, LLVMExpressionNode aggregateAddress, LLVMExpressionNode index, long indexedTypeLength,
                    Type targetType) {
        return LLVMAddressGetElementPtrNodeGen.create(aggregateAddress, index, indexedTypeLength, targetType);
    }

    @Override
    public LLVMExpressionNode createSelect(LLVMParserRuntime runtime, Type type, LLVMExpressionNode condition, LLVMExpressionNode trueValue, LLVMExpressionNode falseValue) {
        if (type instanceof VectorType) {
            if (type instanceof VectorType) {
                final Type elementType = ((VectorType) type).getElementType();
                if (elementType == PrimitiveType.I1) {
                    return LLVMI1VectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.I8) {
                    return LLVMI8VectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.I16) {
                    return LLVMI16VectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.I32) {
                    return LLVMI32VectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.I64) {
                    return LLVMI64VectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.FLOAT) {
                    return LLVMFloatVectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType == PrimitiveType.DOUBLE) {
                    return LLVMDoubleVectorSelectNodeGen.create(condition, trueValue, falseValue);
                } else if (elementType instanceof PointerType) {
                    return LLVMAddressVectorSelectNodeGen.create(condition, trueValue, falseValue);
                }
            }
            throw new AssertionError("Cannot create vector select for type: " + type);
        } else {
            if (Type.isFunctionOrFunctionPointer(type)) {
                return LLVMFunctionSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (type instanceof PointerType) {
                return LLVMAddressSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (type instanceof PrimitiveType) {
                switch (((PrimitiveType) type).getPrimitiveKind()) {
                    case I1:
                        return LLVMI1SelectNodeGen.create(condition, trueValue, falseValue);
                    case I8:
                        return LLVMI8SelectNodeGen.create(condition, trueValue, falseValue);
                    case I16:
                        return LLVMI16SelectNodeGen.create(condition, trueValue, falseValue);
                    case I32:
                        return LLVMI32SelectNodeGen.create(condition, trueValue, falseValue);
                    case I64:
                        return LLVMI64SelectNodeGen.create(condition, trueValue, falseValue);
                    case FLOAT:
                        return LLVMFloatSelectNodeGen.create(condition, trueValue, falseValue);
                    case DOUBLE:
                        return LLVMDoubleSelectNodeGen.create(condition, trueValue, falseValue);
                    case X86_FP80:
                        return LLVM80BitFloatSelectNodeGen.create(condition, trueValue, falseValue);
                    default:
                        throw new AssertionError(type);
                }
            } else {
                throw new AssertionError(type);
            }
        }
    }

    @Override
    public LLVMExpressionNode createZeroVectorInitializer(LLVMParserRuntime runtime, int nrElements, VectorType llvmType) {
        Type llvmType1 = llvmType.getElementType();
        if (llvmType1 instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType1).getPrimitiveKind()) {
                case I1:
                    LLVMExpressionNode[] i1Vals = createI1LiteralNodes(nrElements, false);
                    return LLVMVectorI1LiteralNodeGen.create(i1Vals);
                case I8:
                    LLVMExpressionNode[] i8Vals = createI8LiteralNodes(nrElements, (byte) 0);
                    return LLVMVectorI8LiteralNodeGen.create(i8Vals);
                case I16:
                    LLVMExpressionNode[] i16Vals = createI16LiteralNodes(nrElements, (short) 0);
                    return LLVMVectorI16LiteralNodeGen.create(i16Vals);
                case I32:
                    LLVMExpressionNode[] i32Vals = createI32LiteralNodes(nrElements, 0);
                    return LLVMVectorI32LiteralNodeGen.create(i32Vals);
                case I64:
                    LLVMExpressionNode[] i64Vals = createI64LiteralNodes(nrElements, 0);
                    return LLVMVectorI64LiteralNodeGen.create(i64Vals);
                case FLOAT:
                    LLVMExpressionNode[] floatVals = createFloatLiteralNodes(nrElements, 0.0f);
                    return LLVMVectorFloatLiteralNodeGen.create(floatVals);
                case DOUBLE:
                    LLVMExpressionNode[] doubleVals = createDoubleLiteralNodes(nrElements, 0.0f);
                    return LLVMVectorDoubleLiteralNodeGen.create(doubleVals);
                default:
                    throw new AssertionError(llvmType1);
            }
        } else if (llvmType1 instanceof PointerType) {
            LLVMExpressionNode[] addressVals = createNullAddressLiteralNodes(nrElements);
            return LLVMVectorAddressLiteralNodeGen.create(addressVals);
        } else {
            throw new AssertionError(llvmType1 + " not yet supported");
        }
    }

    @Override
    public LLVMExpressionNode createLiteral(LLVMParserRuntime runtime, Object value, Type type) {
        if (Type.isFunctionOrFunctionPointer(type)) {
            return LLVMFunctionLiteralNodeGen.create((LLVMFunctionDescriptor) value);
        } else if (type instanceof PointerType) {
            if (value instanceof LLVMAddress) {
                return new LLVMAddressLiteralNode((LLVMAddress) value);
            } else if (value instanceof LLVMGlobal) {
                return new LLVMAccessGlobalVariableStorageNode((LLVMGlobal) value);
            } else if (value instanceof LLVMTruffleObject) {
                return new LLVMTruffleObjectLiteralNode((LLVMTruffleObject) value);
            } else {
                throw new AssertionError(value.getClass());
            }
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return new LLVMI1LiteralNode((boolean) value);
                case I8:
                    return new LLVMI8LiteralNode((byte) value);
                case I16:
                    return new LLVMI16LiteralNode((short) value);
                case I32:
                    return new LLVMI32LiteralNode((int) value);
                case I64:
                    return new LLVMI64LiteralNode((long) value);
                case FLOAT:
                    return new LLVMFloatLiteralNode((float) value);
                case DOUBLE:
                    return new LLVMDoubleLiteralNode((double) value);
                default:
                    throw new AssertionError(value + " " + type);
            }
        }
        throw new AssertionError(value + " " + type);
    }

    @Override
    public LLVMControlFlowNode createUnreachableNode(LLVMParserRuntime runtime) {
        return new LLVMUnreachableNode();
    }

    @Override
    public LLVMControlFlowNode createIndirectBranch(LLVMParserRuntime runtime, LLVMExpressionNode value, int[] labelTargets, LLVMExpressionNode[] phiWrites, SourceSection source) {
        return LLVMIndirectBranchNode.create(new LLVMIndirectBranchNode.LLVMBasicBranchAddressNode(value), labelTargets, phiWrites, source);
    }

    @Override
    public LLVMControlFlowNode createSwitch(LLVMParserRuntime runtime, LLVMExpressionNode cond, int[] successors, LLVMExpressionNode[] cases,
                    Type llvmType, LLVMExpressionNode[] phiWriteNodes, SourceSection source) {
        LLVMExpressionNode[] caseNodes = Arrays.copyOf(cases, cases.length, LLVMExpressionNode[].class);
        return new LLVMSwitchNodeImpl(successors, phiWriteNodes, cond, caseNodes, source);
    }

    @Override
    public LLVMControlFlowNode createConditionalBranch(LLVMParserRuntime runtime, int trueIndex, int falseIndex, LLVMExpressionNode conditionNode, LLVMExpressionNode truePhiWriteNodes,
                    LLVMExpressionNode falsePhiWriteNodes, SourceSection sourceSection) {
        return LLVMConditionalBranchNode.create(trueIndex, falseIndex, truePhiWriteNodes, falsePhiWriteNodes, conditionNode, sourceSection);
    }

    @Override
    public LLVMControlFlowNode createUnconditionalBranch(LLVMParserRuntime runtime, int unconditionalIndex, LLVMExpressionNode phiWrites, SourceSection source) {
        return LLVMBrUnconditionalNode.create(unconditionalIndex, phiWrites, source);
    }

    @Override
    public LLVMExpressionNode createArrayLiteral(LLVMParserRuntime runtime, List<LLVMExpressionNode> arrayValues, Type arrayType) {
        int nrElements = arrayValues.size();
        Type elementType = ((ArrayType) arrayType).getElementType();
        int baseTypeSize = runtime.getContext().getByteSize(elementType);
        int size = nrElements * baseTypeSize;
        LLVMExpressionNode arrayAlloc = runtime.allocateFunctionLifetime(arrayType, size, runtime.getContext().getByteAlignment(arrayType));
        int byteLength = runtime.getContext().getByteSize(elementType);
        if (size == 0) {
            throw new AssertionError(elementType + " has size of 0!");
        }
        if (elementType instanceof PrimitiveType) {
            switch (((PrimitiveType) elementType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case I16:
                    return LLVMI16ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case I32:
                    return LLVMI32ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case I64:
                    return LLVMI64ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case FLOAT:
                    return LLVMFloatArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case DOUBLE:
                    return LLVMDoubleArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                case X86_FP80:
                    return LLVM80BitFloatArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
                default:
                    throw new AssertionError(elementType);
            }
        } else if (Type.isFunctionOrFunctionPointer(elementType)) {
            return LLVMFunctionArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), byteLength, arrayAlloc);
        } else if (elementType instanceof PointerType) {
            return LLVMAddressArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), baseTypeSize, arrayAlloc);
        } else if (elementType instanceof ArrayType || elementType instanceof StructureType) {
            return LLVMStructArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), createMemMove(), baseTypeSize, elementType, arrayAlloc);
        }
        throw new AssertionError(elementType);
    }

    @Override
    public LLVMExpressionNode createAlloca(LLVMParserRuntime runtime, Type type, int byteSize, int alignment) {
        if (type instanceof StructureType) {
            StructureType struct = (StructureType) type;
            final int[] offsets = new int[struct.getNumberOfElements()];
            final Type[] types = new Type[struct.getNumberOfElements()];
            int currentOffset = 0;
            for (int i = 0; i < struct.getNumberOfElements(); i++) {
                final Type elemType = struct.getElementType(i);

                if (!struct.isPacked()) {
                    currentOffset += runtime.getContext().getBytePadding(currentOffset, elemType);
                }

                offsets[i] = currentOffset;
                types[i] = elemType;
                currentOffset += runtime.getContext().getByteSize(elemType);
            }
            LLVMAllocaConstInstruction alloc = LLVMAllocaConstInstructionNodeGen.create(byteSize, alignment, type);
            alloc.setTypes(types);
            alloc.setOffsets(offsets);
            return alloc;
        }
        return LLVMAllocaConstInstructionNodeGen.create(byteSize, alignment, type);
    }

    @Override
    public LLVMExpressionNode createAlloca(LLVMParserRuntime runtime, Type elementType, LLVMExpressionNode numElements, int alignment) {
        int byteSize = runtime.getContext().getByteSize(elementType);
        return LLVMAllocaInstructionNodeGen.create(numElements, byteSize, alignment, elementType);
    }

    @Override
    public LLVMStackAllocationNode createStackAllocation(LLVMParserRuntime runtime) {
        return LLVMNativeStackAllocationNodeGen.create();
    }

    @Override
    public LLVMExpressionNode createInsertValue(LLVMParserRuntime runtime, LLVMExpressionNode resultAggregate, LLVMExpressionNode sourceAggregate, int size, long offset,
                    LLVMExpressionNode valueToInsert, Type llvmType) {
        LLVMStoreNode store;
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I8:
                    store = LLVMI8StoreNodeGen.create();
                    break;
                case I16:
                    store = LLVMI16StoreNodeGen.create();
                    break;
                case I32:
                    store = LLVMI32StoreNodeGen.create();
                    break;
                case I64:
                    store = LLVMI64StoreNodeGen.create();
                    break;
                case FLOAT:
                    store = LLVMFloatStoreNodeGen.create();
                    break;
                case DOUBLE:
                    store = LLVMDoubleStoreNodeGen.create();
                    break;
                default:
                    throw new AssertionError(llvmType);
            }
        } else if (llvmType instanceof PointerType) {
            store = LLVMAddressStoreNodeGen.create(llvmType);
        } else {
            throw new AssertionError(llvmType);
        }
        return LLVMInsertValueNodeGen.create(store, createMemMove(), size, offset, llvmType, sourceAggregate, resultAggregate, valueToInsert);
    }

    @Override
    public LLVMExpressionNode createZeroNode(LLVMParserRuntime runtime, LLVMExpressionNode addressNode, int size) {
        return LLVMMemSetNodeGen.create(createMemSet(), addressNode, new LLVMI8LiteralNode((byte) 0), new LLVMI32LiteralNode(size), new LLVMI32LiteralNode(0), new LLVMI1LiteralNode(false), null);
    }

    @Override
    public LLVMExpressionNode createStructureConstantNode(LLVMParserRuntime runtime, Type structType, boolean packed, Type[] types, LLVMExpressionNode[] constants) {
        int[] offsets = new int[types.length];
        LLVMStoreNode[] nodes = new LLVMStoreNode[types.length];
        int currentOffset = 0;
        int structSize = runtime.getContext().getByteSize(structType);
        int structAlignment = runtime.getContext().getByteAlignment(structType);
        LLVMExpressionNode alloc = runtime.allocateFunctionLifetime(structType, structSize, structAlignment);
        for (int i = 0; i < types.length; i++) {
            Type resolvedType = types[i];
            if (!packed) {
                currentOffset += runtime.getContext().getBytePadding(currentOffset, resolvedType);
            }
            offsets[i] = currentOffset;
            int byteSize = runtime.getContext().getByteSize(resolvedType);
            nodes[i] = createMemoryStore(runtime, resolvedType);
            currentOffset += byteSize;
        }
        return StructLiteralNodeGen.create(offsets, types, nodes, constants, alloc);
    }

    private LLVMStoreNode createMemoryStore(LLVMParserRuntime runtime, Type resolvedType) {
        if (resolvedType instanceof ArrayType || resolvedType instanceof StructureType) {
            int byteSize = runtime.getContext().getByteSize(resolvedType);
            return LLVMStructStoreNodeGen.create(createMemMove(), resolvedType, byteSize);
        } else if (resolvedType instanceof PrimitiveType) {
            switch (((PrimitiveType) resolvedType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8StoreNodeGen.create();
                case I16:
                    return LLVMI16StoreNodeGen.create();
                case I32:
                    return LLVMI32StoreNodeGen.create();
                case I64:
                    return LLVMI64StoreNodeGen.create();
                case FLOAT:
                    return LLVMFloatStoreNodeGen.create();
                case DOUBLE:
                    return LLVMDoubleStoreNodeGen.create();
                case X86_FP80:
                    return LLVM80BitFloatStoreNodeGen.create();
                default:
                    throw new AssertionError(resolvedType);
            }
        } else if (resolvedType instanceof PointerType || Type.isFunctionOrFunctionPointer(resolvedType)) {
            return LLVMAddressStoreNodeGen.create(resolvedType);
        }
        throw new AssertionError(resolvedType);
    }

    @Override
    public LLVMExpressionNode createBasicBlockNode(LLVMParserRuntime runtime, LLVMExpressionNode[] statementNodes, LLVMControlFlowNode terminatorNode, int blockId, String blockName) {
        return new LLVMBasicBlockNode(statementNodes, terminatorNode, blockId, blockName);
    }

    @Override
    public LLVMExpressionNode createFunctionBlockNode(LLVMParserRuntime runtime, FrameSlot exceptionValueSlot, List<? extends LLVMExpressionNode> allFunctionNodes,
                    FrameSlot[][] beforeBlockNuller, FrameSlot[][] afterBlockNuller, SourceSection sourceSection) {
        return new LLVMDispatchBasicBlockNode(exceptionValueSlot, allFunctionNodes.toArray(new LLVMBasicBlockNode[allFunctionNodes.size()]), beforeBlockNuller, afterBlockNuller, sourceSection);
    }

    @Override
    public RootNode createFunctionStartNode(LLVMParserRuntime runtime, LLVMExpressionNode functionBodyNode, LLVMExpressionNode[] copyArgumentsToFrame,
                    SourceSection sourceSection, FrameDescriptor frame, FunctionDefinition functionHeader, Source bcSource) {
        String originalName = DebugInfoGenerator.getSourceFunctionName(functionHeader);
        return new LLVMFunctionStartNode(sourceSection, runtime.getLanguage(), functionBodyNode, copyArgumentsToFrame, frame, functionHeader.getName(), functionHeader.getParameters().size(),
                        originalName, bcSource);
    }

    @Override
    public LLVMExpressionNode createInlineAssemblerExpression(LLVMParserRuntime runtime, String asmExpression, String asmFlags, LLVMExpressionNode[] args, Type[] argTypes, Type retType,
                    SourceSection sourceSection) {
        Type[] retTypes = null;
        int[] retOffsets = null;
        if (retType instanceof StructureType) { // multiple out values
            assert args[1] instanceof LLVMAllocaConstInstruction;
            LLVMAllocaConstInstruction alloca = (LLVMAllocaConstInstruction) args[1];
            retTypes = alloca.getTypes();
            retOffsets = alloca.getOffsets();
        }

        Parser asmParser = new Parser(runtime.getLanguage(), sourceSection, asmExpression, asmFlags, argTypes, retType, retTypes, retOffsets);
        LLVMInlineAssemblyRootNode assemblyRoot = asmParser.Parse();
        LLVMFunctionDescriptor asm = LLVMFunctionDescriptor.createDescriptor(runtime.getContext(), runtime.getLibraryName(), "<asm>", new FunctionType(MetaType.UNKNOWN, new Type[0], false), -1);
        asm.declareInSulong(Truffle.getRuntime().createCallTarget(assemblyRoot), false);
        LLVMFunctionLiteralNode asmFunction = LLVMFunctionLiteralNodeGen.create(asm);

        return new LLVMCallNode(new FunctionType(MetaType.UNKNOWN, argTypes, false), asmFunction, args, sourceSection);
    }

    @Override
    public LLVMExpressionNode createFunctionArgNode(int i) {
        return LLVMArgNodeGen.create(i);
    }

    @Override
    public Object allocateGlobalVariable(LLVMParserRuntime runtime, GlobalVariable globalVariable) {
        return allocateGlobalIntern(runtime, globalVariable);

    }

    private static Object allocateGlobalIntern(LLVMParserRuntime runtime, final GlobalValueSymbol globalVariable) {
        final Type resolvedType = ((PointerType) globalVariable.getType()).getPointeeType();
        final String name = globalVariable.getName();

        LLVMContext context = runtime.getContext();

        final LLVMGlobal descriptor;

        if (globalVariable.getInitialiser() == 0 && Linkage.isExtern(globalVariable.getLinkage())) {
            NFIContextExtension nfiContextExtension = context.getContextExtension(NFIContextExtension.class);
            return LLVMGlobal.external(context, globalVariable, name, resolvedType, LLVMAddress.fromLong(nfiContextExtension.getNativeHandle(context, name)));
        } else {
            descriptor = LLVMGlobal.internal(context, globalVariable, name, resolvedType);
            runtime.addDestructor(new LLVMExpressionNode() {

                private final LLVMGlobal global = descriptor;
                private final LLVMContext c = context;

                @Override
                public Object executeGeneric(VirtualFrame frame) {
                    LLVMGlobal.free(c, global);
                    return null;
                }
            });
            return descriptor;
        }

    }

    @Override
    public Object allocateGlobalConstant(LLVMParserRuntime runtime, GlobalConstant globalConstant) {
        return allocateGlobalIntern(runtime, globalConstant);
    }

    @Override
    public RootNode createStaticInitsRootNode(LLVMParserRuntime runtime, LLVMExpressionNode[] staticInits) {
        return new LLVMStaticInitsBlockNode(runtime.getLanguage(), staticInits, runtime.getGlobalFrameDescriptor());
    }

    @Override
    public LLVMControlFlowNode createFunctionInvoke(LLVMParserRuntime runtime, FrameSlot resultLocation, LLVMExpressionNode functionNode, LLVMExpressionNode[] argNodes, FunctionType type,
                    int normalIndex, int unwindIndex, LLVMExpressionNode normalPhiWriteNodes, LLVMExpressionNode unwindPhiWriteNodes,
                    SourceSection sourceSection) {
        return new LLVMInvokeNode.LLVMFunctionInvokeNode(type, resultLocation, functionNode, argNodes, normalIndex, unwindIndex, normalPhiWriteNodes, unwindPhiWriteNodes,
                        sourceSection);
    }

    @Override
    public LLVMExpressionNode createLandingPad(LLVMParserRuntime runtime, LLVMExpressionNode allocateLandingPadValue, FrameSlot exceptionValueSlot, boolean cleanup, long[] clauseKinds,
                    LLVMExpressionNode[] entries) {

        LLVMLandingpadNode.LandingpadEntryNode[] landingpadEntries = new LLVMLandingpadNode.LandingpadEntryNode[entries.length];
        for (int i = 0; i < entries.length; i++) {
            if (clauseKinds[i] == 0) {
                // catch
                landingpadEntries[i] = getLandingpadCatchEntry(entries[i]);
            } else if (clauseKinds[i] == 1) {
                // filter
                landingpadEntries[i] = getLandingpadFilterEntry(entries[i]);
            } else {
                throw new IllegalStateException();
            }
        }
        return new LLVMLandingpadNode(allocateLandingPadValue, exceptionValueSlot, cleanup, landingpadEntries);
    }

    private static LLVMLandingpadNode.LandingpadEntryNode getLandingpadCatchEntry(LLVMExpressionNode exp) {
        return new LLVMLandingpadNode.LandingpadCatchEntryNode(exp);
    }

    private static LLVMLandingpadNode.LandingpadEntryNode getLandingpadFilterEntry(LLVMExpressionNode exp) {
        LLVMAddressArrayLiteralNode array = (LLVMAddressArrayLiteralNode) exp;
        LLVMExpressionNode[] types = array == null ? new LLVMExpressionNode[]{} : array.getValues();
        return new LLVMLandingpadNode.LandingpadFilterEntryNode(types);
    }

    @Override
    public LLVMControlFlowNode createResumeInstruction(LLVMParserRuntime runtime, FrameSlot exceptionValueSlot, SourceSection source) {
        return new LLVMResumeNode(exceptionValueSlot, source);
    }

    @Override
    public LLVMExpressionNode createCompareExchangeInstruction(LLVMParserRuntime runtime, Type returnType, Type elementType, LLVMExpressionNode ptrNode, LLVMExpressionNode cmpNode,
                    LLVMExpressionNode newNode) {
        return LLVMCompareExchangeNodeGen.create(runtime.getContext().getByteSize(returnType),
                        runtime.getContext().getIndexOffset(1, (AggregateType) returnType), ptrNode, cmpNode, newNode);
    }

    @Override
    public LLVMExpressionNode createLLVMBuiltin(LLVMParserRuntime runtime, Symbol target, LLVMExpressionNode[] args, int callerArgumentCount, SourceSection sourceSection) {
        /*
         * This LLVM Builtins are *not* function intrinsics. Builtins replace statements that look
         * like function calls but are actually LLVM intrinsics. An example is llvm.stackpointer.
         * Also, it is not possible to retrieve the functionpointer of such pseudo-call-targets.
         *
         * This builtins shall not be used for regular function intrinsification!
         */
        if (target instanceof FunctionDeclaration) {
            FunctionDeclaration declaration = (FunctionDeclaration) target;
            if (declaration.getName().startsWith("@llvm.")) {
                return getLLVMBuiltin(runtime, declaration, args, callerArgumentCount, sourceSection);
            } else if (declaration.getName().startsWith("@__builtin_")) {
                return getGccBuiltin(declaration, args, sourceSection);
            } else if (declaration.getName().equals("@truffle_get_arg")) {
                // this function accesses the frame directly
                // it must therefore not be hidden behind a call target
                return LLVMTruffleGetArgNodeGen.create(args[1], sourceSection);
            } else if (declaration.getName().equals("@__divsc3")) {
                // this function allocates the result on the stack
                return new LLVMComplexDivSC(args[1], args[2], args[3], args[4]);
            }
        }
        return null;
    }

    protected LLVMExpressionNode getLLVMBuiltin(LLVMParserRuntime runtime, FunctionDeclaration declaration, LLVMExpressionNode[] args, int callerArgumentCount, SourceSection sourceSection) {

        switch (declaration.getName()) {
            case "@llvm.memset.p0i8.i32":
            case "@llvm.memset.p0i8.i64":
                return LLVMMemSetNodeGen.create(createMemSet(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.assume":
                return LLVMAssumeNodeGen.create(args[1], sourceSection);
            case "@llvm.clear_cache": // STUB
            case "@llvm.donothing":
                return LLVMNoOpNodeGen.create(sourceSection);
            case "@llvm.prefetch":
                return LLVMPrefetchNodeGen.create(args[1], args[2], args[3], args[4], sourceSection);
            case "@llvm.ctlz.i8":
                return CountLeadingZeroesI8NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i16":
                return CountLeadingZeroesI16NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i32":
                return CountLeadingZeroesI32NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i64":
                return CountLeadingZeroesI64NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.memcpy.p0i8.p0i8.i64":
            case "@llvm.memcpy.p0i8.p0i8.i32":
                return LLVMMemCopyNodeGen.create(createMemMove(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.ctpop.i32":
                return CountSetBitsI32NodeGen.create(args[1], sourceSection);
            case "@llvm.ctpop.i64":
                return CountSetBitsI64NodeGen.create(args[1], sourceSection);
            case "@llvm.cttz.i8":
                return CountTrailingZeroesI8NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i16":
                return CountTrailingZeroesI16NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i32":
                return CountTrailingZeroesI32NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i64":
                return CountTrailingZeroesI64NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.trap":
                return LLVMTrapNodeGen.create(sourceSection);
            case "@llvm.bswap.i16":
                return LLVMByteSwapI16NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.i32":
                return LLVMByteSwapI32NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.i64":
                return LLVMByteSwapI64NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.v8i16":
                return LLVMByteSwapV8I16NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.v4i32":
                return LLVMByteSwapV4I32NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.v2i64":
                return LLVMByteSwapV2I64NodeGen.create(args[1], sourceSection);
            case "@llvm.memmove.p0i8.p0i8.i64":
                return LLVMMemMoveI64NodeGen.create(createMemMove(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.pow.f32":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.pow.f64":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.pow.f80":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f32":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f64":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f80":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.fabs.f32":
            case "@llvm.fabs.f64":
            case "@llvm.fabs.f80":
                return LLVMFAbsNodeGen.create(args[1], sourceSection);
            case "@llvm.returnaddress":
                return LLVMReturnAddressNodeGen.create(args[1], sourceSection);
            case "@llvm.lifetime.start.p0i8":
            case "@llvm.lifetime.start":
                return LLVMLifetimeStartNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.lifetime.end.p0i8":
            case "@llvm.lifetime.end":
                return LLVMLifetimeEndNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.invariant.start":
            case "@llvm.invariant.start.p0i8":
                return LLVMInvariantStartNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.invariant.end":
            case "@llvm.invariant.end.p0i8":
                return LLVMInvariantEndNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.stacksave":
                return LLVMStackSaveNodeGen.create(sourceSection);
            case "@llvm.stackrestore":
                return LLVMStackRestoreNodeGen.create(args[1], sourceSection);
            case "@llvm.frameaddress":
                return LLVMFrameAddressNodeGen.create(args[1], sourceSection);
            case "@llvm.va_start":
                return LLVMX86_64VAStartNodeGen.create(callerArgumentCount, sourceSection, createStackAllocation(runtime), createMemMove(), args[1]);
            case "@llvm.va_end":
                return new LLVMX86_64BitVAEnd(args[1], sourceSection);
            case "@llvm.va_copy":
                return LLVMX86_64BitVACopyNodeGen.create(args[1], args[2], sourceSection, callerArgumentCount);
            case "@llvm.eh.sjlj.longjmp":
            case "@llvm.eh.sjlj.setjmp":
                throw new LLVMUnsupportedException(UnsupportedReason.SET_JMP_LONG_JMP);
            case "@llvm.dbg.declare":
                throw new IllegalStateException("@llvm.dbg.declare should be handled in the parser!");
            case "@llvm.dbg.value":
                throw new IllegalStateException("@llvm.dbg.value should be handled in the parser!");
            case "@llvm.eh.typeid.for":
                return new LLVMTypeIdForExceptionNode(args[1], sourceSection);
            case "@llvm.expect.i1": {
                boolean expectedValue = args[2].executeI1(null);
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI1NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.expect.i32": {
                int expectedValue = args[2].executeI32(null);
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI32NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.expect.i64": {
                long expectedValue = args[2].executeI64(null);
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI64NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.objectsize.i64.p0i8":
            case "@llvm.objectsize.i64":
                return LLVMI64ObjectSizeNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.copysign.f32":
            case "@llvm.copysign.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMCopySignNodeGen.create(args[1], args[2], sourceSection);

            case "@llvm.uadd.with.overflow.i8":
            case "@llvm.uadd.with.overflow.i16":
            case "@llvm.uadd.with.overflow.i32":
            case "@llvm.uadd.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_ADD, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.usub.with.overflow.i8":
            case "@llvm.usub.with.overflow.i16":
            case "@llvm.usub.with.overflow.i32":
            case "@llvm.usub.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_SUB, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.umul.with.overflow.i8":
            case "@llvm.umul.with.overflow.i16":
            case "@llvm.umul.with.overflow.i32":
            case "@llvm.umul.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_MUL, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.sadd.with.overflow.i8":
            case "@llvm.sadd.with.overflow.i16":
            case "@llvm.sadd.with.overflow.i32":
            case "@llvm.sadd.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_ADD, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.ssub.with.overflow.i8":
            case "@llvm.ssub.with.overflow.i16":
            case "@llvm.ssub.with.overflow.i32":
            case "@llvm.ssub.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_SUB, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.smul.with.overflow.i8":
            case "@llvm.smul.with.overflow.i16":
            case "@llvm.smul.with.overflow.i32":
            case "@llvm.smul.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_MUL, getOverflowFieldOffset(runtime, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.exp2.f32":
            case "@llvm.exp2.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMExp2NodeGen.create(args[1], sourceSection);
            case "@llvm.sqrt.f32":
            case "@llvm.sqrt.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMSqrtNodeGen.create(args[1], sourceSection);
            case "@llvm.sin.f32":
            case "@llvm.sin.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMSinNodeGen.create(args[1], sourceSection);
            case "@llvm.cos.f32":
            case "@llvm.cos.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMCosNodeGen.create(args[1], sourceSection);
            case "@llvm.exp.f32":
            case "@llvm.exp.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMExpNodeGen.create(args[1], sourceSection);
            case "@llvm.log.f32":
            case "@llvm.log.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLogNodeGen.create(args[1], sourceSection);
            case "@llvm.log2.f32":
            case "@llvm.log2.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLog2NodeGen.create(args[1], sourceSection);
            case "@llvm.log10.f32":
            case "@llvm.log10.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLog10NodeGen.create(args[1], sourceSection);
            case "@llvm.floor.f32":
            case "@llvm.floor.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMFloorNodeGen.create(args[1], sourceSection);
            case "@llvm.ceil.f32":
            case "@llvm.ceil.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMCeilNodeGen.create(args[1], sourceSection);
            case "@llvm.rint.f32":
            case "@llvm.rint.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMRintNodeGen.create(args[1], sourceSection);
            case "@llvm.x86.sse.cvtss2si":
                return LLVMX86_ConversionFloatToIntNodeGen.create(args[1], sourceSection);
            case "@llvm.x86.sse2.cvtsd2si":
                return LLVMX86_ConversionDoubleToIntNodeGen.create(args[1], sourceSection);
            default:
                throw new IllegalStateException("Missing LLVM builtin: " + declaration.getName());
        }
    }

    private static long getOverflowFieldOffset(LLVMParserRuntime runtime, FunctionDeclaration declaration) {
        return runtime.getContext().getIndexOffset(1, (AggregateType) declaration.getType().getReturnType());
    }

    protected LLVMExpressionNode getGccBuiltin(FunctionDeclaration declaration, LLVMExpressionNode[] args, SourceSection sourceSection) {
        switch (declaration.getName()) {
            case "@__builtin_addcb":
            case "@__builtin_addcs":
            case "@__builtin_addc":
            case "@__builtin_addcl":
                return LLVMArithmeticWithOverflowAndCarryNodeGen.create(LLVMArithmetic.CARRY_ADD, args[1], args[2], args[3], args[4], sourceSection);
            case "@__builtin_subcb":
            case "@__builtin_subcs":
            case "@__builtin_subc":
            case "@__builtin_subcl":
                return LLVMArithmeticWithOverflowAndCarryNodeGen.create(LLVMArithmetic.CARRY_SUB, args[1], args[2], args[3], args[4], sourceSection);
            case "@__builtin_add_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_ADD, args[1], args[2], args[3], sourceSection);
                }
            case "@__builtin_sub_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_SUB, args[1], args[2], args[3], sourceSection);
                }
            case "@__builtin_mul_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.UNSIGNED_MUL, args[1], args[2], args[3], sourceSection);
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_MUL, args[1], args[2], args[3], sourceSection);
                }

            default:
                throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
        }
    }

    private static boolean isZeroExtendArithmeticBuiltin(FunctionDeclaration declaration) {
        final AttributesGroup group = declaration.getParameterAttributesGroup(0);
        if (group == null) {
            return false;
        }
        for (Attribute a : group.getAttributes()) {
            if (a instanceof KnownAttribute && ((KnownAttribute) a).getAttr() == Attribute.Kind.ZEROEXT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public LLVMExpressionNode createPhi(LLVMExpressionNode[] from, FrameSlot[] to, Type[] types) {
        return new LLVMWritePhisNode(from, to, types);
    }

    @Override
    public RootNode createGlobalRootNode(LLVMParserRuntime runtime, RootCallTarget mainCallTarget, Source sourceFile, Type mainReturnType, Type[] mainTypes) {
        return new LLVMGlobalRootNode(runtime.getLanguage(), runtime.getGlobalFrameDescriptor(), sourceFile, mainReturnType, mainTypes, mainCallTarget);
    }

    @Override
    public RootNode createGlobalRootNodeWrapping(LLVMParserRuntime runtime, RootCallTarget mainCallTarget, Type returnType) {
        LLVMLanguage language = runtime.getLanguage();
        if (returnType instanceof VoidType) {
            return new LLVMMainFunctionReturnValueRootNode.LLVMMainFunctionReturnVoidRootNode(language, mainCallTarget);
        } else if (returnType instanceof VariableBitWidthType) {
            return new LLVMMainFunctionReturnValueRootNode.LLVMMainFunctionReturnIVarBitRootNode(language, mainCallTarget);
        } else if (returnType instanceof PrimitiveType) {
            switch (((PrimitiveType) returnType).getPrimitiveKind()) {
                case I1:
                    return new LLVMMainFunctionReturnValueRootNode.LLVMMainFunctionReturnI1RootNode(language, mainCallTarget);
                case I8:
                case I16:
                case I32:
                case I64:
                case FLOAT:
                case DOUBLE:
                    return new LLVMMainFunctionReturnValueRootNode.LLVMMainFunctionReturnNumberRootNode(language, mainCallTarget);
                default:
                    throw new AssertionError(returnType);
            }
        }
        throw new AssertionError(returnType);
    }

    @Override
    public LLVMExpressionNode createCopyStructByValue(LLVMParserRuntime runtime, Type type, int length, int alignment, LLVMExpressionNode parameterNode) {
        LLVMStackAllocationNode allocationNode = createStackAllocation(runtime);
        return LLVMStructByValueNodeGen.create(createMemMove(), allocationNode, parameterNode, length);
    }

    @Override
    public LLVMExpressionNode createVarArgCompoundValue(LLVMParserRuntime runtime, int length, int alignment, LLVMExpressionNode parameterNode) {
        return LLVMVarArgCompoundAddressNodeGen.create(parameterNode, length, alignment);
    }

    private static LLVMDebugBuilder getDebugBuilder(boolean isDeclaration) {
        if (isDeclaration) {
            return LLVMDebugBuilder.NATIVE_DECLARATION;
        } else {
            return LLVMDebugBuilder.NATIVE_VALUE;
        }
    }

    @Override
    public LLVMExpressionNode createDebugWrite(boolean isDeclaration, LLVMExpressionNode valueRead, FrameSlot targetSlot) {
        final LLVMDebugBuilder builder = getDebugBuilder(isDeclaration);
        return LLVMDebugFrameWriteNodeFactory.WriteNodeGen.create(targetSlot, builder, valueRead);
    }

    @Override
    public LLVMExpressionNode createDebugFragmentWrite(boolean isDeclaration, LLVMExpressionNode valueRead, FrameSlot targetSlot, LLVMExpressionNode aggregateRead, int partIndex, int[] clearParts) {
        final LLVMDebugBuilder builder = getDebugBuilder(isDeclaration);
        return LLVMDebugFrameWriteNodeFactory.AggregateWriteNodeGen.create(targetSlot, partIndex, builder, clearParts, aggregateRead, valueRead);
    }

    @Override
    public LLVMExpressionNode createDebugFragmentInit(FrameSlot targetSlot, int[] offsets, int[] lengths) {
        return LLVMDebugFrameWriteNodeFactory.AggregateInitNodeGen.create(targetSlot, offsets, lengths);
    }

    @Override
    public LLVMDebugValue createDebugConstantValue(LLVMExpressionNode valueNode) {
        final LLVMDebugValueProvider.Builder toDebugNode = LLVMToDebugValueNodeGen.create(LLVMLanguage.getLLVMContextReference());
        Object value;
        try {
            value = valueNode.executeGeneric(null);
        } catch (Throwable t) {
            // constant values should not need frame access
            value = null;
        }
        return LLVMDebugValue.create(toDebugNode, value);
    }

    @Override
    public LLVMExpressionNode registerSourceType(FrameSlot valueSlot, LLVMSourceType type) {
        return new LLVMSetInteropTypeNode(valueSlot, type);
    }

    @Override
    public LLVMMemMoveNode createMemMove() {
        return NativeProfiledMemMoveNodeGen.create();
    }

    @Override
    public LLVMAllocateStringNode createAllocateString() {
        return NativeAllocateStringNodeGen.create();
    }

    @Override
    public LLVMMemSetNode createMemSet() {
        return NativeMemSetNodeGen.create();
    }

    private static LLVMExpressionNode[] createDoubleLiteralNodes(int nrElements, double value) {
        LLVMExpressionNode[] doubleZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            doubleZeroInits[i] = new LLVMDoubleLiteralNode(value);
        }
        return doubleZeroInits;
    }

    private static LLVMExpressionNode[] createFloatLiteralNodes(int nrElements, float value) {
        LLVMExpressionNode[] floatZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            floatZeroInits[i] = new LLVMFloatLiteralNode(value);
        }
        return floatZeroInits;
    }

    private static LLVMExpressionNode[] createI64LiteralNodes(int nrElements, long value) {
        LLVMExpressionNode[] i64ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i64ZeroInits[i] = new LLVMI64LiteralNode(value);
        }
        return i64ZeroInits;
    }

    private static LLVMExpressionNode[] createI32LiteralNodes(int nrElements, int value) {
        LLVMExpressionNode[] i32ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i32ZeroInits[i] = new LLVMI32LiteralNode(value);
        }
        return i32ZeroInits;
    }

    private static LLVMExpressionNode[] createI16LiteralNodes(int nrElements, short value) {
        LLVMExpressionNode[] i16ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i16ZeroInits[i] = new LLVMI16LiteralNode(value);
        }
        return i16ZeroInits;
    }

    private static LLVMExpressionNode[] createI8LiteralNodes(int nrElements, byte value) {
        LLVMExpressionNode[] i8ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i8ZeroInits[i] = new LLVMI8LiteralNode(value);
        }
        return i8ZeroInits;
    }

    private static LLVMExpressionNode[] createI1LiteralNodes(int nrElements, boolean value) {
        LLVMExpressionNode[] i1ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i1ZeroInits[i] = new LLVMI1LiteralNode(value);
        }
        return i1ZeroInits;
    }

    private static LLVMExpressionNode[] createNullAddressLiteralNodes(int nrElements) {
        LLVMExpressionNode[] addressZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            addressZeroInits[i] = new LLVMAddressLiteralNode(LLVMAddress.nullPointer());
        }
        return addressZeroInits;
    }

    private static LLVMExpressionNode createLoad(Type resultType, LLVMExpressionNode loadTarget, int bits) {
        LLVMLoadNode load;
        if (resultType instanceof PrimitiveType) {
            switch (((PrimitiveType) resultType).getPrimitiveKind()) {
                case I1:
                    load = LLVMI1LoadNodeGen.create();
                    break;
                case I8:
                    load = LLVMI8LoadNodeGen.create();
                    break;
                case I16:
                    load = LLVMI16LoadNodeGen.create();
                    break;
                case I32:
                    load = LLVMI32LoadNodeGen.create();
                    break;
                case I64:
                    load = LLVMI64LoadNodeGen.create();
                    break;
                case FLOAT:
                    load = LLVMFloatLoadNodeGen.create();
                    break;
                case DOUBLE:
                    load = LLVMDoubleLoadNodeGen.create();
                    break;
                case X86_FP80:
                    load = LLVM80BitFloatDirectLoadNodeGen.create();
                    break;
                default:
                    throw new AssertionError(resultType);
            }
        } else if (resultType instanceof VariableBitWidthType) {
            load = LLVMIVarBitDirectLoadNodeGen.create(bits);
        } else if (Type.isFunctionOrFunctionPointer(resultType)) {
            load = LLVMFunctionDirectLoadNodeGen.create();
        } else if (resultType instanceof StructureType || resultType instanceof ArrayType) {
            load = LLVMStructDirectLoadNodeGen.create();
        } else if (resultType instanceof PointerType) {
            if (loadTarget instanceof LLVMAccessGlobalVariableStorageNode) {
                return new LLVMGlobalDirectLoadNode(((LLVMAccessGlobalVariableStorageNode) loadTarget).getDescriptor());
            } else {
                load = LLVMAddressDirectLoadNodeGen.create();
            }
        } else {
            throw new AssertionError(resultType);
        }
        return LLVMLoadExpressionNodeGen.create(load, loadTarget);
    }

    private LLVMExpressionNode createStore(LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type, int size, SourceSection source) {
        LLVMStoreNode store;
        if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    store = LLVMI1StoreNodeGen.create();
                    break;
                case I8:
                    store = LLVMI8StoreNodeGen.create();
                    break;
                case I16:
                    store = LLVMI16StoreNodeGen.create();
                    break;
                case I32:
                    store = LLVMI32StoreNodeGen.create();
                    break;
                case I64:
                    store = LLVMI64StoreNodeGen.create();
                    break;
                case FLOAT:
                    store = LLVMFloatStoreNodeGen.create();
                    break;
                case DOUBLE:
                    store = LLVMDoubleStoreNodeGen.create();
                    break;
                case X86_FP80:
                    store = LLVM80BitFloatStoreNodeGen.create();
                    break;
                default:
                    throw new AssertionError(type);
            }
        } else if (type instanceof VariableBitWidthType) {
            store = LLVMIVarBitStoreNodeGen.create((VariableBitWidthType) type);
        } else if (Type.isFunctionOrFunctionPointer(type)) {
            store = LLVMFunctionStoreNodeGen.create(type);
        } else if (type instanceof StructureType || type instanceof ArrayType) {
            store = LLVMStructStoreNodeGen.create(createMemMove(), type, size);
        } else if (type instanceof PointerType) {
            if (pointerNode instanceof LLVMAccessGlobalVariableStorageNode) {
                return LLVMGlobalVariableStoreNodeGen.create(((LLVMAccessGlobalVariableStorageNode) pointerNode).getDescriptor(), source, valueNode);
            } else {
                store = LLVMAddressStoreNodeGen.create(type);
            }
        } else if (type instanceof VectorType) {
            store = LLVMStoreVectorNodeGen.create(type, size);
        } else {
            throw new AssertionError(type);
        }
        return LLVMStoreExpressionNodeGen.create(source, store, pointerNode, valueNode);
    }
}
