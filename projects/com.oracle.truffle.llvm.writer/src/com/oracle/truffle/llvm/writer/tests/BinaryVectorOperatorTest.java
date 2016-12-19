package com.oracle.truffle.llvm.writer.tests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.*;

import com.oracle.truffle.llvm.parser.api.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.api.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.api.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.api.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.api.model.types.IntegerType;
import com.oracle.truffle.llvm.parser.api.model.types.VectorType;
import com.oracle.truffle.llvm.parser.bc.util.writer.ModelToIRVisitor;
import com.oracle.truffle.llvm.writer.facades.InstructionGeneratorFacade;

@SuppressWarnings("unused")
@RunWith(Parameterized.class)
public class BinaryVectorOperatorTest {

    private final IntegerType type;
    private final BinaryOperator operator;

    public BinaryVectorOperatorTest(IntegerType type, BinaryOperator operator) {
        this.type = type;
        this.operator = operator;
    }

    @Parameters(name = "{index}: BinaryVectorOperator[type={0} ,operator={1}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (IntegerType type : IntegerType.values()) {
            for (BinaryOperator operator : BinaryOperator.values()) {
                if (operator.isFloatingPoint()) {
                    continue;
                }
                parameters.add(new Object[]{type, operator});
            }
        }

        return parameters;
    }

    private static final long VECTOR1_1 = 111191111L; // prim
    private static final long VECTOR1_2 = 792606555396976L; // even

    private static final long VECTOR2_1 = 200560490131L; // prim
    private static final long VECTOR2_2 = 1442968193L; // prim

    @Test
    public void test() {
        long max_value = type.getBits() < 64 ? 1L << type.getBits() : Long.MAX_VALUE;

        long vector1_1 = VECTOR1_1 % max_value;
        long vector1_2 = VECTOR1_2 % max_value;
        long vector2_1 = VECTOR2_1 % max_value;
        long vector2_2 = VECTOR2_2 % max_value;

        long result1 = 0, result2 = 0;

        // TODO: signed/unsigned?
        switch (operator) {
            case INT_ADD:
                result1 = (vector1_1 + vector2_1) % max_value;
                result2 = (vector1_2 + vector2_2) % max_value;
                break;
            case INT_SUBTRACT:
                result1 = (vector1_1 - vector2_1) % max_value;
                result2 = (vector1_2 - vector2_2) % max_value;
                break;
            case INT_MULTIPLY:
                result1 = (vector1_1 * vector2_1) % max_value;
                result2 = (vector1_2 * vector2_2) % max_value;
                break;
            case INT_UNSIGNED_DIVIDE:
                result1 = (vector1_1 / vector2_1) % max_value;
                result2 = (vector1_2 / vector2_2) % max_value;
                break;
            case INT_SIGNED_DIVIDE:
                result1 = (vector1_1 / vector2_1) % max_value;
                result2 = (vector1_2 / vector2_2) % max_value;
                break;
            case INT_UNSIGNED_REMAINDER:
                result1 = (vector1_1 % vector2_1) % max_value;
                result2 = (vector1_2 % vector2_2) % max_value;
                break;
            case INT_SIGNED_REMAINDER:
                result1 = (vector1_1 % vector2_1) % max_value;
                result2 = (vector1_2 % vector2_2) % max_value;
                break;
            case INT_SHIFT_LEFT:
                result1 = (vector1_1 << vector2_1) % max_value;
                result2 = (vector1_2 << vector2_2) % max_value;
                break;
            case INT_LOGICAL_SHIFT_RIGHT:
                result1 = (vector1_1 >> vector2_1) % max_value;
                result2 = (vector1_2 >> vector2_2) % max_value;
                break;
            case INT_ARITHMETIC_SHIFT_RIGHT:
                result1 = (vector1_1 >>> vector2_1) % max_value;
                result2 = (vector1_2 >>> vector2_2) % max_value;
                break;
            case INT_AND:
                result1 = (vector1_1 & vector2_1) % max_value;
                result2 = (vector1_2 & vector2_2) % max_value;
                break;
            case INT_OR:
                result1 = (vector1_1 | vector2_1) % max_value;
                result2 = (vector1_2 | vector2_2) % max_value;
                break;
            case INT_XOR:
                result1 = (vector1_1 ^ vector2_1) % max_value;
                result2 = (vector1_2 ^ vector2_2) % max_value;
                break;
            default:
                fail("unexpected operator");
                break;
        }

        // Checkstyle: stop magic number name check
        InstructionGeneratorFacade facade = new InstructionGeneratorFacade("main", 1, IntegerType.BOOLEAN, false);

        Instruction vec1 = facade.createAllocate(new VectorType(type, 2)); // TODO: wrong align
        Instruction vec2 = facade.createAllocate(new VectorType(type, 2));
        Instruction vec_res = facade.createAllocate(new VectorType(type, 2));

        vec1 = facade.createLoad(vec1);
        vec1 = facade.createInsertElement(vec1, new IntegerConstant(type, vector1_1), 0);
        vec1 = facade.createInsertElement(vec1, new IntegerConstant(type, vector1_2), 1);

        vec2 = facade.createLoad(vec2);
        vec2 = facade.createInsertElement(vec2, new IntegerConstant(type, vector2_1), 0);
        vec2 = facade.createInsertElement(vec2, new IntegerConstant(type, vector2_2), 1);

        Instruction retVec = facade.createBinaryOperation(vec1, vec2, operator);

        // // TODO: this version doesn't work yet, so we evaluate the result in another way
        // vec_res = facade.createLoad(vec_res);
        // vec_res = facade.createInsertElement(vec_res, new IntegerConstant(type, result1), 0);
        // vec_res = facade.createInsertElement(vec_res, new IntegerConstant(type, result2), 1);
        //
        // retVec = facade.createCompare(CompareOperator.INT_NOT_EQUAL, vec1, vec2);
        //
        // Instruction retVec1 = facade.createExtractElement(retVec, 0);
        // Instruction retVec2 = facade.createExtractElement(retVec, 1);

        Instruction retVec1 = facade.createExtractElement(retVec, 0);
        Instruction retVec2 = facade.createExtractElement(retVec, 1);

        retVec1 = facade.createCompare(CompareOperator.INT_NOT_EQUAL, retVec1, new IntegerConstant(type, result1));
        retVec2 = facade.createCompare(CompareOperator.INT_NOT_EQUAL, retVec2, new IntegerConstant(type, result2));

        Instruction ret = facade.createBinaryOperation(retVec1, retVec2, BinaryOperator.INT_OR);
        facade.createReturn(ret); // 0=OK, 1=ERROR
        // Checkstyle: resume magic number name check

        System.out.println(ModelToIRVisitor.getIRString(facade.getModel()));
    }
}
