package com.joshng.util.blocks;

import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.joshng.util.collect.Either;
import com.joshng.util.collect.Maybe;
import com.joshng.util.exceptions.ExceptionPolicy;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:10:50 AM
 */
@FunctionalInterface
public interface Source<T> extends F<Object, T>, Supplier<T>, com.google.common.base.Supplier<T>, Callable<T> {
  static final Source NULL_SOURCE = Source.ofInstance(null);
  static final Source MAYBE_NOT = Source.ofInstance(Maybe.not());
  static final Source<Boolean> FALSE = Source.ofInstance(Boolean.FALSE);
  static final Source<Boolean> TRUE = Source.ofInstance(Boolean.TRUE);

  static <T> Source<T> source(Source<T> method) {
    return method;
  }

  @SuppressWarnings("unchecked")
  public static <T> Source<T> nullSource() {
    return NULL_SOURCE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Source<Maybe<T>> maybeNot() {
    return MAYBE_NOT;
  }

  public static <T> Source<T> ofInstance(final T value) {
    return new SourceOfInstance<>(value);
  }

  @SuppressWarnings("unchecked")
  public static <T> Source<T> extendSupplier(final Supplier<? extends T> supplier) {
    if (supplier instanceof Source) return (Source<T>) supplier;
    return supplier::get;
  }

  @SuppressWarnings("unchecked")
  public static <T> Source<T> extendGuava(final com.google.common.base.Supplier<? extends T> supplier) {
    if (supplier instanceof Source) return (Source<T>) supplier;
    return supplier::get;
  }

  public static <T> Source<T> extendRunnable(final Runnable runnable, final T result) {
    return () -> {
      runnable.run();
      return result;
    };
  }

  @SuppressWarnings({"unchecked"})
  public static <T> F<Supplier<? extends T>, T> getter() {
    // despite IntelliJ's suggestion, a method reference does NOT work here. Don't ask me...
    return s -> s.get();
  }

  @Override
  default <U> Source<U> andThen(final Function<? super T, ? extends U> transformer) {
//        return F.extendF(transformer).bindFrom(this);
    return () -> transformer.apply(get());
  }

  default DoubleSupplier toDouble(ToDoubleFunction<? super T> toDouble) {
    return () -> toDouble.applyAsDouble(get());
  }

  default <U> Source<U> map(final Function<? super T, U> transformer) {
    return andThen(transformer);
  }

  default <U> Source<U> flatMap(final Function<? super T, ? extends Supplier<U>> transformer) {
    return () -> transformer.apply(get()).get();
  }

  default Source<T> memoize() {
    return extendGuava(Suppliers.memoize(this));
  }

  default Source<T> memoizeWithExpiration(long duration, TimeUnit timeUnit) {
    return extendGuava(Suppliers.memoizeWithExpiration(this, duration, timeUnit));
  }

  /**
   * Returns a Source that wraps the result of calling get in a Maybe, returning Maybe.not()
   * if the underlying call throws a Throwable which is "handled" by the provided ExceptionPolicy.
   * <p>
   * <p>NOTE: any Throwable that is NOT handled by the ExceptionPolicy will still be thrown out
   * (wrapped in a RuntimeException if necessary by {@link Throwables#propagate}).
   */
  default Source<Maybe<T>> handlingExceptions(final ExceptionPolicy exceptionPolicy) {
    return () -> {
      try {
        return Maybe.definitely(get());
      } catch (Throwable e) {
        if (exceptionPolicy.apply(e)) return Maybe.not();
        throw Throwables.propagate(e);
      }
    };
  }

  default Source<T> recover(final ThrowingFunction<? super Exception, ? extends T> recovery) {
    return () -> {
      try {
        return get();
      } catch (Exception t) {
        try {
          return recovery.apply(t);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
    };
  }

  default Source<Either<Exception, T>> exceptionToLeft() {
    return () -> F.applyExceptionToLeft(null, this);
  }

  default T apply(Object input) {
    return get();
  }

  @Override
  default T call() {
    return get();
  }

  static class SourceOfInstance<T> implements Source<T> {
    private final T value;

    public SourceOfInstance(T value) {
      this.value = value;
    }

    @Override
    public T get() {
      return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Source<U> andThen(Function<? super T, ? extends U> transformer) {
      return (Source<U>) F.extendFunction(transformer).bind(value);
    }

    @Override
    public Source<T> memoize() {
      return this;
    }

    @Override
    public Source<T> memoizeWithExpiration(long duration, TimeUnit timeUnit) {
      return this;
    }

    @Override
    public Source<Maybe<T>> handlingExceptions(ExceptionPolicy exceptionPolicy) {
      return Source.ofInstance(Maybe.definitely(value));
    }

    @Override
    public Source<Either<Exception, T>> exceptionToLeft() {
      return Source.ofInstance(Either.<Exception, T>right(value));
    }

    @Override
    public <U> Source<U> flatMap(Function<? super T, ? extends Supplier<U>> transformer) {
      return Source.<U>getter().compose(transformer).bind(value);
    }
  }

}
