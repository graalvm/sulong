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
package com.oracle.truffle.llvm.runtime.types.symbols;

import java.util.regex.Pattern;

public final class LLVMIdentifier {

    public static final String UNKNOWN = "<anon>";

    private static final Pattern GLOBAL_VARNAME_PATTERN = Pattern.compile("@(\\\\01)?(\"[^\"]+\"|[\\w\\d\\u0024_\\u002e]+)");
    private static final Pattern LOCAL_VARNAME_PATTERN = Pattern.compile("%(\"[^\"]+\"|[\\w\\d\\u0024_\\u002e]+)");

    private LLVMIdentifier() {
    }

    public static String toGlobalIdentifier(String name) {
        if (GLOBAL_VARNAME_PATTERN.matcher(name).matches()) {
            // already a global identifier
            return name;
        } else {
            return "@" + escapeNameIfNecessary(name);
        }
    }

    public static String toLocalIdentifier(String name) {
        if (LOCAL_VARNAME_PATTERN.matcher(name).matches()) {
            // already a global identifier
            return name;
        } else {
            return "%" + escapeNameIfNecessary(name);
        }
    }

    private static final Pattern NUMERIC_VARNAME_PATTERN = Pattern.compile("\\d+");

    public static String toBlockName(String name) {
        if (NUMERIC_VARNAME_PATTERN.matcher(name).matches()) {
            return String.format("%%\"%s\"", name);
        } else {
            return "%" + escapeNameIfNecessary(name);
        }
    }

    private static final String IMPLICIT_BLOCK_PREFIX = "implicit";

    public static String toImplicitBlockName(int label) {
        return IMPLICIT_BLOCK_PREFIX + String.valueOf(label);
    }

    public static String extractLabelFromImplicitBlockName(String name) {
        return name.substring(IMPLICIT_BLOCK_PREFIX.length());
    }

    public static boolean isImplicitBlockName(String name) {
        return name.startsWith(IMPLICIT_BLOCK_PREFIX);
    }

    public static String toTypeIdentifier(String name) {
        return escapeNameIfNecessary(name);
    }

    private static final Pattern UNESCAPED_VARNAME_PATTERN = Pattern.compile("[\\w\\d\\u0024_\\u002e]+");
    private static final Pattern ESCAPED_VARNAME_PATTERN = Pattern.compile("(%|@|@\\\\01)?\"[^\"]+\"");

    private static String escapeNameIfNecessary(String unescaped) {
        // see http://releases.llvm.org/3.8.1/docs/LangRef.html#identifiers
        if (UNESCAPED_VARNAME_PATTERN.matcher(unescaped).matches()) {
            return unescaped;
        } else if (ESCAPED_VARNAME_PATTERN.matcher(unescaped).matches()) {
            // do not escape an already escaped name again
            return unescaped;
        }

        final StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < unescaped.length(); i++) {
            final char c = unescaped.charAt(i);
            if (c == '\"' || c < ' ' || c > '~') {
                // use the format "\xx" where xx is the hex-value of c
                builder.append("\\");
                final String hexVal = Integer.toHexString(c);
                for (int j = 0; j < hexVal.length() - 2; j++) {
                    builder.append('0');
                }
                builder.append(hexVal);
            } else {
                builder.append(c);
            }
        }
        builder.append('\"');
        return builder.toString();
    }
}
