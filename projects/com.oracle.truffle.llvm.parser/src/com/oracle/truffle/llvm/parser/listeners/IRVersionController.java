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
package com.oracle.truffle.llvm.parser.listeners;

import java.util.List;

import com.oracle.truffle.llvm.parser.listeners.constants.Constants;
import com.oracle.truffle.llvm.parser.listeners.constants.ConstantsVersion.ConstantsV1;
import com.oracle.truffle.llvm.parser.listeners.constants.ConstantsVersion.ConstantsV2;
import com.oracle.truffle.llvm.parser.listeners.function.Function;
import com.oracle.truffle.llvm.parser.listeners.function.FunctionVersion.FunctionV1;
import com.oracle.truffle.llvm.parser.listeners.function.FunctionVersion.FunctionV2;
import com.oracle.truffle.llvm.parser.listeners.metadata.Metadata;
import com.oracle.truffle.llvm.parser.listeners.metadata.MetadataVersion.MetadataV1;
import com.oracle.truffle.llvm.parser.listeners.metadata.MetadataVersion.MetadataV2;
import com.oracle.truffle.llvm.parser.listeners.module.Module;
import com.oracle.truffle.llvm.parser.listeners.module.ModuleVersionHelper;
import com.oracle.truffle.llvm.parser.listeners.module.ModuleVersionHelper.ModuleV1;
import com.oracle.truffle.llvm.parser.listeners.module.ModuleVersionHelper.ModuleV2;
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.generators.ConstantGenerator;
import com.oracle.truffle.llvm.parser.model.generators.FunctionGenerator;
import com.oracle.truffle.llvm.parser.model.generators.SymbolGenerator;
import com.oracle.truffle.llvm.runtime.types.Type;

public final class IRVersionController {

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

        ModuleVersionHelper instantiate();
    }

    @FunctionalInterface
    private interface FunctionParser {

        Function instantiate(IRVersionController version, Types types, List<Type> symbols, FunctionGenerator generator, int mode);
    }

    private enum IRVersion {
        DEFAULT(ModuleV1::new, FunctionV1::new, ConstantsV1::new, MetadataV1::new, -1),
        LLVM_1(ModuleV1::new, FunctionV1::new, ConstantsV1::new, MetadataV1::new, 1),
        LLVM_2(ModuleV2::new, FunctionV2::new, ConstantsV2::new, MetadataV2::new, 2);

        private final int version;
        private final FunctionParser function;
        private final ConstantsParser constants;
        private final MetadataParser metadata;
        private final ModuleParser module;

        IRVersion(ModuleParser module, FunctionParser function, ConstantsParser constants, MetadataParser metadata, int version) {
            this.function = function;
            this.constants = constants;
            this.metadata = metadata;
            this.module = module;
            this.version = version;
        }
    }

    private IRVersion version;

    public IRVersionController() {
        version = IRVersion.DEFAULT;
    }

    public void setVersion(int versionNumber) {
        if (version == IRVersion.DEFAULT) {
            for (IRVersion candidate : IRVersion.values()) {
                if (candidate.version == versionNumber) {
                    version = candidate;
                    return;
                }
            }
        }
        throw new IllegalArgumentException("version: " + version);
    }

    public Metadata createMetadata(Types types, List<Type> symbols, SymbolGenerator generator) {
        return version.metadata.instantiate(types, symbols, generator);
    }

    public Constants createConstants(Types types, List<Type> symbols, ConstantGenerator generator) {
        return version.constants.instantiate(types, symbols, generator);
    }

    public Function createFunction(Types types, List<Type> symbols, FunctionGenerator generator, int mode) {
        return version.function.instantiate(this, types, symbols, generator, mode);
    }

    public ModuleVersionHelper createModuleVersionHelper() {
        return version.module.instantiate();
    }

    public Module createModule(ModelModule generator) {
        return new Module(this, generator);
    }

}
