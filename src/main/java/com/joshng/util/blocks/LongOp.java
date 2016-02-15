package com.joshng.util.blocks;

import java.util.function.LongBinaryOperator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 1/15/13
 * Time: 9:40 AM
 */
public interface LongOp extends LongBinaryOperator, F2<Long, Long, Long> {
  default Long apply(Long input1, Long input2) {
    return applyAsLong(input1, input2);
  }

  default F<Long, Long> by(long rhs) {
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

  public static final LongOp Add = new LongOp() {
    @Override
    public long applyAsLong(long input1, long input2) {
      return input1 + input2;
    }

    @Override
    public String toString() {
      return "Add";
    }
  };
  public static final LongOp Subtract = new LongOp() {
    @Override
    public long applyAsLong(long input1, long input2) {
      return input1 - input2;
    }

    @Override
    public String toString() {
      return "Subtract";
    }
  };
  public static final LongOp Multiply = new LongOp() {
    @Override
    public long applyAsLong(long input1, long input2) {
      return input1 * input2;
    }

    @Override
    public String toString() {
      return "Multiply";
    }
  };
  public static final LongOp Divide = new LongOp() {
    @Override
    public F<Long, Long> bindSecond(Long rhs) {
      checkArgument(rhs != 0, "Tried to specify a zero denominator");
      return LongOp.super.by(rhs);
    }

    @Override
    public long applyAsLong(long input1, long input2) {
      return input1 / input2;
    }

    @Override
    public String toString() {
      return "Divide";
    }
  };
  public static final LongOp Modulo = new LongOp() {
    @Override
    public F<Long, Long> bindSecond(Long rhs) {
      checkArgument(rhs > 0, "Tried to specify a non-positive denominator");
      return LongOp.super.bindSecond(rhs);
    }

    @Override
    public long applyAsLong(long input1, long input2) {
      return input1 % input2;
    }

    @Override
    public String toString() {
      return "Modulo";
    }
  };
}
