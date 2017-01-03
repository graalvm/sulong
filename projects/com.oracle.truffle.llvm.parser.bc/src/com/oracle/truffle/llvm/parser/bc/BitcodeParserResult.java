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
package com.oracle.truffle.llvm.parser.bc;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.api.datalayout.DataLayoutConverter;
import com.oracle.truffle.llvm.parser.api.model.Model;
import com.oracle.truffle.llvm.parser.api.model.ModelModule;
import com.oracle.truffle.llvm.parser.api.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.bc.parser.ir.LLVMParser;
import com.oracle.truffle.llvm.parser.bc.parser.listeners.ModuleVersion;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;

public final class BitcodeParserResult {
    private final Model model;
    private final LLVMPhiManager phis;
    private final StackAllocation stackAllocation;
    private final LLVMLabelList labels;

    private BitcodeParserResult(Model model, LLVMPhiManager phis, StackAllocation stackAllocation, LLVMLabelList labels) {
        this.model = model;
        this.phis = phis;
        this.stackAllocation = stackAllocation;
        this.labels = labels;
    }

    public Model getModel() {
        return model;
    }

    public LLVMPhiManager getPhis() {
        return phis;
    }

    public StackAllocation getStackAllocation() {
        return stackAllocation;
    }

    public LLVMLabelList getLabels() {
        return labels;
    }

    public static BitcodeParserResult getFromSource(Source source) {
        final Model model = new Model();
        new LLVMParser(model).parse(ModuleVersion.getModuleVersion(LLVMOptions.ENGINE.llvmVersion()), source);

        return getFromModel(model);
    }

    public static BitcodeParserResult getFromModel(Model model) {
        final LLVMPhiManager phis = LLVMPhiManager.generate(model);
        final StackAllocation stackAllocation = StackAllocation.generate(model);
        final LLVMLabelList labels = LLVMLabelList.generate(model);

        final TargetDataLayout layout = ((ModelModule) model.createModule()).getTargetDataLayout();
        final DataLayoutConverter.DataSpecConverter targetDataLayout = layout != null ? DataLayoutConverter.getConverter(layout.getDataLayout()) : null;
        LLVMMetadata.generate(model, targetDataLayout);

        return new BitcodeParserResult(model, phis, stackAllocation, labels);
    }
}
