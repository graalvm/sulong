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
package com.oracle.truffle.llvm.writer.facades;

import com.oracle.truffle.llvm.parser.api.datalayout.DataLayoutConverter;
import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.parser.api.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.api.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.api.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.api.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.parser.api.model.types.FunctionType;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.PointerType;
import com.oracle.truffle.llvm.parser.api.model.types.Type;

public class InstructionGeneratorFacade {
    private static final String x86TargetDataLayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64-S128";
    public static final DataLayoutConverter.DataSpecConverter targetDataLayout = DataLayoutConverter.getConverter(x86TargetDataLayout);

    private final Model model;
    private final FunctionDefinition def;
    private InstructionBlock gen;

    private int counter = 1;
    private int argCounter = 1;

    public InstructionGeneratorFacade(Model model, String name, int blocks, FunctionType type) {
        this.model = model;
        model.createModule().createFunction(type, false);
        this.def = (FunctionDefinition) model.createModule().generateFunction();
        def.setName(name);
        this.def.allocateBlocks(blocks);
        this.gen = (InstructionBlock) this.def.generateBlock();
    }

    public InstructionGeneratorFacade(Model model, String name, int blocks, Type retType, Type[] args, boolean isVarArg) {
        this(model, name, blocks, new FunctionType(retType, args, isVarArg));
    }

    public Model getModel() {
        return model;
    }

    public FunctionDefinition getFunctionDefinition() {
        return def;
    }

    public FunctionParameter createParameter(Type type) {
        def.createParameter(type);
        FunctionParameter newParam = def.getParameters().get(def.getParameters().size() - 1);
        newParam.setName("arg_" + Integer.toString(argCounter++));
        return newParam;
    }

    public void nextBlock() {
        this.gen = (InstructionBlock) this.def.generateBlock();
        this.gen.setName(Integer.toString(counter++));
    }

    public void exitFunction() {
        this.def.exitFunction();
    }

    private static Symbol createI32Constant(int value) {
        return new IntegerConstant(IntegerType.INTEGER, value);
    }

    /**
     * Add a new Symbol to the Symbol list, and return it's given symbol position.
     */
    private int addSymbol(Symbol sym) {
        Symbols symbols = def.getSymbols();
        symbols.addSymbol(sym);
        return symbols.getSize() - 1; // return index of new symbol
    }

    /**
     * Get the last instruction added to the function.
     */
    private Instruction getLastInstruction() {
        Instruction lastInstr = gen.getInstruction(gen.getInstructionCount() - 1);
        if (lastInstr instanceof ValueInstruction && lastInstr.getName().equals(Instruction.UNKNOWN)) {
            ((ValueInstruction) lastInstr).setName(Integer.toString(counter++));
        }
        return lastInstr;
    }

    private static int calculateAlign(int align) {
        assert Integer.highestOneBit(align) == align;

        return align == 0 ? 0 : Integer.numberOfTrailingZeros(align) + 1;
    }

    public Instruction createAllocate(Type type) {
        Type pointerType = new PointerType(type);
        int count = addSymbol(createI32Constant(1));
        int align = type.getAlignment(targetDataLayout);
        gen.createAllocation(pointerType, count, calculateAlign(align));
        return getLastInstruction();
    }

    public Instruction createAtomicLoad(Type type, Instruction source, int align, boolean isVolatile, long atomicOrdering, long synchronizationScope) {
        int sourceIdx = addSymbol(source);
        gen.createAtomicLoad(type, sourceIdx, calculateAlign(align), isVolatile, atomicOrdering, synchronizationScope);
        return getLastInstruction();
    }

    public Instruction createAtomicStore(Instruction destination, Instruction source, int align, boolean isVolatile, long atomicOrdering, long synchronizationScope) {
        int destinationIdx = addSymbol(destination);
        int sourceIdx = addSymbol(source);
        gen.createAtomicStore(destinationIdx, sourceIdx, calculateAlign(align), isVolatile, atomicOrdering, synchronizationScope);
        return getLastInstruction();
    }

