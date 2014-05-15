package joshng.util.collect;

import com.google.common.collect.Iterables;

import java.util.Iterator;

/**
 * User: josh
 * Date: 1/13/12
 * Time: 11:58 AM
 */
public class InfiniteIterable<T> implements Iterable<T>, Iterator<T> {
  private final T value;

  public static <T> InfiniteIterable<T> of(T value) {
    return new InfiniteIterable<>(value);
  }

  public InfiniteIterable(T value) {
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

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public Iterable<T> limit(int size) {
    return Iterables.limit(this, size);
  }
}
