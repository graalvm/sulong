package com.oracle.truffle.llvm.writer.util;

import java.io.OutputStream;
import java.io.PrintWriter;

import com.oracle.truffle.llvm.parser.api.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;
import com.oracle.truffle.llvm.writer.ConstantPrintVisitor;

public class IRWriterUtil {

    private final PrintWriter out;

    private final ConstantPrintVisitor constantVisitor;

    public IRWriterUtil(PrintWriter out) {
        this.out = out;
        this.constantVisitor = new ConstantPrintVisitor(out);
    }

    public IRWriterUtil(OutputStream out) {
        this(new PrintWriter(out));
    }

    public void printSymbol(Symbol symbol) {
        if (symbol instanceof Constant) {
            ((Constant) symbol).accept(constantVisitor);
        } else {
            out.print(symbol); // TODO: put warning
        }
    }

    public void printSymbolName(Symbol symbol) {
        if (symbol instanceof ValueSymbol) {
            out.print(((ValueSymbol) symbol).getName());
        } else {
            printSymbol(symbol); // TODO
        }
    }

    public void printConstantValue(Constant symbol) {
        symbol.accept(constantVisitor.getStringRepresentationVisitor());
    }
}
