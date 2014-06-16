package joshng.util.collect;

import java.util.Iterator;

/**
 * User: josh
 * Date: 1/13/12
 * Time: 11:58 AM
 */
public class ConstantIterable<T> implements AbstractFunIterable<T>, Iterator<T> {
  private final T value;

  public static <T> ConstantIterable<T> of(T value) {
    return new ConstantIterable<>(value);
  }

  public ConstantIterable(T value) {
    this.value = value;
  }

  public Iterator<T> iterator() {
    return this;
  }

  public boolean hasNext() {
    return true;
  }

  public T next() {
    return value;
  }
}
