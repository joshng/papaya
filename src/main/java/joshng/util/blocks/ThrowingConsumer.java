package joshng.util.blocks;

/**
 * User: josh
 * Date: Jul 15, 2010
 * Time: 11:33:40 AM
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {
  void accept(T value) throws Exception;

  default ThrowingRunnable bind(T value) {
    return () -> accept(value);
  }
}
