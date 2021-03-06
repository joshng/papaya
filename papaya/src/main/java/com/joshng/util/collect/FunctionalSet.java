package com.joshng.util.collect;

import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 4:26:26 PM
 */
public class FunctionalSet<T> extends FunctionalIterable<T> implements FunSet<T> {
  public FunctionalSet(ImmutableSet<T> delegate) {
    super(delegate);
  }

  @Override
  public ImmutableSet<T> delegate() {
    return (ImmutableSet<T>) super.delegate();
  }

  public static <T> FunSet<T> copyOf(Iterable<T> delegate) {
    if (delegate instanceof FunSet) return (FunSet<T>) delegate;
    return extendSet(ImmutableSet.copyOf(delegate));
  }

  public static <T> FunSet<T> extendSet(ImmutableSet<T> delegate) {
    if (delegate.isEmpty()) return Functional.emptySet();
    return new FunctionalSet<>(delegate);
  }

  static final EmptySet EMPTY = new EmptySet();

  @SuppressWarnings({"unchecked"})
  private static class EmptySet extends EmptyCollection implements FunSet {
    public ImmutableSet delegate() {
      return ImmutableSet.of();
    }

    @Override
    public EmptySet foreach(Consumer visitor) {
      return this;
    }

    @Override
    public FunSet cast() {
      return this;
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(@Nullable Object object) {
      return object == this || (object instanceof Set && ((Set) object).isEmpty());
    }

    @Override
    public int hashCode() {
      return delegate().hashCode();
    }
  }
}
