package com.joshng.util.blocks;

/**
 * User: josh
 * Date: 6/22/12
 * Time: 2:51 PM
 */

import java.util.function.Consumer;

/**
 * This is the "K combinator": a type of function which applies side-effects to its input, then
 * returns the input itself.
 * This can be convenient when applying side-effects or validations to a value prior to returning it.
 */
public interface Tapper<T> extends F<T, T> {
  public static <T> T tap(T value, Consumer<? super T> sideEffect) {
    sideEffect.accept(value);
    return value;
  }

  public static <T> Tapper<T> tapper(Tapper<T> tapper) {
    return tapper;
  }

  public static <T> Tapper<T> extendConsumer(Consumer<? super T> consumer) {
    return tapper(consumer::accept);
  }

  void tap(T value);

  @Override

  default T apply(T input) {
    tap(input);
    return input;
  }
}
