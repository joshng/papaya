package joshng.util.blocks;

/**
 * User: josh
 * Date: 10/22/14
 * Time: 9:38 PM
 */
@FunctionalInterface
public interface ThrowingBiConsumer<A,B> {
  void accept(A a, B b) throws Exception;

  default ThrowingConsumer<B> bindFirst(A firstParam) {
    return secondParam -> accept(firstParam, secondParam);
  }

  default ThrowingConsumer<A> bindSecond(B secondParam) {
    return firstParam -> accept(firstParam, secondParam);
  }

  default ThrowingRunnable bind(A first, B second) {
    return () -> accept(first, second);
  }
}
