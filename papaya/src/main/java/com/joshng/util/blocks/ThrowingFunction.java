package com.joshng.util.blocks;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * User: josh
 * Date: 12/26/11
 * Time: 2:32 PM
 */
@FunctionalInterface
public interface ThrowingFunction<I, O> {
  static <I, O> ThrowingFunction<I, O> throwingFunction(ThrowingFunction<I, O> block) { return block; }

  O apply(I input) throws Exception;

  static <I,O> F<I, O> unchecked(ThrowingFunction<I,O> throwingFunction) {
    return input -> {
      try {
        return throwingFunction.apply(input);
      } catch (Exception e) {
        Throwables.propagateIfPossible(e);
        throw new UncheckedExecutionException(e);
      }
    };
  }

  default F<I, O> unchecked() {
    return ThrowingFunction.unchecked(this);
  }
}
