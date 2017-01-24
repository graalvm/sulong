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
package com.oracle.truffle.llvm.parser.bc.parser.bc.records;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.llvm.parser.bc.parser.bc.Operation;
import com.oracle.truffle.llvm.parser.bc.parser.bc.Parser;
import com.oracle.truffle.llvm.parser.bc.parser.bc.ParserResult;

public final class UserRecordBuilder implements Operation {

    private static final int INITIAL_BUFFER_SIZE = 128;

    private static long[] buffer = new long[INITIAL_BUFFER_SIZE];

    private final List<UserRecordOperand> operands;

    public UserRecordBuilder(List<UserRecordOperand> operands) {
        this.operands = operands;
    }

    @Override
    public Parser apply(Parser parser) {
        int bufferIndex = 0;
        ParserResult result = operands.get(0).get(parser, buffer, bufferIndex);
        buffer = result.getBuffer();
        bufferIndex = result.getBufferIndex();
        long id = result.getValue();

        for (int i = 0; i < operands.size() - 1; i++) {
            result = operands.get(i + 1).get(result.getParser(), buffer, bufferIndex);
            if (bufferIndex == result.getBufferIndex()) {
                if (buffer.length <= bufferIndex) {
                    buffer = Arrays.copyOf(buffer, bufferIndex * 2);
                }
                buffer[bufferIndex++] = result.getValue();
            } else {
                buffer = result.getBuffer();
                bufferIndex = result.getBufferIndex();
            }
        }

        result.getParser().handleRecord(id, buffer, bufferIndex);

        return result.getParser();
    }
}
