package com.joshng.util.collect;

import com.google.common.collect.ObjectArrays;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * User: josh
 * Date: 12/29/11
 * Time: 3:13 PM
 */
public interface FunCollection<T> extends FunIterable<T>, Collection<T> {
  @Override
  default Iterator<T> iterator() {
    return FunIterable.super.iterator();
  }

  @Override
  Collection<T> delegate();

  @Override
  default Stream<T> stream() {
    return delegate().stream();
  }

  default FunCollection<T> foreach(Consumer<? super T> visitor) {
    FunIterable.super.foreach(visitor);
    return this;
  }

  default int size() {
    return delegate().size();
  }


  default boolean removeAll(Collection<?> collection) {
    throw rejectMutation();
  }

  default boolean isEmpty() {
    return delegate().isEmpty();
  }

  default boolean contains(Object object) {
    return delegate().contains(object);
  }

  default Object[] toArray() {
    return delegate().toArray();
  }

  default <T> T[] toArray(T[] array) {
    //noinspection SuspiciousToArrayCall
    return delegate().toArray(array);
  }

  default boolean add(T element) {
    throw rejectMutation();
  }

  default boolean remove(Object object) {
    throw rejectMutation();
  }

  default boolean containsAll(Collection<?> collection) {
    return delegate().containsAll(collection);
  }

  default boolean addAll(Collection<? extends T> collection) {
    throw rejectMutation();
  }

  default boolean retainAll(Collection<?> collection) {
    throw rejectMutation();
  }

  default void clear() {
    throw rejectMutation();
  }

  abstract static class EmptyCollection extends Functional.EmptyIterable implements Collection {
    public abstract Collection delegate();

    @Override
    public Stream stream() {
      return Stream.empty();
    }

    public Object[] toArray() {
      return new Object[]{};
    }

    public Object[] toArray(Object[] a) {
      return ObjectArrays.newArray(a, 0);
    }

    public boolean add(Object o) {
      throw rejectMutation();
    }

    public boolean remove(Object o) {
      throw rejectMutation();
    }

    public boolean containsAll(Collection c) {
      return c.isEmpty();
    }

    public boolean addAll(Collection c) {
      throw rejectMutation();
    }

    public boolean removeAll(Collection c) {
      throw rejectMutation();
    }

    public boolean retainAll(Collection c) {
      throw rejectMutation();
    }

    public void clear() {
      throw rejectMutation();
    }

    public Object remove(int index) {
      throw rejectMutation();
    }

    public boolean contains(Object o) {
      return false;
    }
  }

  static UnsupportedOperationException rejectMutation() {
    return new UnsupportedOperationException("FunctionalCollections are immutable");
  }
}