    public Instruction createBinaryOperation(Symbol lhs, Symbol rhs, BinaryOperator op) {
        Type type = lhs.getType();
        int flagbits = 0; // TODO: flags are not supported yet
        int lhsIdx = addSymbol(lhs);
        int rhsIdx = addSymbol(rhs);
        gen.createBinaryOperation(type, op.ordinal(), flagbits, lhsIdx, rhsIdx);
        return getLastInstruction();
    }

    public Instruction createBranch(int block) {
        gen.createBranch(block);
        return getLastInstruction();
    }

    public Instruction createBranch(Symbol condition, int ifBlock, int elseBlock) {
        int conditionIdx = addSymbol(condition);

        gen.createBranch(conditionIdx, ifBlock, elseBlock);
        return getLastInstruction();
    }

    public Instruction createCall(Symbol target, Symbol[] arguments) {
        Type returnType;
        if (target instanceof FunctionType) {
            returnType = ((FunctionType) target).getReturnType();
        } else if (target.getType() instanceof FunctionType) {
            returnType = ((FunctionType) target.getType()).getReturnType();
        } else if (target instanceof LoadInstruction) {
            Type pointeeType = ((LoadInstruction) target).getSource().getType();
            while (pointeeType instanceof PointerType) {
                pointeeType = ((PointerType) pointeeType).getPointeeType();
            }
            if (pointeeType instanceof FunctionType) {
                returnType = ((FunctionType) pointeeType).getReturnType();
            } else {
                throw new RuntimeException("cannot handle target type: " + pointeeType.getClass().getName());
            }
        } else {
            throw new RuntimeException("cannot handle target type: " + target.getClass().getName());
        }
        int targetIdx = addSymbol(target);
        int[] argumentsIdx = new int[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentsIdx[i] = addSymbol(arguments[i]);
        }
        gen.createCall(returnType, targetIdx, argumentsIdx, Visibility.DEFAULT.ordinal(), Linkage.EXTERNAL.ordinal());
        return getLastInstruction();
    }

    public Instruction createCast(Type type, CastOperator op, Symbol value) {
        int valueIdx = addSymbol(value);
        gen.createCast(type, op.ordinal(), valueIdx);
        return getLastInstruction();
    }

    public Instruction createCompare(CompareOperator op, Symbol lhs, Symbol rhs) {
        Type type = lhs.getType();
        int lhsIdx = addSymbol(lhs);
        int rhsIdx = addSymbol(rhs);
        gen.createCompare(type, op.getIndex(), lhsIdx, rhsIdx);
        return getLastInstruction();
    }

    public Instruction createExtractValue(Instruction struct, Symbol vector, int index) {
        Type type = struct.getType().getIndexType(index); // TODO: correct?
        int vectorIdx = addSymbol(vector);
        int indexIdx = addSymbol(createI32Constant(index));
        gen.createExtractElement(type, vectorIdx, indexIdx);
        return getLastInstruction();
    }

    public Instruction createExtractElement(Instruction vector, int index) {
        Type type = vector.getType().getIndexType(index);
        int vectorIdx = addSymbol(vector);
        int indexIdx = addSymbol(createI32Constant(index));
        gen.createExtractElement(type, vectorIdx, indexIdx);
        return getLastInstruction();
    }

