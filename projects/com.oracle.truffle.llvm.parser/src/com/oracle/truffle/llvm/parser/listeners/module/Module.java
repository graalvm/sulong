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
package com.oracle.truffle.llvm.parser.listeners.module;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.llvm.parser.listeners.IRVersionController;
import com.oracle.truffle.llvm.parser.listeners.Identification;
import com.oracle.truffle.llvm.parser.listeners.ParserListener;
import com.oracle.truffle.llvm.parser.listeners.Types;
import com.oracle.truffle.llvm.parser.listeners.ValueSymbolTable;
import com.oracle.truffle.llvm.parser.listeners.metadata.Metadata;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.generators.FunctionGenerator;
import com.oracle.truffle.llvm.parser.model.generators.ModuleGenerator;
import com.oracle.truffle.llvm.parser.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.model.target.TargetInformation;
import com.oracle.truffle.llvm.parser.model.target.TargetTriple;
import com.oracle.truffle.llvm.parser.records.ModuleRecord;
import com.oracle.truffle.llvm.parser.records.Records;
import com.oracle.truffle.llvm.parser.scanner.Block;
import com.oracle.truffle.llvm.runtime.LLVMLogger;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.Type;

public final class Module implements ParserListener {

    protected final ModuleGenerator generator;

    private final IRVersionController version;

    private int mode = 1;

    protected final Types types;

    private final List<TargetInformation> info = new ArrayList<>();

    protected final List<FunctionType> functions = new ArrayList<>();

    protected final List<Type> symbols = new ArrayList<>();

    public Module(IRVersionController version, ModuleGenerator generator) {
        this.version = version;
        this.generator = generator;
        types = new Types(generator);
    }

    private void createFunction(long[] args) {
        // this allows us to switch module version at runtime
        FunctionType type = version.createModuleVersionHelper().getFunctionType(this, args[0]);
        boolean isPrototype = args[2] != 0;

        generator.createFunction(type, isPrototype);
        symbols.add(type);
        if (!isPrototype) {
            functions.add(type);
        }
    }

    private void createGlobalVariable(long[] args) {
        // this allows us to switch module version at runtime
        Type type = version.createModuleVersionHelper().getGlobalType(this, args[0]);
        boolean isConstant = (args[1] & 1) == 1;
        int initialiser = (int) args[2];
        long linkage = args[3];
        int align = (int) args[4];

        long visibility = Visibility.DEFAULT.getEncodedValue();
        if (args.length >= 7) {
            visibility = args[6];
        }

        generator.createGlobal(type, isConstant, initialiser, align, linkage, visibility);
        symbols.add(type);
    }

    private void createGlobalAliasNew(long[] args) {
        // this allows us to switch module version at runtime
        Type type = version.createModuleVersionHelper().getGlobalType(this, args[0]);
        // idx = 1 is address space information
        int value = (int) args[2];
        long linkage = args[3];

        generator.createAlias(type, value, linkage, Visibility.DEFAULT.ordinal());
        symbols.add(type);
    }

    private void createGlobalAliasOld(long[] args) {
        // this allows us to switch module version at runtime
        Type type = version.createModuleVersionHelper().getGlobalType(this, args[0]);
        int value = (int) args[1];
        long linkage = args[2];

        generator.createAlias(type, value, linkage, Visibility.DEFAULT.ordinal());
        symbols.add(type);
    }

    @Override
    public ParserListener enter(Block block) {
        switch (block) {
            case MODULE:
                return this; // Entering from root

            case CONSTANTS:
                return version.createConstants(types, symbols, generator);

            case FUNCTION: {
                FunctionType function = functions.remove(0);

                FunctionGenerator gen = generator.generateFunction();

                List<Type> sym = new ArrayList<>(symbols);

                for (Type arg : function.getArgumentTypes()) {
                    gen.createParameter(arg);
                    sym.add(arg);
                }

                return version.createFunction(types, sym, gen, mode);
            }
            case IDENTIFICATION:
                return new Identification(version);

            case TYPE:
                return types;

            case VALUE_SYMTAB:
                return new ValueSymbolTable(generator);

            case METADATA:
            case METADATA_KIND:
                return new Metadata(types, symbols, generator);

            default:
                LLVMLogger.info("Entering Unknown Block inside Module: " + block);
                return ParserListener.DEFAULT;
        }
    }

    @Override
    public void exit() {
        generator.exitModule();
    }

    @Override
    public void record(long id, long[] args) {
        final ModuleRecord record = ModuleRecord.decode(id);
        switch (record) {
            case VERSION:
                mode = (int) args[0];
                break;

            case TARGET_TRIPLE:
                info.add(new TargetTriple(Records.toString(args)));
                break;

            case TARGET_DATALAYOUT:
                final TargetDataLayout layout = TargetDataLayout.fromString(Records.toString(args));
                info.add(layout);
                generator.createTargetDataLayout(layout);
                break;

            case GLOBAL_VARIABLE:
                createGlobalVariable(args);
                break;

            case FUNCTION:
                createFunction(args);
                break;

            case ALIAS:
                createGlobalAliasNew(args);
                break;
            case ALIAS_OLD:
                createGlobalAliasOld(args);
                break;

            default:
                LLVMLogger.info("Unknown Top-Level Record: " + Records.describe(id, args));
                break;
        }
    }
}
