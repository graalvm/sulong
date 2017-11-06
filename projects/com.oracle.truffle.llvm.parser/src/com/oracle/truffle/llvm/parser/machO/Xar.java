/* Copyright (c) 2017, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.machO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class Xar {

    private static final long XAR_MAGIC = 0x78617221L;

    private final XarHeader header;
    private final XarTOC toc;
    private final ByteBuffer heap;

    public Xar(XarHeader header, XarTOC toc, ByteBuffer heap) {
        this.header = header;
        this.toc = toc;
        this.heap = heap;
    }

    public XarHeader getHeader() {
        return header;
    }

    public XarTOC getToc() {
        return toc;
    }

    public ByteBuffer getHeap() {
        return heap;
    }

    public static Xar create(ByteBuffer data) {
        int magic = data.getInt();
        if (magic != XAR_MAGIC) {
            throw new RuntimeException("No valid xar file!");
        }

        XarHeader header = XarHeader.create(data);
        XarTOC toc = XarTOC.create(data, header);
        ByteBuffer heap = data.slice();

        return new Xar(header, toc, heap);
    }

    public static final class XarHeader {
        private final short size;
        private final short version;
        private final long tocComprSize;
        private final long tocUncomprSize;
        private final int checksumAlgo;

        private XarHeader(short size, short version, long tocComprSize, long tocUncomprSize, int checksumAlgo) {
            super();
            this.size = size;
            this.version = version;
            this.tocComprSize = tocComprSize;
            this.tocUncomprSize = tocUncomprSize;
            this.checksumAlgo = checksumAlgo;
        }

        public short getSize() {
            return size;
        }

        public short getVersion() {
            return version;
        }

        public long getTocComprSize() {
            return tocComprSize;
        }

        public long getTocUncomprSize() {
            return tocUncomprSize;
        }

        public int getChecksumAlgo() {
            return checksumAlgo;
        }

        public static XarHeader create(ByteBuffer data) {
            short size = data.getShort();
            short version = data.getShort();
            long tocComprSize = data.getLong();
            long tocUncomprSize = data.getLong();
            int checksumAlgo = data.getInt();

            return new XarHeader(size, version, tocComprSize, tocUncomprSize, checksumAlgo);
        }
    }

    public static final class XarTOC {

        private final Document tableOfContents;

        private XarTOC(Document toc) {
            this.tableOfContents = toc;
        }

        public Document getTableOfContents() {
            return tableOfContents;
        }

        public static XarTOC create(ByteBuffer data, XarHeader header) {
            int comprSize = (int) header.getTocComprSize();
            int uncomprSize = (int) header.getTocUncomprSize();

            byte[] compressedData = new byte[comprSize];
            data.get(compressedData);

            // decompress
            Inflater decompresser = new Inflater();
            decompresser.setInput(compressedData, 0, comprSize);
            byte[] uncompressedData = new byte[uncomprSize];
            try {
                decompresser.inflate(uncompressedData);
            } catch (DataFormatException e) {
                throw new RuntimeException("DataFormatException when decompressing xar table of contents!");
            }
            decompresser.end();

            try {
                Document xmlTOC = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(uncompressedData));
                return new XarTOC(xmlTOC);
            } catch (SAXException | IOException | ParserConfigurationException e1) {
                throw new RuntimeException("Could not parse xar table of contents xml!");
            }

        }
    }
}
