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

package com.oracle.truffle.llvm.parser.scanner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.llvm.parser.elf.ElfDynamicSection;
import com.oracle.truffle.llvm.parser.elf.ElfFile;
import com.oracle.truffle.llvm.parser.elf.ElfSectionHeaderTable.Entry;
import com.oracle.truffle.llvm.parser.listeners.Module;
import com.oracle.truffle.llvm.parser.listeners.ParserListener;
import com.oracle.truffle.llvm.parser.macho.MachOFile;
import com.oracle.truffle.llvm.parser.model.ModelModule;

public final class LLVMScanner {

    private static final String CHAR6 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._";

    private static final int DEFAULT_ID_SIZE = 2;

    private static final long BC_MAGIC_WORD = 0xdec04342L; // 'BC' c0de
    private static final long WRAPPER_MAGIC_WORD = 0x0B17C0DEL;
    private static final long ELF_MAGIC_WORD = 0x464C457FL;

    private static final int MAX_BLOCK_DEPTH = 3;

    private final List<List<AbbreviatedRecord>> abbreviationDefinitions = new ArrayList<>();

    private final BitStream bitstream;

    private final Map<Block, List<List<AbbreviatedRecord>>> defaultAbbreviations = new HashMap<>();

    private final Deque<ScannerState> parents = new ArrayDeque<>(MAX_BLOCK_DEPTH);

    private final RecordBuffer recordBuffer = new RecordBuffer();

    private Block block;

    private int idSize;

    private ParserListener parser;

    private long offset;

    private LLVMScanner(BitStream bitstream, ParserListener listener) {
        this.bitstream = bitstream;
        this.parser = listener;
        this.block = Block.ROOT;
        this.idSize = DEFAULT_ID_SIZE;
        this.offset = 0;
    }

    public static ModelModule parse(ByteBuffer bytes) {
        final ModelModule model = new ModelModule();

        ByteBuffer b = bytes.duplicate();
        b.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bitcode;
        long magicWord = Integer.toUnsignedLong(b.getInt());
        if (Long.compareUnsigned(magicWord, BC_MAGIC_WORD) == 0) {
            bitcode = b;
        } else if (magicWord == WRAPPER_MAGIC_WORD) {
            // Version
            b.getInt();
            // Offset32
            long offset = b.getInt();
            // Size32
            long size = b.getInt();
            b.position((int) offset);
            b.limit((int) (offset + size));
            bitcode = b.slice();
        } else if (magicWord == ELF_MAGIC_WORD) {
            ElfFile elfFile = ElfFile.create(b);
            Entry llvmbc = elfFile.getSectionHeaderTable().getEntry(".llvmbc");
            if (llvmbc == null) {
                throw new RuntimeException("ELF File does not contain an .llvmbc section.");
            }
            ElfDynamicSection dynamicSection = elfFile.getDynamicSection();
            if (dynamicSection != null) {
                List<String> libraries = dynamicSection.getDTNeeded();
                List<String> paths = dynamicSection.getDTRPath();
                model.addLibraries(libraries);
                model.addLibraryPaths(paths);
            }
            long offset = llvmbc.getOffset();
            long size = llvmbc.getSize();
            b.position((int) offset);
            b.limit((int) (offset + size));
            bitcode = b.slice();
        } else if (MachOFile.isMachOMagicNumber(magicWord)) {
            b.position(0);
            MachOFile machOFile = MachOFile.create(b);

            List<String> libraries = machOFile.getDyLibs();
            model.addLibraries(libraries);

            bitcode = machOFile.extractBitcode();

            long wrapperMagic = Integer.toUnsignedLong(bitcode.getInt(bitcode.position()));
            if (wrapperMagic == WRAPPER_MAGIC_WORD) {
                // the first read did not change position
                bitcode.position(bitcode.position() + 4);
                // Version
                bitcode.getInt();
                // Offset32
                long offset = bitcode.getInt();
                // Size32
                long size = bitcode.getInt();
                bitcode.position((int) offset);
                bitcode.limit((int) (offset + size));
                bitcode = bitcode.slice();
            }

        } else {
            throw new RuntimeException("Not a valid input file!");
        }

        final BitStream bitstream = BitStream.create(bitcode);
        final LLVMScanner scanner = new LLVMScanner(bitstream, new Module(model));
        parseBitcodeBlock(scanner);
        return model;
    }

