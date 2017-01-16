package com.oracle.truffle.llvm.writer.util;

import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.writer.LLVMPrintVersion;

public class IRWriterUtil {

    private final LLVMPrintVersion.LLVMPrintVisitors printVisitors;

    public IRWriterUtil(LLVMPrintVersion.LLVMPrintVisitors printVisitors) {
        this.printVisitors = printVisitors;
    }

    public void printSymbol(Symbol symbol) {
        if (symbol instanceof Constant) {
            ((Constant) symbol).accept(printVisitors.getConstantVisitor());
        } else {
            printVisitors.print(symbol); // TODO: put warning
        }
    }

    public void printSymbolName(Symbol symbol) {
        if (symbol instanceof ValueSymbol) {
            printVisitors.print(((ValueSymbol) symbol).getName());
        } else {
            printSymbol(symbol); // TODO
        }
    }

    public void printConstantValue(Constant symbol) {
        symbol.accept(printVisitors.getConstantVisitor().getStringRepresentationVisitor());
    }
}
