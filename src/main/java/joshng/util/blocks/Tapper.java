package joshng.util.blocks;

/**
 * User: josh
 * Date: 6/22/12
 * Time: 2:51 PM
 */

/**
 * This is the "K combinator": a type of function which applies side-effects to its input, then
 * returns the input itself.
 * This can be convenient when applying side-effects or validations to a value prior to returning it.
 */
public interface Tapper<T> extends F<T, T> {
  void tap(T value);

  @Override

  default T apply(T input) {
    tap(input);
    return input;
  }
}