    public static boolean isSupportedFile(ByteBuffer bytes) {
        ByteBuffer duplicate = bytes.duplicate();
        BitStream bs = BitStream.create(duplicate);
        long magicWord = bs.read(0, Integer.SIZE);
        return magicWord == BC_MAGIC_WORD || magicWord == WRAPPER_MAGIC_WORD || magicWord == ELF_MAGIC_WORD || MachOFile.isMachOMagicNumber(magicWord);
    }

    private static void parseBitcodeBlock(LLVMScanner scanner) {
        final long actualMagicWord = scanner.read(Integer.SIZE);
        if (actualMagicWord != BC_MAGIC_WORD) {
            throw new RuntimeException("Not a valid Bitcode File!");
        }

        while (scanner.offset < scanner.bitstream.size()) {
            scanner.scanNext();
        }
    }

    private static <V> List<V> subList(List<V> original, int from) {
        final List<V> newList = new ArrayList<>(original.size() - from);
        for (int i = from; i < original.size(); i++) {
            newList.add(original.get(i));
        }
        return newList;
    }

    long read(int bits) {
        final long value = bitstream.read(offset, bits);
        offset += bits;
        return value;
    }

    private long read(Primitive primitive) {
        if (primitive.isFixed()) {
            return read(primitive.getBits());
        } else {
            return readVBR(primitive.getBits());
        }
    }

    private long readChar() {
        final long value = read(Primitive.CHAR6);
        return CHAR6.charAt((int) value);
    }

    private long readVBR(long width) {
        final long value = bitstream.readVBR(offset, width);
        offset += BitStream.widthVBR(value, width);
        return value;
    }

    private void scanNext() {
        final int id = (int) read(idSize);

        switch (id) {
            case BuiltinIDs.END_BLOCK:
                exitBlock();
                break;

            case BuiltinIDs.ENTER_SUBBLOCK:
                enterSubBlock();
                break;

            case BuiltinIDs.DEFINE_ABBREV:
                defineAbbreviation();
                break;

            case BuiltinIDs.UNABBREV_RECORD:
                unabbreviatedRecord();
                break;

            default:
                // custom defined abbreviation
                abbreviatedRecord(id);
                break;
        }
    }

    private void abbreviatedRecord(int recordId) {
        abbreviationDefinitions.get(recordId - BuiltinIDs.CUSTOM_ABBREV_OFFSET).forEach(AbbreviatedRecord::scan);
        passRecordToParser();
    }

    private void alignInt() {
        long mask = Integer.SIZE - 1;
        if ((offset & mask) != 0) {
            offset = (offset & ~mask) + Integer.SIZE;
        }
    }

    private void defineAbbreviation() {
        final long operandCount = read(Primitive.ABBREVIATED_RECORD_OPERANDS);

        final List<AbbreviatedRecord> operandScanners = new ArrayList<>((int) operandCount);

        int i = 0;
        boolean containsArrayOperand = false;
        while (i < operandCount) {
            // first operand contains the record id

            final boolean isLiteral = read(Primitive.USER_OPERAND_LITERALBIT) == 1;
            if (isLiteral) {
                final long fixedValue = read(Primitive.USER_OPERAND_LITERAL);
                operandScanners.add(() -> recordBuffer.addOp(fixedValue));

            } else {

                final long recordType = read(Primitive.USER_OPERAND_TYPE);

                switch ((int) recordType) {
                    case AbbrevRecordId.FIXED: {
                        final int width = (int) read(Primitive.USER_OPERAND_DATA);
                        operandScanners.add(() -> {
                            final long op = read(width);
                            recordBuffer.addOp(op);
                        });
                        break;
                    }

                    case AbbrevRecordId.VBR: {
                        final int width = (int) read(Primitive.USER_OPERAND_DATA);
                        operandScanners.add(() -> {
                            final long op = readVBR(width);
                            recordBuffer.addOp(op);
                        });
                        break;
                    }

                    case AbbrevRecordId.ARRAY:
                        // arrays only occur as the second to last operand in an abbreviation, just
                        // before their element type
                        // then this can only be executed once for any abbreviation
                        containsArrayOperand = true;
                        break;

                    case AbbrevRecordId.CHAR6:
                        operandScanners.add(() -> {
                            final long op = readChar();
                            recordBuffer.addOp(op);
                        });
                        break;

                    case AbbrevRecordId.BLOB:
                        operandScanners.add(() -> {
                            long blobLength = read(Primitive.USER_OPERAND_BLOB_LENGTH);
                            alignInt();
                            final long maxBlobPartLength = Long.SIZE / Primitive.USER_OPERAND_LITERAL.getBits();
                            recordBuffer.ensureFits(blobLength / maxBlobPartLength);
                            while (blobLength > 0) {
                                final long l = blobLength <= maxBlobPartLength ? blobLength : maxBlobPartLength;
                                final long blobValue = read((int) (Primitive.USER_OPERAND_LITERAL.getBits() * l));
                                recordBuffer.addOp(blobValue);
                                blobLength -= l;
                            }
                            alignInt();
                        });
                        break;

                    default:
                        throw new IllegalStateException("Unexpected Record Type Id: " + recordType);
                }

            }

            i++;
        }

        if (containsArrayOperand) {
            final AbbreviatedRecord elementScanner = operandScanners.get(operandScanners.size() - 1);
            final AbbreviatedRecord arrayScanner = () -> {
                final long arrayLength = read(Primitive.USER_OPERAND_ARRAY_LENGTH);
                recordBuffer.ensureFits(arrayLength);
                for (int j = 0; j < arrayLength; j++) {
                    elementScanner.scan();
                }
            };
            operandScanners.set(operandScanners.size() - 1, arrayScanner);
        }

        abbreviationDefinitions.add(operandScanners);
    }

