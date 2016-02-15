package com.joshng.util.exceptions;

/**
 * User: josh
 * Date: 2/6/12
 * Time: 8:58 PM
 */

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.joshng.util.blocks.F2;
import com.joshng.util.collect.Maybe;

import java.util.Collections;
import java.util.List;

public interface MultiException {
  public static final F2<Throwable, MultiException, MultiException> COMBINER = new F2<Throwable, MultiException, MultiException>() {
    @Override
    public MultiException apply(Throwable input1, MultiException input2) {
      return input2.with(input1);
    }
  };

  MultiException with(Throwable toAdd);

  List<Throwable> getThrowables();

  Maybe<Throwable> getCombinedThrowable();

  <E extends Throwable> void throwIfException(Class<E> exceptionClass) throws E;

  void throwRuntimeIfAny();

  default MultiException collectExceptions(Iterable<? extends Runnable> runnables) {
    MultiException result = this;
    for (Runnable runnable : runnables) {
      result = result.consumeException(runnable);
    }
    return result;
  }

  default MultiException collectThrowables(Iterable<? extends Runnable> runnables) {
    MultiException result = this;
    for (Runnable runnable : runnables) {
      result = result.consumeThrowable(runnable);
    }
    return result;
  }

  default MultiException consumeException(Runnable runnable) {
    try {
      runnable.run();
      return this;
    } catch (Exception e) {
      return with(e);
    }
  }

  default MultiException consumeThrowable(Runnable runnable) {
    try {
      runnable.run();
      return this;
    } catch (Throwable e) {
      return with(e);
    }
  }

  public static final MultiException Empty = new EmptyImpl();

  class EmptyImpl implements MultiException {
    private EmptyImpl() {
    }

    public MultiException with(Throwable toAdd) {
      if (toAdd instanceof MultiException) return (MultiException) toAdd;
      return new Single(toAdd);
    }

    public List<Throwable> getThrowables() {
      return ImmutableList.of();
    }

    @Override
    public Maybe<Throwable> getCombinedThrowable() {
      return Maybe.not();
    }

    public <E extends Throwable> void throwIfException(Class<E> exceptionClass) throws E {
    }

    public void throwRuntimeIfAny() {
    }
  }

  class Single implements MultiException {
    private final Throwable throwable;

    private Single(Throwable throwable) {
      this.throwable = throwable;
    }

    public MultiException with(Throwable toAdd) {
      return new Multiple().with(throwable).with(toAdd);
    }

    public List<Throwable> getThrowables() {
      return ImmutableList.of(throwable);
    }

    @Override
    public Maybe<Throwable> getCombinedThrowable() {
      return Maybe.definitely(throwable);
    }

    public <E extends Throwable> void throwIfException(Class<E> exceptionClass) throws E {
      throw Exceptions.propagate(throwable, exceptionClass);
    }

    public void throwRuntimeIfAny() {
      throw Throwables.propagate(throwable);
    }
  }

  class Multiple extends RuntimeException implements MultiException {
    private List<Throwable> nested = Lists.newArrayList();

    private Multiple() {
    }

    public MultiException with(Throwable toAdd) {
      if (toAdd instanceof Multiple) {
        nested.addAll(((Multiple) toAdd).nested);
      } else {
        nested.add(toAdd);
      }
      return this;
    }

    public List<Throwable> getThrowables() {
      return Collections.unmodifiableList(nested);
    }

    @Override
    public Maybe<Throwable> getCombinedThrowable() {
      return Maybe.<Throwable>definitely(this);
    }

    public <E extends Throwable> void throwIfException(Class<E> exceptionClass) throws E {
      throw this;
    }

    public void throwRuntimeIfAny() {
      throw this;
    }

    @Override
    public String getMessage() {
      final StringBuilder builder = new StringBuilder("Multiple exceptions thrown (").append(nested.size()).append(" total):");
      for (int i = 0; i < nested.size(); i++) {
        Throwable throwable = nested.get(i);
        builder.append("\n\n ----> ").append(i + 1).append(") ").append(Throwables.getStackTraceAsString(throwable));
      }
      return builder.toString();
    }
  }
}
