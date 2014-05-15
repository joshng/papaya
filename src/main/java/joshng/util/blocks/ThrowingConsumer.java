package joshng.util.blocks;

/**
 * User: josh
 * Date: Jul 15, 2010
 * Time: 11:33:40 AM
 */
public interface ThrowingConsumer<T> {
  void accept(T value) throws Exception;
}
