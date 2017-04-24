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
package com.oracle.truffle.llvm.parser.metadata;

import com.oracle.truffle.llvm.parser.metadata.subtypes.MDName;

public final class MDEnumerator extends MDName implements MDBaseNode {

    private final long value;

    private MDEnumerator(MDReference name, long value) {
        super(name);
        this.value = value;
    }

    @Override
    public void accept(MetadataVisitor visitor) {
        visitor.visit(this);
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Enumerator (name=%s, value=%d)", getName(), value);
    }

    private static final int ARGINDEX_38_VALUE = 1;
    private static final int ARGINDEX_38_NAME = 2;

    public static MDEnumerator create38(long[] args, MetadataList md) {
        final long value = ParseUtil.unrotateSign(args[ARGINDEX_38_VALUE]);
        final MDReference name = md.getMDRefOrNullRef(args[ARGINDEX_38_NAME]);
        return new MDEnumerator(name, value);
    }

    private static final int ARGINDEX_32_NAME = 1;
    private static final int ARGINDEX_32_VALUE = 2;

    public static MDEnumerator create32(MDTypedValue[] args) {
        final MDReference name = ParseUtil.getReference(args[ARGINDEX_32_NAME]);
        final long value = ParseUtil.asInt64(args[ARGINDEX_32_VALUE]);
        return new MDEnumerator(name, value);
    }
}
