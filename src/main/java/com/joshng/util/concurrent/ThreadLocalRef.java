package com.joshng.util.concurrent;

import com.joshng.util.collect.MutableReference;

/**
 * User: josh
 * Date: 2/4/12
 * Time: 9:36 AM
 */
public class ThreadLocalRef<T> extends ThreadLocal<T> implements MutableReference<T> {
  private final T initialValue;

  public ThreadLocalRef(T initialValue) {
    this.initialValue = initialValue;
  }

  public ThreadLocalRef() {
    this(null);
  }

  @Override
  protected T initialValue() {
    return initialValue;
  }
}
