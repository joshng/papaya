package joshng.util;

import joshng.util.blocks.F;
import joshng.util.blocks.F2;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 1/15/13
 * Time: 9:40 AM
 */
public abstract class LongFunction implements F2<Long, Long, Long> {
    public F<Long, Long> by(long rhs) {
        return bindSecond(rhs);
    }

    public static F<Long, Long> add(long addend) {
        return Add.by(addend);
    }
    public static F<Long, Long> subtract(long addend) {
        return Subtract.by(addend);
    }
    public static F<Long, Long> multiply(long factor) {
        return Multiply.by(factor);
    }
    public static F<Long, Long> divide(long denom) {
        return Divide.by(denom);
    }
    public static F<Long, Long> modulo(long modulus) {
        return Modulo.by(modulus);
    }

    public static final LongFunction Add = new LongFunction() {
        @Override public Long apply(Long input1, Long input2) { return input1 + input2; }
        @Override public String toString() { return "Add"; }
    };
    public static final LongFunction Subtract = new LongFunction() {
        @Override public Long apply(Long input1, Long input2) { return input1 - input2; }
        @Override public String toString() { return "Subtract"; }
    };
    public static final LongFunction Multiply = new LongFunction() {
        @Override public Long apply(Long input1, Long input2) { return input1 * input2; }
        @Override public String toString() { return "Multiply"; }
    };
    public static final LongFunction Divide = new LongFunction() {
        @Override
        public F<Long, Long> bindSecond(Long rhs) {
            checkArgument(rhs != 0, "Tried to specify a zero denominator");
            return super.by(rhs);
        }

        @Override public Long apply(Long input1, Long input2) { return input1 / input2; }
        @Override public String toString() { return "Divide"; }
    };
    public static final LongFunction Modulo = new LongFunction() {
        @Override
        public F<Long, Long> bindSecond(Long rhs) {
            checkArgument(rhs > 0, "Tried to specify a non-positive denominator");
            return super.bindSecond(rhs);
        }

        @Override public Long apply(Long input1, Long input2) { return input1 % input2; }
        @Override public String toString() { return "Modulo"; }
    };
}
