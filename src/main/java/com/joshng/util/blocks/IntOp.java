package com.joshng.util.blocks;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 1/15/13
 * Time: 9:40 AM
 */
public interface IntOp extends IntBinaryOperator, F2<Integer, Integer, Integer> {
  default Integer apply(Integer input1, Integer input2) {
    return applyAsInt(input1, input2);
  }

  default IntUnaryOperator by(int rhs) {
    return new IntUnaryOperator() {
      @Override
      public int applyAsInt(int operand) {
        return IntOp.this.applyAsInt(operand, rhs);
      }

      public String toString() {
        return IntOp.this.toString() + "(?, " + rhs + ")";
      }
    };
  }

  public static IntUnaryOperator add(int addend) {
    return Add.by(addend);
  }

  public static IntUnaryOperator subtract(int addend) {
    return Subtract.by(addend);
  }

  public static IntUnaryOperator multiply(int factor) {
    return Multiply.by(factor);
  }

  public static IntUnaryOperator divide(int denom) {
    return Divide.by(denom);
  }

  public static IntUnaryOperator modulo(int modulus) {
    return Modulo.by(modulus);
  }

  public static final IntOp Add = new IntOp() {
    @Override
    public int applyAsInt(int input1, int input2) {
      return input1 + input2;
    }

    @Override
    public String toString() {
      return "Add";
    }
  };
  public static final IntOp Subtract = new IntOp() {
    @Override
    public int applyAsInt(int input1, int input2) {
      return input1 - input2;
    }

    @Override
    public String toString() {
      return "Subtract";
    }
  };
  public static final IntOp Multiply = new IntOp() {
    @Override
    public int applyAsInt(int input1, int input2) {
      return input1 * input2;
    }

    @Override
    public String toString() {
      return "Multiply";
    }
  };
  public static final IntOp Divide = new IntOp() {
    @Override
    public IntUnaryOperator by(int rhs) {
      checkArgument(rhs != 0, "Tried to specify a zero denominator");
      return IntOp.super.by(rhs);
    }

    @Override
    public int applyAsInt(int input1, int input2) {
      return input1 / input2;
    }

    @Override
    public String toString() {
      return "Divide";
    }
  };
  public static final IntOp Modulo = new IntOp() {
    @Override
    public F<Integer, Integer> bindSecond(Integer rhs) {
      checkArgument(rhs > 0, "Tried to specify a non-positive denominator");
      return IntOp.super.bindSecond(rhs);
    }

    @Override
    public int applyAsInt(int input1, int input2) {
      return input1 % input2;
    }

    @Override
    public String toString() {
      return "Modulo";
    }
  };
}
