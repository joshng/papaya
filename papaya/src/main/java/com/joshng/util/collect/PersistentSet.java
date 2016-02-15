package com.joshng.util.collect;


import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.joshng.util.blocks.F2;

import javax.annotation.concurrent.Immutable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A persistent immutable single linked set class.
 *
 * NOTE: this is not an efficient structure for large sets: insertion is O(N)
 */
@Immutable
public class PersistentSet<T> extends AbstractSequentialList<T> implements Set<T> {
  private static final F2 WITH = new F2<Object, PersistentSet<Object>, PersistentSet<Object>>() {
    @Override
    public PersistentSet<Object> apply(Object input1, PersistentSet<Object> input2) {
      return input2.with(input1);
    }
  };

  private final T value;
  // logically final, but Builder uses mutable private copies
  private PersistentSet<T> next;

  @SuppressWarnings("unchecked")
  public static <T> F2<T, PersistentSet<T>, PersistentSet<T>> with() {
    return WITH;
  }

  private PersistentSet(T value, PersistentSet<T> next) {
    this.value = value;
    this.next = next;
  }

  @SuppressWarnings({"rawtypes"})
  private final static PersistentSet Nil = makeNil();

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static PersistentSet makeNil() {
    PersistentSet nil = new PersistentSet(null, null);
    nil.next = nil;
    return nil;
  }

  /**
   * @return The empty set
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentSet<T> nil() {
    return Nil;
  }

  /**
   * @return A set containing the specified value
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentSet<T> of(T value) {
    return new PersistentSet<T>(value, Nil);
  }

  /**
   * @return A set containing the specified values
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentSet<T> of(T v1, T v2) {
    return Nil.with(v2).with(v1);
  }

  /**
   * @return A set containing the specified values
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentSet<T> of(T v1, T v2, T v3) {
    return Nil.with(v3).with(v2).with(v1);
  }

  /**
   * @return A set containing the specified values
   */
  @SuppressWarnings("unchecked")
  public static <T> PersistentSet<T> of(T... values) {
    PersistentSet<T> set = Nil;
    for (int i = values.length - 1; i >= 0; --i)
      set = set.with(values[i]);
    return set;
  }

  /**
   * @return A new set with the values from the Iterable
   */
  public static <T> PersistentSet<T> copyOf(Iterable<T> values) {
    if (values instanceof PersistentSet) return (PersistentSet<T>) values;
    return new Builder<T>().addAll(values).build();
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  /**
   * @return The value at the start of the set
   */
  public T head() {
    return value;
  }

  /**
   * @return The remainder of the set without the first element
   */
  public PersistentSet<T> tail() {
    return next;
  }

  /**
   * @return A new set with value as the head and the old set as the tail
   */
  public PersistentSet<T> with(T value) {
    checkNotNull(value);
    return contains(value) ? this : new PersistentSet<>(value, this);
  }

  @Override
  public T get(int i) {
    if (i >= 0)
      for (PersistentSet<T> set = this; set != Nil; set = set.tail())
        if (i-- == 0)
          return set.head();
    throw new IndexOutOfBoundsException();
  }

  /**
   * Note: O(N)
   *
   * @return A new set omitting the specified value
   */
  public PersistentSet<T> without(T x) {
    if (x == null) return this;
    PersistentSet<T> match = this;
    while (!x.equals(match.head())) {
      if (match.isEmpty()) return this;
      match = match.tail();
    }

    Builder<T> prefix = new Builder<>();
    PersistentSet<T> p = this;
    while (p != match) {
      prefix.add(p.head());
    }
    return prefix.buildOnto(match.tail());
  }

  public PersistentSet<T> union(Iterable<? extends T> items) {
    return PersistentSet.<T>builder().addAll(items).buildOnto(this);
  }

  /**
   * Note: O(N)
   */
  @Override
  public boolean contains(Object value) {
    for (PersistentSet<T> set = this; set != Nil; set = set.next)
      if (value.equals(set.head()))
        return true;
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object other) {
    if (this == other)
      return true;
    if (!(other instanceof PersistentSet))
      return false;
    PersistentSet<?> x = this;
    PersistentSet<Object> y = (PersistentSet<Object>) other;
    for (; x != Nil && y != Nil; x = x.tail(), y = y.tail())
      if (!x.head().equals(y.head()))
        return false;
    return x == Nil && y == Nil;
  }

  public static final Joiner commaJoiner = Joiner.on(",");

  @Override
  public String toString() {
    return "(" + commaJoiner.join(this) + ")";
  }

  /**
   * Note: O(N)
   *
   * @return The number of elements in the set
   */
  @Override
  public int size() {
    int size = 0;
    for (PersistentSet<T> set = this; set != Nil; set = set.next)
      ++size;
    return size;
  }

  @Override
  public boolean isEmpty() {
    return this == Nil;
  }

  /**
   * @return A new set with the elements in the reverse order
   */
  public PersistentSet<T> reversed() {
    PersistentSet<T> set = nil();
    for (PersistentSet<T> p = this; p != Nil; p = p.next) {
      set = new PersistentSet<>(p.value, set);
    }
    return set;
  }

  public static class Builder<T> {

    private PersistentSet<T> set = nil();

    public Builder<T> add(T value) {
      set = set.with(value);
      return this;
    }

    public Builder<T> addAll(Iterable<? extends T> values) {
      for (T value : values)
        add(value);
      return this;
    }

    /**
     * The Builder cannot be used after calling build()
     *
     * @return The set
     */
    @SuppressWarnings("unchecked")
    public PersistentSet<T> build() {
      return buildOnto(nil());
    }

    public PersistentSet<T> buildOnto(PersistentSet<T> tail) {
      // reverse in place by changing pointers (no allocation)
      Set<T> contents  = Sets.newHashSet(tail);
      for (PersistentSet<T> p = set; p != Nil; ) {
        PersistentSet<T> next = p.next;
        if (!contents.contains(p.value)) {
          p.next = tail;
          tail = p;
        }
        p = next;
      }
      set = null;
      return tail;
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new Iter<T>(this);
  }

  private static class Iter<T> implements Iterator<T> {

    private PersistentSet<T> set;

    private Iter(PersistentSet<T> set) {
      this.set = new PersistentSet<T>(null, set);
    }

    public boolean hasNext() {
      return set.tail() != Nil;
    }

    public T next() {
      set = set.tail();
      return set.head();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override public Spliterator<T> spliterator() {
    return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.DISTINCT);
  }
}
