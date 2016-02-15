package com.joshng.util.collect;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class FunctionalIterable<T> implements FunIterable<T> {
  private final Iterable<T> delegate;

  public FunctionalIterable(Iterable<T> delegate) {
    this.delegate = checkNotNull(delegate, "iterable");
  }

  @Override
  public Iterable<T> delegate() {
    return delegate;
  }

  @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
  @Override
  public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }
}