    private void enterSubBlock() {
        final long blockId = read(Primitive.SUBBLOCK_ID);
        final long newIdSize = read(Primitive.SUBBLOCK_ID_SIZE);
        alignInt();
        final long numWords = read(Integer.SIZE);

        final Block subBlock = Block.lookup(blockId);
        if (subBlock == null) {
            offset += numWords * Integer.SIZE;

        } else {
            final int localAbbreviationDefinitionsOffset = defaultAbbreviations.getOrDefault(block, Collections.emptyList()).size();
            parents.push(new ScannerState(subList(abbreviationDefinitions, localAbbreviationDefinitionsOffset), block, idSize, parser));
            abbreviationDefinitions.clear();
            abbreviationDefinitions.addAll(defaultAbbreviations.getOrDefault(subBlock, Collections.emptyList()));
            block = subBlock;
            idSize = (int) newIdSize;
            parser = parser.enter(subBlock);

            if (block == Block.BLOCKINFO) {
                final ParserListener parentListener = parser;
                parser = new ParserListener() {

                    int currentBlockId = -1;

                    @Override
                    public ParserListener enter(Block newBlock) {
                        return parentListener.enter(newBlock);
                    }

                    @Override
                    public void exit() {
                        setDefaultAbbreviations();
                        parentListener.exit();
                    }

                    @Override
                    public void record(long id, long[] args) {
                        if (id == 1) {
                            // SETBID tells us which blocks is currently being described
                            // we simply ignore SETRECORDNAME since we do not need it
                            setDefaultAbbreviations();
                            currentBlockId = (int) args[0];
                        }
                        parentListener.record(id, args);
                    }

                    private void setDefaultAbbreviations() {
                        if (currentBlockId >= 0) {
                            final Block currentBlock = Block.lookup(currentBlockId);
                            defaultAbbreviations.putIfAbsent(currentBlock, new ArrayList<>());
                            defaultAbbreviations.get(currentBlock).addAll(abbreviationDefinitions);
                            abbreviationDefinitions.clear();
                        }
                    }
                };
            }

        }
    }

    private void exitBlock() {
        alignInt();
        parser.exit();

        final ScannerState parentState = parents.pop();
        block = parentState.getBlock();

        abbreviationDefinitions.clear();
        abbreviationDefinitions.addAll(defaultAbbreviations.getOrDefault(block, Collections.emptyList()));
        abbreviationDefinitions.addAll(parentState.getAbbreviatedRecords());

        idSize = parentState.getIdSize();
        parser = parentState.getParser();
    }

    private void passRecordToParser() {
        parser.record(recordBuffer.getId(), recordBuffer.getOps());
        recordBuffer.invalidate();
    }

    private void unabbreviatedRecord() {
        final long recordId = read(Primitive.UNABBREVIATED_RECORD_ID);
        recordBuffer.addOp(recordId);

        final long opCount = read(Primitive.UNABBREVIATED_RECORD_OPS);
        recordBuffer.ensureFits(opCount);

        long op;
        for (int i = 0; i < opCount; i++) {
            op = read(Primitive.UNABBREVIATED_RECORD_OPERAND);
            recordBuffer.addOpNoCheck(op);
        }
        passRecordToParser();
    }
}
