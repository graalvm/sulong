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

import com.oracle.truffle.llvm.parser.base.datalayout.DataLayoutConverter;
import com.oracle.truffle.llvm.parser.base.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.base.model.blocks.MetadataBlock;
import com.oracle.truffle.llvm.parser.base.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.base.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.base.model.symbols.Symbol;
import com.oracle.truffle.llvm.parser.base.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.base.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.base.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.base.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.base.model.types.FunctionType;
import com.oracle.truffle.llvm.parser.base.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.base.model.types.PointerType;
import com.oracle.truffle.llvm.parser.base.model.types.Type;

public class InstructionGeneratorFacade {
    private static final String x86DataTargetLayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64-S128";
    public static final DataLayoutConverter.DataSpecConverter targetDataLayout = DataLayoutConverter.getConverter(x86DataTargetLayout);

    private final FunctionDefinition def;
    private InstructionBlock gen;

    public InstructionGeneratorFacade(String name, int blocks, Type retType) {
        this(name, blocks, retType, new Type[]{}, false);
    }

    public InstructionGeneratorFacade(String name, int blocks, Type retType, Type[] params, boolean varArgs) {
        FunctionType func = new FunctionType(retType, params, varArgs);
        this.def = new FunctionDefinition(func, new MetadataBlock());
        def.setName(name);
        this.def.allocateBlocks(blocks);
        this.gen = (InstructionBlock) this.def.generateBlock();
    }

    public FunctionDefinition getFunctionDefinition() {
        return def;
    }

    public void nextBlock() {
        this.gen = (InstructionBlock) this.def.generateBlock();
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
        return gen.getInstruction(gen.getInstructionCount() - 1);
    }

    public Instruction createAllocate(Type type) {
        Type pointerType = new PointerType(type);
        int count = addSymbol(createI32Constant(0));
        int align = addSymbol(createI32Constant(type.getAlignment(targetDataLayout)));
        gen.createAllocation(pointerType, count, align);
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

    public Instruction createLoad(Instruction source) {
        Type type = ((PointerType) source.getType()).getPointeeType();
        int sourceIdx = addSymbol(source);
        int alignIdx = addSymbol(createI32Constant(type.getAlignment(targetDataLayout)));
        // because we don't have any optimizations, we can set isVolatile to false
        boolean isVolatile = false;
        gen.createLoad(type, sourceIdx, alignIdx, isVolatile);
        return getLastInstruction();
    }

    public Instruction createInsertelement(Instruction vector, Constant value, int index) {
        Type type = vector.getType();
        int vectorIdx = addSymbol(vector);
        int valueIdx = addSymbol(value);
        int indexIdx = addSymbol(new IntegerConstant(IntegerType.INTEGER, index));
        gen.createInsertElement(type, vectorIdx, indexIdx, valueIdx);
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

    public Instruction createExtractelement(Instruction vector, int index) {
        Type type = vector.getType().getIndexType(index);
        int vectorIdx = addSymbol(vector);
        int indexIdx = addSymbol(createI32Constant(index));
        gen.createExtractElement(type, vectorIdx, indexIdx);
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
}
