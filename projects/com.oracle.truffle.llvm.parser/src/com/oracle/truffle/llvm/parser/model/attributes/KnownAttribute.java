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
package com.oracle.truffle.llvm.parser.model.attributes;

import java.util.Optional;

import com.oracle.truffle.llvm.parser.records.ParameterAttributeGroupRecord;

public final class KnownAttribute implements Attribute {

    private final ParameterAttributeGroupRecord attr;
    private final Optional<Long> value;

    public KnownAttribute(ParameterAttributeGroupRecord attr, long value) {
        this.attr = attr;
        this.value = Optional.of(value);
    }

    public KnownAttribute(ParameterAttributeGroupRecord attr) {
        this.attr = attr;
        this.value = Optional.empty();
    }

    public ParameterAttributeGroupRecord getAttr() {
        return attr;
    }

    public Optional<Long> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "KnownAttribute [attr=" + attr + ", value=" + value + "]";
    }

    @Override
    public String getIrString() {
        if (value.isPresent()) {
            if (attr == ParameterAttributeGroupRecord.ALIGN) {
                return String.format("%s %d", attr.getIrString(), value.get());
            } else {
                return String.format("%s(%d)", attr.getIrString(), value.get());
            }
        } else {
            return attr.getIrString();
        }
    }

}
