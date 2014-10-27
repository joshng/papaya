package joshng.util.blocks;

import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: 10/23/14
 * Time: 5:52 PM
 */
/** @deprecated just use a {@link Callable} instead */
@Deprecated
public interface ThrowingSource<T> extends Callable<T> {
  T get() throws Exception;

  default T call() throws Exception {
    return get();
  }
}
