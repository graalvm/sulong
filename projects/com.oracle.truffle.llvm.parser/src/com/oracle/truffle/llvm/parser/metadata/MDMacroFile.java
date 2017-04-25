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
package com.oracle.truffle.llvm.parser.metadata;

public final class MDMacroFile implements MDBaseNode {

    private final long type;

    private final long line;

    private final MDReference file;

    private final MDReference elements;

    private MDMacroFile(long type, long line, MDReference file, MDReference elements) {
        this.type = type;
        this.line = line;
        this.file = file;
        this.elements = elements;
    }

    public long getType() {
        return type;
    }

    public long getLine() {
        return line;
    }

    public MDReference getFile() {
        return file;
    }

    public MDReference getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return String.format("MacroFile (type=%s, line=%d, file=%s, elements=%s)", type, line, file, elements);
    }

    @Override
    public void accept(MetadataVisitor visitor) {
        visitor.visit(this);
    }

    private static final int ARGINDEX_TYPE = 1;
    private static final int ARGINDEX_LINE = 2;
    private static final int ARGINDEX_FILE = 3;
    private static final int ARGINDEX_ELEMENTS = 4;

    public static MDMacroFile create38(long[] args, MetadataList md) {
        final long type = args[ARGINDEX_TYPE];
        final long line = args[ARGINDEX_LINE];
        final MDReference file = md.getMDRefOrNullRef(args[ARGINDEX_FILE]);
        final MDReference elements = md.getMDRefOrNullRef(args[ARGINDEX_ELEMENTS]);
        return new MDMacroFile(type, line, file, elements);
    }
}
