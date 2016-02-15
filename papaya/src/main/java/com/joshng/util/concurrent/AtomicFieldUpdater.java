package com.joshng.util.concurrent;

import com.joshng.util.collect.MutableReference;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * User: josh
 * Date: 9/24/13
 * Time: 2:26 PM
 */
public class AtomicFieldUpdater<T, V> implements MutableReference<V> {
  private final T obj;
  private final AtomicReferenceFieldUpdater<T, V> updater;

  private AtomicFieldUpdater(AtomicReferenceFieldUpdater<T, V> updater, T obj) {
    this.obj = obj;
    this.updater = updater;
  }

  public static <T, V> AtomicFieldUpdater<T, V> bind(AtomicReferenceFieldUpdater<T, V> updater, T obj) {
    return new AtomicFieldUpdater<>(updater, obj);
  }

  @Override
  public boolean compareAndSet(V expect, V update) {
    return updater.compareAndSet(obj, expect, update);
  }

  @Override
  public void set(V value) {
    updater.set(obj, value);
  }

  @Override
  public V getAndSet(V value) {
    return updater.getAndSet(obj, value);
  }

  @Override
  public V get() {
    return updater.get(obj);
  }
}
