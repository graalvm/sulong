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
package com.oracle.truffle.llvm.parser.macho;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oracle.truffle.llvm.parser.macho.MachOSegmentCommand.MachOSection;

public final class MachOFile {

    private static final long MH_MAGIC = 0xFEEDFACEL;
    private static final long MH_CIGAM = 0xCEFAEDFEL;
    private static final long MH_MAGIC_64 = 0xFEEDFACFL;
    private static final long MH_CIGAM_64 = 0xCFFAEDFEL;

    private static final String LLVM_SEGMENT = "__LLVM";
    private static final String INTERMEDIATE_SEGMENT = "";
    private static final String BITCODE_SECTION = "__bitcode";
    private static final String BUNDLE_SECTION = "__bundle";
    private static final String WLLVM_SEGMENT = "__WLLVM";
    private static final String WLLVM_BITCODE_SECTION = "__llvm_bc";

    private static final int MH_OBJECT = 0x1; /* relocatable object file */
    private static final int MH_EXECUTE = 0x2; /* demand paged executable file */
    // currently unused types:
    @SuppressWarnings("unused") private static final int MH_FVMLIB = 0x3;
    @SuppressWarnings("unused") private static final int MH_CORE = 0x4;
    @SuppressWarnings("unused") private static final int MH_PRELOAD = 0x5;
    @SuppressWarnings("unused") private static final int MH_DYLIB = 0x6;
    @SuppressWarnings("unused") private static final int MH_DYLINKER = 0x7;
    @SuppressWarnings("unused") private static final int MH_BUNDLE = 0x8;
    @SuppressWarnings("unused") private static final int MH_DYLIB_STUB = 0x9;

    private final MachOHeader header;
    private final MachOLoadCommandTable loadCommandTable;
    private final ByteBuffer buffer;

    private MachOFile(MachOHeader header, MachOLoadCommandTable loadCommandTable, ByteBuffer buffer) {
        this.header = header;
        this.loadCommandTable = loadCommandTable;
        this.buffer = buffer;
    }

    public MachOSegmentCommand getSegment(String name) {
        return loadCommandTable.getSegment(name);
    }

    public MachOHeader getHeader() {
        return header;
    }

    public List<String> getDyLibs() {
        List<String> dylibs = new ArrayList<>();
        for (MachOLoadCommand lc : loadCommandTable.getLoadCommands()) {
            if (lc instanceof MachODylibCommand) {
                dylibs.add(((MachODylibCommand) lc).getName());
            }
        }
        return dylibs;
    }

    public ByteBuffer extractBitcode() {

        ByteBuffer bc = null;

        switch (header.getFileType()) {
            case MH_OBJECT:
                bc = extractBitcodeFromObject();
                break;
            case MH_EXECUTE:
                if (loadCommandTable.getSegment(WLLVM_SEGMENT) != null) {
                    bc = extractBitcodeFromWLLVMExecute();
                } else {
                    bc = extractBitcodeFromExecute();
                }
                break;
            default:
                throw new RuntimeException("Mach-O file type not supported!");
        }

        bc.order(ByteOrder.LITTLE_ENDIAN);
        return bc;
    }

    private ByteBuffer extractBitcodeFromExecute() {
        ByteBuffer sectionData = getSectionData(LLVM_SEGMENT, BUNDLE_SECTION);
        Xar xar = Xar.create(sectionData);

        Document bcDoc = xar.getToc().getTableOfContents();
        NodeList dataNodes = bcDoc.getElementsByTagName("data");

        // extract all bitcodes from the archive
        ByteBuffer[] bitcodes = new ByteBuffer[dataNodes.getLength()];

        if (bitcodes.length == 1) {
            return extractBcFromXar(xar, dataNodes.item(0));
        } else {
            throw new RuntimeException("Multiple bitcode files are found in the executable! Consider building with WLLVM and executing mx inject afterwards.");
        }

    }

    private static ByteBuffer extractBcFromXar(Xar xar, Node data) {
        NodeList dataChildren = data.getChildNodes();

        long offset = -1L;
        long size = -1L;  // not specified whether "length" or "size" is needed

        Node n = dataChildren.item(0);

        while (n != null) {
            if (n.getNodeName().equals("offset")) {
                offset = Long.parseLong(n.getTextContent());
            } else if (n.getNodeName().equals("size")) {
                size = Long.parseLong(n.getTextContent());
            }
            n = n.getNextSibling();
        }

        if (size == -1 || offset == -1) {
            throw new RuntimeException("Error while attempting to extract bitcode from a xar archive!");
        }

        ByteBuffer xarHeap = xar.getHeap();
        int oldLimit = xarHeap.limit();

        xarHeap.position((int) offset);
        xarHeap.limit((int) (offset + size));

        ByteBuffer bc = xarHeap.slice();
        xarHeap.limit(oldLimit);

        return bc;
    }

    private ByteBuffer extractBitcodeFromObject() {
        return getSectionData(INTERMEDIATE_SEGMENT, BITCODE_SECTION);
    }

    private ByteBuffer extractBitcodeFromWLLVMExecute() {
        return getSectionData(WLLVM_SEGMENT, WLLVM_BITCODE_SECTION);
    }

    public ByteBuffer getSectionData(String segment, String section) {
        MachOSegmentCommand seg = loadCommandTable.getSegment(segment);

        if (seg == null) {
            throw new RuntimeException("Mach-O file does not contain a " + segment + " segment!");
        }

        MachOSection sect = seg.getSection(section);

        if (sect == null) {
            throw new RuntimeException("Mach-O file does not contain a " + section + " section!");
        }

        long offset = sect.getOffset();
        long size = sect.getSize();
        int oldLimit = buffer.limit();

        buffer.position((int) offset);
        buffer.limit((int) (offset + size));

        ByteBuffer data = buffer.slice().duplicate();

        buffer.limit(oldLimit);

        return data;
    }

    public static MachOFile create(ByteBuffer data) {
        long magic = Integer.toUnsignedLong(data.getInt());

        if (!isMachOMagicNumber(magic)) {
            throw new IllegalArgumentException("Invalid Mach-O file!");
        }

        boolean is64Bit = isMachO64MagicNumber(magic);

        ByteOrder byteOrder = data.order();
        if (isReversedByteOrder(magic)) {
            if (byteOrder.equals(ByteOrder.BIG_ENDIAN)) {
                data.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                data.order(ByteOrder.BIG_ENDIAN);
            }
        }

        MachOHeader header = MachOHeader.create(data, is64Bit);
        MachOLoadCommandTable loadCommandTable = MachOLoadCommandTable.create(header, data, is64Bit);

        return new MachOFile(header, loadCommandTable, data.duplicate());
    }

    public static boolean isMachOMagicNumber(long magic) {
        return isMachO32MagicNumber(magic) || isMachO64MagicNumber(magic);
    }

    public static boolean isMachO32MagicNumber(long magic) {
        return magic == MH_MAGIC || magic == MH_CIGAM;
    }

    public static boolean isMachO64MagicNumber(long magic) {
        return magic == MH_MAGIC_64 || magic == MH_CIGAM_64;
    }

    public static boolean isReversedByteOrder(long magic) {
        return magic == MH_CIGAM || magic == MH_CIGAM_64;
    }

}
