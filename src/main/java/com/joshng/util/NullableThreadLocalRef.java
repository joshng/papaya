package com.joshng.util;

/**
 * User: josh
 * Date: May 22, 2011
 * Time: 10:54:16 AM
 */
public class NullableThreadLocalRef<T> extends ThreadLocalRef<T> {
  @Override
  protected final T initialValue() {
    return null;
  }

  public boolean isSet() {
    return get() != null;
  }
}
