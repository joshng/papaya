package joshng.util.blocks;

import com.google.common.base.Throwables;

/**
 * User: josh
 * Date: 12/26/11
 * Time: 2:32 PM
 */
public interface ThrowingFunction<I, O> {
  static <I, O> ThrowingFunction<I, O> method(ThrowingFunction<I, O> block) { return block; }

  public O apply(I input) throws Exception;

  public static <I,O> F<I, O> unchecked(ThrowingFunction<I,O> throwingFunction) {
    return input -> {
      try {
        return throwingFunction.apply(input);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    };
  }

  default F<I, O> unchecked() {
    return ThrowingFunction.unchecked(this);
  }
}
