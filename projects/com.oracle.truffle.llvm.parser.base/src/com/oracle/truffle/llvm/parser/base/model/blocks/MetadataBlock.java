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
package com.oracle.truffle.llvm.parser.base.model.blocks;

import com.oracle.truffle.llvm.parser.base.model.metadata.MetadataBaseNode;
import com.oracle.truffle.llvm.parser.base.model.types.IntegerConstantType;
import com.oracle.truffle.llvm.parser.base.model.types.MetaType;
import com.oracle.truffle.llvm.parser.base.model.types.MetadataConstantType;
import com.oracle.truffle.llvm.parser.base.model.types.Type;

import java.util.ArrayList;
import java.util.List;

public class MetadataBlock {

    protected final List<MetadataBaseNode> metadata;

    protected int startIndex = 0;

    public MetadataBlock() {
        metadata = new ArrayList<>();
    }

    public MetadataBlock(MetadataBlock orig) {
        this.metadata = new ArrayList<>(orig.metadata);
        this.startIndex = orig.startIndex;
    }

    public void setStartIndex(int index) {
        startIndex = index;
    }

    public void add(MetadataBaseNode element) {
        metadata.add(element);
    }

    public MetadataBaseNode get(int index) {
        return metadata.get(index - startIndex);
    }

    public MetadataBaseNode getAbsolute(int index) { // TOOD: do index recalculation in getReference
        return metadata.get(index);
    }

    public int size() {
        return metadata.size();
    }

    public MetadataReference getReference(int index) {
        if (index == 0) {
            return voidRef;
        } else {
            return new Reference(index);
        }
    }

    public MetadataReference getReference(long index) {
        return getReference((int) index);
    }

    public int getCurrentIndex() {
        return startIndex + metadata.size();
    }

    public MetadataReference getReference(Type t) {
        if (t instanceof MetadataConstantType) {
            int index = (int) ((MetadataConstantType) t).getValue();
            return getReference(index);
        } else if (t instanceof MetaType && (MetaType) t == MetaType.VOID) {
            return voidRef;
        } else if (t instanceof IntegerConstantType) {
            int index = (int) ((IntegerConstantType) t).getValue();
            if (index == 0) { // We only allow 0 as integer constant
                return voidRef;
            }
        }
        throw new RuntimeException("Invalid reference type: " + t);
    }

    /**
     * Based on the idea of Optional, but used for automatic forward reference lookup.
     */
    public interface MetadataReference extends MetadataBaseNode {
        boolean isPresent();

        MetadataBaseNode get();

        int getIndex();
    }

    @Override
    public String toString() {
        return "MetadataBlock [startIndex=" + startIndex + ", metadata=" + metadata + "]";
    }

    public static final VoidReference voidRef = new VoidReference();

    public static final class VoidReference implements MetadataReference {

        private VoidReference() {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public MetadataBaseNode get() {
            // TODO: better exception
            throw new IndexOutOfBoundsException("That's an empty reference");
        }

        @Override
        public int getIndex() {
            return -1;
        }

        @Override
        public String toString() {
            return "VoidReference";
        }
    }

    public final class Reference implements MetadataReference {
        public final int index;

        private Reference(int index) {
            this.index = index;
        }

        @Override
        public boolean isPresent() {
            return metadata.size() > index;
        }

        @Override
        public MetadataBaseNode get() {
            return metadata.get(index - startIndex);
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + index;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Reference other = (Reference) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (index != other.index) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "!" + index;
        }

        private MetadataBlock getOuterType() {
            return MetadataBlock.this;
        }
    }

}
