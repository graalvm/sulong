package com.oracle.truffle.llvm.parser.util;

import uk.ac.man.cs.llvm.ir.types.Type;

public interface LLVMBitcodeTypeHelper {

    int getPadding(int offset, int alignment);

    int getPadding(int offset, Type type);

    int getByteSize(Type type);

    int getAlignment(Type type);

    int goIntoTypeGetLength(Type type, int index);

}