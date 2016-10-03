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
package com.oracle.truffle.llvm.parser.bc.impl.parser.ir.module;

import java.util.List;

import com.oracle.truffle.llvm.parser.base.model.generators.ConstantGenerator;
import com.oracle.truffle.llvm.parser.base.model.generators.FunctionGenerator;
import com.oracle.truffle.llvm.parser.base.model.generators.ModuleGenerator;
import com.oracle.truffle.llvm.parser.base.model.generators.SymbolGenerator;
import com.oracle.truffle.llvm.parser.base.model.types.Type;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.*;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.constants.Constants;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.constants.ConstantsV32;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.constants.ConstantsV38;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.function.Function;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.function.FunctionV32;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.function.FunctionV38;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.metadata.Metadata;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.metadata.MetadataV32;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.metadata.MetadataV38;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.module.Module;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.module.ModuleV32;
import com.oracle.truffle.llvm.parser.bc.impl.parser.listeners.module.ModuleV38;
import com.oracle.truffle.llvm.runtime.LLVMLogger;

public enum ModuleVersion {

    LLVM_3_2(ModuleV32::new, FunctionV32::new, ConstantsV32::new, MetadataV32::new),
    LLVM_3_8(ModuleV38::new, FunctionV38::new, ConstantsV38::new, MetadataV38::new),
    DEFAULT(Module::new, Function::new, Constants::new, Metadata::new);

    @FunctionalInterface
    private interface MetadataParser {

        Metadata instantiate(Types types, List<Type> symbols, SymbolGenerator generator);
    }

    @FunctionalInterface
    private interface ConstantsParser {

        Constants instantiate(Types types, List<Type> symbols, ConstantGenerator generator);
    }

    @FunctionalInterface
    private interface ModuleParser {

        Module instantiate(ModuleVersion version, ModuleGenerator generator);
    }

    @FunctionalInterface
    private interface FunctionParser {

        Function instantiate(ModuleVersion version, Types types, List<Type> symbols, FunctionGenerator generator, int mode);
    }

    private final ModuleParser module;

    private final FunctionParser function;

    private final ConstantsParser constants;

    private final MetadataParser metadata;

    ModuleVersion(ModuleParser module, FunctionParser function, ConstantsParser constants, MetadataParser metadata) {
        this.module = module;
        this.function = function;
        this.constants = constants;
        this.metadata = metadata;
    }

    public static ModuleVersion getModuleVersion(String versionStr) {
        if (versionStr.contains("3.2")) {
            return LLVM_3_2;
        } else if (versionStr.contains("3.8")) {
            return LLVM_3_8;
        } else {
            LLVMLogger.unconditionalInfo("Couldn't parse LLVM Version, use default one");
            return DEFAULT;
        }
    }

    public Metadata createMetadata(Types types, List<Type> symbols, SymbolGenerator generator) {
        return metadata.instantiate(types, symbols, generator);
    }

    public Constants createConstants(Types types, List<Type> symbols, ConstantGenerator generator) {
        return constants.instantiate(types, symbols, generator);
    }

    public Function createFunction(Types types, List<Type> symbols, FunctionGenerator generator, int mode) {
        return function.instantiate(this, types, symbols, generator, mode);
    }

    public Module createModule(ModuleGenerator generator) {
        return module.instantiate(this, generator);
    }
}