    public Instruction createGetElementPointer(Type type, Symbol base, Symbol[] indices, boolean isInbounds) {
        int pointerIdx = addSymbol(base);
        int[] indicesIdx = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indicesIdx[i] = addSymbol(indices[i]);
        }
        gen.createGetElementPointer(type, pointerIdx, indicesIdx, isInbounds);
        return getLastInstruction();
    }

    public Instruction createIndirectBranch(Symbol address, int[] successors) {
        int addressIdx = addSymbol(address);
        gen.createIndirectBranch(addressIdx, successors);
        return getLastInstruction();
    }

    public Instruction createInsertElement(Instruction vector, Constant value, int index) {
        Type type = vector.getType();
        int vectorIdx = addSymbol(vector);
        int valueIdx = addSymbol(value);
        int indexIdx = addSymbol(new IntegerConstant(IntegerType.INTEGER, index));
        gen.createInsertElement(type, vectorIdx, indexIdx, valueIdx);
        return getLastInstruction();
    }

    public Instruction createInsertValue(Instruction struct, Symbol aggregate, int index, Symbol value) {
        Type type = struct.getType(); // TODO: correct?
        int valueIdx = addSymbol(value);
        int aggregateIdx = addSymbol(aggregate);
        gen.createInsertValue(type, aggregateIdx, index, valueIdx);
        return getLastInstruction();
    }

    public Instruction createLoad(Instruction source) {
        Type type = ((PointerType) source.getType()).getPointeeType();
        int sourceIdx = addSymbol(source);
        int align = type.getAlignment(targetDataLayout);
        // because we don't have any optimizations, we can set isVolatile to false
        boolean isVolatile = false;
        gen.createLoad(type, sourceIdx, calculateAlign(align), isVolatile);
        return getLastInstruction();
    }

    public Instruction createPhi(Type type, int[] values, InstructionBlock[] blocks) {
        assert values.length == blocks.length;

        int[] valuesIdx = new int[values.length];
        int[] blocksIdx = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            valuesIdx[i] = addSymbol(createI32Constant(values[i]));
            blocksIdx[i] = addSymbol(blocks[i]);
        }
        gen.createPhi(type, values, blocksIdx);
        return getLastInstruction();
    }

    public Instruction createReturn() {
        gen.createReturn();
        return getLastInstruction();
    }

    public Instruction createReturn(Symbol value) {
        int valueIdx = addSymbol(value);
        gen.createReturn(valueIdx);
        return getLastInstruction();
    }

    public Instruction createSelect(Type type, Symbol condition, Symbol trueValue, Symbol falseValue) {
        int conditionIdx = addSymbol(condition);
        int trueValueIdx = addSymbol(trueValue);
        int falseValueIdx = addSymbol(falseValue);
        gen.createSelect(type, conditionIdx, trueValueIdx, falseValueIdx);
        return getLastInstruction();
    }

    public Instruction createShuffleVector(Type type, Symbol vector1, Symbol vector2, Symbol mask) {
        int vector1Idx = addSymbol(vector1);
        int vector2Idx = addSymbol(vector2);
        int maskIdx = addSymbol(mask);
        gen.createShuffleVector(type, vector1Idx, vector2Idx, maskIdx);
        return getLastInstruction();
    }

    public Instruction createStore(Symbol destination, Symbol source, int align) {
        int destinationIdx = addSymbol(destination);
        int sourceIdx = addSymbol(source);
        // because we don't have any optimizations, we can set isVolatile to false
        boolean isVolatile = false;
        gen.createStore(destinationIdx, sourceIdx, calculateAlign(align), isVolatile);
        return getLastInstruction();
    }

    public Instruction createSwitch(Symbol condition, InstructionBlock defaultBlock, Symbol[] caseValues, InstructionBlock[] caseBlocks) {
        assert caseValues.length == caseBlocks.length;

        int conditionIdx = addSymbol(condition);
        int defaultBlockIdx = defaultBlock.getBlockIndex();

        int[] caseValuesIdx = new int[caseValues.length];
        int[] caseBlocksIdx = new int[caseBlocks.length];
        for (int i = 0; i < caseBlocks.length; i++) {
            caseValuesIdx[i] = addSymbol(caseValues[i]);
            caseBlocksIdx[i] = caseBlocks[i].getBlockIndex();
        }

        gen.createSwitch(conditionIdx, defaultBlockIdx, caseValuesIdx, caseBlocksIdx);
        return getLastInstruction();
    }

    public Instruction createSwitchOld(Symbol condition, InstructionBlock defaultBlock, long[] caseConstants, InstructionBlock[] caseBlocks) {
        assert caseConstants.length == caseBlocks.length;

        int conditionIdx = addSymbol(condition);
        int defaultBlockIdx = defaultBlock.getBlockIndex();

        int[] caseBlocksIdx = new int[caseBlocks.length];
        for (int i = 0; i < caseBlocks.length; i++) {
            caseBlocksIdx[i] = caseBlocks[i].getBlockIndex();
        }

        gen.createSwitchOld(conditionIdx, defaultBlockIdx, caseConstants, caseBlocksIdx); // TODO
        return getLastInstruction();
    }

    public Instruction createUnreachable() {
        gen.createUnreachable();
        return getLastInstruction();
    }
}
