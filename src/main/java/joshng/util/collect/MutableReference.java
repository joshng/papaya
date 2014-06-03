package joshng.util.collect;

import java.util.function.Supplier;

/**
 * User: josh
 * Date: 2/22/13
 * Time: 1:53 PM
 */
public interface MutableReference<T> extends Supplier<T> {
  void set(T value);

  boolean compareAndSet(T expect, T update);

  T getAndSet(T value);

  default Maybe<T> getMaybe() {
    return Maybe.of(get());
  }
}
