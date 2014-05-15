package joshng.util;

import joshng.util.blocks.F;
import joshng.util.blocks.F2;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 1/15/13
 * Time: 9:40 AM
 */
public abstract class IntFunction implements F2<Integer, Integer, Integer> {
  public F<Integer, Integer> by(int rhs) {
    return bindSecond(rhs);
  }

  public static F<Integer, Integer> add(int addend) {
    return Add.by(addend);
  }

  public static F<Integer, Integer> subtract(int addend) {
    return Subtract.by(addend);
  }

  public static F<Integer, Integer> multiply(int factor) {
    return Multiply.by(factor);
  }

  public static F<Integer, Integer> divide(int denom) {
    return Divide.by(denom);
  }

  public static F<Integer, Integer> modulo(int modulus) {
    return Modulo.by(modulus);
  }

  public static final IntFunction Add = new IntFunction() {
    @Override
    public Integer apply(Integer input1, Integer input2) {
      return input1 + input2;
    }

    @Override
    public String toString() {
      return "Add";
    }
  };
  public static final IntFunction Subtract = new IntFunction() {
    @Override
    public Integer apply(Integer input1, Integer input2) {
      return input1 - input2;
    }

    @Override
    public String toString() {
      return "Subtract";
    }
  };
  public static final IntFunction Multiply = new IntFunction() {
    @Override
    public Integer apply(Integer input1, Integer input2) {
      return input1 * input2;
    }

    @Override
    public String toString() {
      return "Multiply";
    }
  };
  public static final IntFunction Divide = new IntFunction() {
    @Override
    public F<Integer, Integer> bindSecond(Integer rhs) {
      checkArgument(rhs != 0, "Tried to specify a zero denominator");
      return super.by(rhs);
    }

    @Override
    public Integer apply(Integer input1, Integer input2) {
      return input1 / input2;
    }

    @Override
    public String toString() {
      return "Divide";
    }
  };
  public static final IntFunction Modulo = new IntFunction() {
    @Override
    public F<Integer, Integer> bindSecond(Integer rhs) {
      checkArgument(rhs > 0, "Tried to specify a non-positive denominator");
      return super.bindSecond(rhs);
    }

    @Override
    public Integer apply(Integer input1, Integer input2) {
      return input1 % input2;
    }

    @Override
    public String toString() {
      return "Modulo";
    }
  };
}
