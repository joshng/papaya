package com.joshng.util.collect;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import static com.joshng.util.reflect.Reflect.blindCast;

/**
 * User: josh
 * Date: 10/3/11
 * Time: 4:04 PM
 */
public interface FunSet<T> extends Set<T>, FunCollection<T> {
  @Override
  default FunSet<T> foreach(Consumer<? super T> visitor) {
    FunCollection.super.foreach(visitor);
    return this;
  }

  @Override
  ImmutableSet<T> delegate();

  @Override
  default FunList<T> toList() {
    return new FunctionalList<>(delegate().asList());
  }

  default FunSet<T> toSet() {
    return this;
  }

  @Override
  default <U> FunSet<U> cast() {
    return blindCast(this);
  }

  @Override
  default Object[] toArray() {
    return FunCollection.super.toArray();
  }

  @Override
  default <T1> T1[] toArray(T1[] a) {
    return FunCollection.super.toArray(a);
  }

  @Override
  default boolean contains(Object o) {
    return FunCollection.super.contains(o);
  }

  @Override
  default boolean containsAll(Collection<?> collection) {
    return FunCollection.super.containsAll(collection);
  }

  @Override
  default boolean isEmpty() {
    return FunCollection.super.isEmpty();
  }

  @Override
  default int size() {
    return FunCollection.super.size();
  }

  @Override
  default Iterator<T> iterator() {
    return FunCollection.super.iterator();
  }

  @Override
  default boolean add(T o) {
    throw FunCollection.rejectMutation();
  }

  @Override
  default boolean remove(Object o) {
    throw FunCollection.rejectMutation();
  }

  @Override
  default void clear() {
    throw FunCollection.rejectMutation();
  }

  @Override
  default boolean retainAll(Collection<?> collection) {
    throw FunCollection.rejectMutation();
  }

  @Override
  default boolean removeAll(Collection<?> collection) {
    throw FunCollection.rejectMutation();
  }

  @Override
  default boolean addAll(Collection<? extends T> collection) {
    throw FunCollection.rejectMutation();
  }
}
