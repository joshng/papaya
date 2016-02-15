package com.joshng.util.concurrent;

import com.joshng.util.collect.MutableReference;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: josh
 * Date: 2/14/13
 * Time: 6:11 PM
 */
public class AtomicMutableReference<T> extends AtomicReference<T> implements MutableReference<T>, Callable<T> {
  public AtomicMutableReference(T initialValue) {
    super(initialValue);
  }

  public AtomicMutableReference() {
  }

  public static <T> AtomicMutableReference<T> newReference(T initialValue) {
    return new AtomicMutableReference<>(initialValue);
  }

  public static <T> AtomicMutableReference<T> newNullReference() {
    return new AtomicMutableReference<>();
  }

  @Override
  public T call() {
    return get();
  }
}
