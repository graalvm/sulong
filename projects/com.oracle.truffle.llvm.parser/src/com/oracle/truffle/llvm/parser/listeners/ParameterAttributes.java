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
package com.oracle.truffle.llvm.parser.listeners;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesCodeEntry;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;

public class ParameterAttributes implements ParserListener {

    // https://github.com/llvm-mirror/llvm/blob/release_38/include/llvm/Bitcode/LLVMBitCodes.h#L110
    private static final int PARAMATTR_CODE_ENTRY_OLD = 1;
    private static final int PARAMATTR_CODE_ENTRY = 2;
    private static final int PARAMATTR_GRP_CODE_ENTRY = 3;

    // http://llvm.org/docs/BitCodeFormat.html#paramattr-grp-code-entry-record
    private static final int WELL_KNOWN_ATTRIBUTE_KIND = 0;
    private static final int WELL_KNOWN_INTEGER_ATTRIBUTE_KIND = 1;
    private static final int STRING_ATTRIBUTE_KIND = 3;
    private static final int STRING_VALUE_ATTRIBUTE_KIND = 4;

    // stores attributes defined in PARAMATTR_GRP_CODE_ENTRY
    private final List<AttributesGroup> attributes = new ArrayList<>();

    // store code entries defined in PARAMATTR_CODE_ENTRY
    private final List<AttributesCodeEntry> parameterCodeEntry = new ArrayList<>();

    /**
     * Get ParsedAttributeGroup by Bitcode index.
     *
     * @param idx index as it was defined in the LLVM-Bitcode, means starting with 1
     * @return found attributeGroup, or otherwise an empty List
     */
    public AttributesCodeEntry getCodeEntry(long idx) {
        if (idx <= 0 || parameterCodeEntry.size() < idx) {
            return AttributesCodeEntry.EMPTY;
        }

        return parameterCodeEntry.get((int) (idx - 1));
    }

    @Override
    public void record(long id, long[] args) {
        switch ((int) id) {
            case PARAMATTR_CODE_ENTRY_OLD:
                decodeOldCodeEntry(args);
                break;

            case PARAMATTR_CODE_ENTRY:
                decodeCodeEntry(args);
                break;

            case PARAMATTR_GRP_CODE_ENTRY:
                decodeGroupCodeEntry(args);
                break;

            default:
                break;
        }
    }

    private void decodeOldCodeEntry(long[] args) {
        final List<AttributesGroup> attrGroup = new ArrayList<>();

        for (int i = 0; i < args.length; i += 2) {
            attrGroup.add(decodeOldGroupCodeEntry(args[i], args[i + 1]));
        }

        parameterCodeEntry.add(new AttributesCodeEntry(attrGroup));
    }

    private static AttributesGroup decodeOldGroupCodeEntry(long paramIdx, long attr) {
        AttributesGroup group = new AttributesGroup(-1, paramIdx);

        if ((attr & (1L << 0)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.ZEROEXT));
        }
        if ((attr & (1L << 1)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.SIGNEXT));
        }
        if ((attr & (1L << 2)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NORETURN));
        }
        if ((attr & (1L << 3)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.INREG));
        }
        if ((attr & (1L << 4)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.SRET));
        }
        if ((attr & (1L << 5)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOUNWIND));
        }
        if ((attr & (1L << 6)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOALIAS));
        }
        if ((attr & (1L << 7)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.BYVAL));
        }
        if ((attr & (1L << 8)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NEST));
        }
        if ((attr & (1L << 9)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.READNONE));
        }
        if ((attr & (1L << 10)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.READONLY));
        }
        if ((attr & (1L << 11)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOINLINE));
        }
        if ((attr & (1L << 12)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.ALWAYSINLINE));
        }
        if ((attr & (1L << 13)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.OPTSIZE));
        }
        if ((attr & (1L << 14)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.SSP));
        }
        if ((attr & (1L << 15)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.SSPREQ));
        }
        if ((attr & (0xFFL << 16)) != 0) {
            final int align = (int) ((attr >> 16) & 0xFFL);
            group.addAttribute(new Attribute.KnownIntegerValueAttribute(Attribute.Kind.ALIGN, align));
        }
        if ((attr & (1L << 32)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOCAPTURE));
        }
        if ((attr & (1L << 33)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOREDZONE));
        }
        if ((attr & (1L << 34)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NOIMPLICITFLOAT));
        }
        if ((attr & (1L << 35)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.NAKED));
        }
        if ((attr & (1L << 36)) != 0) {
            group.addAttribute(new Attribute.KnownAttribute(Attribute.Kind.INLINEHINT));
        }
        if ((attr & (0x07L << 37)) != 0) {
            final int alignstack = 1 << ((int) ((attr >> 37) & 0x07L) - 1);
            group.addAttribute(new Attribute.KnownIntegerValueAttribute(Attribute.Kind.ALIGNSTACK, alignstack));
        }

        return group;
    }

    private void decodeCodeEntry(long[] args) {
        final List<AttributesGroup> attrGroup = new ArrayList<>();

        for (long groupId : args) {
            for (AttributesGroup attr : attributes) {
                if (attr.getGroupId() == groupId) {
                    attrGroup.add(attr);
                    break;
                }
            }
        }

        if (attrGroup.size() != args.length) {
            throw new AssertionError("there is a mismatch between defined and found group id's");
        }

        parameterCodeEntry.add(new AttributesCodeEntry(attrGroup));
    }

    private void decodeGroupCodeEntry(long[] args) {
        int i = 0;

        final long groupId = args[i++];
        final long paramIdx = args[i++];

        AttributesGroup group = new AttributesGroup(groupId, paramIdx);
        attributes.add(group);

        while (i < args.length) {
            long type = args[i++];
            switch ((int) type) {
                case WELL_KNOWN_ATTRIBUTE_KIND: {
                    Attribute.Kind attr = Attribute.Kind.decode(args[i++]);
                    group.addAttribute(new Attribute.KnownAttribute(attr));
                    break;
                }

                case WELL_KNOWN_INTEGER_ATTRIBUTE_KIND: {
                    Attribute.Kind attr = Attribute.Kind.decode(args[i++]);
                    group.addAttribute(new Attribute.KnownIntegerValueAttribute(attr, (int) args[i++]));
                    break;
                }

                case STRING_ATTRIBUTE_KIND: {
                    StringBuilder strAttr = new StringBuilder();
                    i = readString(i, args, strAttr);
                    group.addAttribute(new Attribute.StringAttribute(strAttr.toString()));
                    break;
                }

                case STRING_VALUE_ATTRIBUTE_KIND: {
                    StringBuilder strAttr = new StringBuilder();
                    i = readString(i, args, strAttr);
                    StringBuilder strVal = new StringBuilder();
                    i = readString(i, args, strVal);
                    group.addAttribute(new Attribute.StringValueAttribute(strAttr.toString(), strVal.toString()));
                    break;
                }

                default:
                    throw new RuntimeException("unexpected type: " + type);
            }
        }
    }

    private static int readString(int idx, long[] args, StringBuilder sb) {
        int i = idx;
        for (; args[i] != 0; i++) {
            sb.append((char) args[i]);
        }
        i++;
        return i;
    }

}
