package com.joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.joshng.util.blocks.*;
import com.joshng.util.collect.Maybe;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * User: josh
 * Date: 5/7/14
 * Time: 10:01 AM
 */
public interface FunFutureMaybe<T> extends FunFuture<Maybe<T>> {
  FunFutureMaybe EMPTY_FUTURE = new Not();
  F MAYBE_WRAPPER = FunFuture.maybeMapper(Maybe.of());
  F<Maybe<?>, Boolean> IS_DEFINED = Maybe.IS_DEFINED.asFunction();
  static <T, U> ThrowingFunction<Maybe<T>, Maybe<U>> throwingMaybeMapper(ThrowingFunction<? super T, ? extends U> f) {
    return maybe -> maybe.isDefined() ? Maybe.of(f.apply(maybe.getOrThrow())) : Maybe.not();
  }

  static <T, U> ThrowingFunction<Maybe<T>, Maybe<U>> throwingMaybeFlatMapper(ThrowingFunction<? super T, Maybe<U>> f) {
    return maybe -> maybe.isDefined() ? f.apply(maybe.getOrThrow()) : Maybe.not();
  }

//  static <T> FunFutureMaybe<T> immediateFutureMaybeOf(@Nullable T value) {
//    return value == null ? futureMaybeNot() : immediateFutureMaybe(value);
//  }

  static <T> FunFutureMaybe<T> immediateFutureOfMaybe(Maybe<T> maybe) {
    return maybe.isDefined() ? new ForwardingFunFutureMaybe<>(Futures.immediateFuture(maybe)) : futureMaybeNot();
  }

  static <T> FunFutureMaybe<T> immediateDefiniteFuture(@Nonnull T value) {
    return new ForwardingFunFutureMaybe<>(Futures.immediateFuture(Maybe.definitely(value)));
  }

  @SuppressWarnings("unchecked")
  static <T> FunFutureMaybe<T> futureMaybeNot() {
    return EMPTY_FUTURE;
  }

  static <T> FunFutureMaybe<T> failedFutureMaybe(Throwable t) {
    return new ForwardingFunFutureMaybe<>(Futures.<Maybe<T>>immediateFailedFuture(t));
  }

  static <T> FunFutureMaybe<T> callSafelyMaybe(Callable<? extends FunFutureMaybe<T>> futureSupplier) {
    try {
      return futureSupplier.call();
    } catch (Exception e) {
      return failedFutureMaybe(e);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> F<ListenableFuture<? extends T>, FunFutureMaybe<T>> maybeWrapper() {
    return MAYBE_WRAPPER;
  }

  static <T> FunFutureMaybe<T> asFutureMaybe(Maybe<? extends ListenableFuture<? extends T>> maybeOfFuture) {
    return maybeOfFuture.map(FunFutureMaybe.<T>maybeWrapper()).getOrElse(FunFutureMaybe.<T>futureMaybeNot());
  }

  static <O> ForwardingFunFutureMaybe<O> wrapFutureMaybe(ListenableFuture<Maybe<O>> futureMaybe) {
    return new ForwardingFunFutureMaybe<>(futureMaybe);
  }

  default FunFuture<Boolean> isDefined() {
    return map(Maybe::isDefined);
  }

  default FunFuture<Boolean> isEmpty() {
    return map(Maybe::isEmpty);
  }

  default <O> FunFutureMaybe<O> mapIfDefined(ThrowingFunction<? super T, ? extends O> f) {
    return mapMaybe(throwingMaybeMapper(f));
  }

  default <O> FunFutureMaybe<O> mapMaybeIfDefined(ThrowingFunction<? super T, Maybe<O>> f) {
    return mapMaybe(throwingMaybeFlatMapper(f));
  }

  default <O> FunFutureMaybe<O> flatMapIfDefined(AsyncFunction<? super T, O> f) {
    return flatMapMaybe(flatMapper(f));
  }

  default <O> FunFutureMaybe<O> flatMapMaybeIfDefined(AsyncFunction<? super T, Maybe<O>> f) {
    return flatMapMaybe(maybeFlatMapper(f));
  }

  default FunFutureMaybe<T> foreachIfDefined(Consumer<? super T> consumer) {
    return mapIfDefined(Tapper.extendConsumer(consumer));
  }

  default FunFuture<T> getOrFail() {
    return map(Maybe.getter());
  }

  default FunFuture<T> getOrFailFrom(Supplier<? extends Exception> exceptionSupplier) {
    return map((ThrowingFunction<Maybe<T>, T>)maybe -> maybe.getOrThrowFrom(exceptionSupplier));
  }

  default FunFuture<T> getOrFail(String format, Object... args) {
    return map(Maybe.getter(format, args));
  }

  default FunFuture<T> getOrElse(T alternateValue) {
    return map(Maybe.getterWithDefault(alternateValue));
  }

  default FunFuture<T> getOrElseFrom(Supplier<? extends T> alternateValue) {
    return map(Maybe.getterWithDefaultFrom(alternateValue));
  }

  default FunFutureMaybe<T> orElseFrom(Supplier<? extends Maybe<T>> alternateMaybe) {
    return mapMaybe(maybe -> maybe.orElseFrom(alternateMaybe));
  }

  default FunFutureMaybe<T> orElseFromFlattened(Supplier<? extends ListenableFuture<Maybe<T>>> alternateFutureSupplier) {
    return flatMapMaybe(maybe -> maybe.isDefined() ? this : alternateFutureSupplier.get());
  }

  default FunFuture<T> getOrElseFromFlattened(Supplier<? extends ListenableFuture<T>> alternateFuture) {
    return flatMap(maybe -> maybe.<ListenableFuture<T>>map(FunFuture::immediateFuture).getOrElseFrom(alternateFuture));
  }

  default <U> FunFutureMaybe<U> castMaybe(Class<U> castClass) {
    return mapMaybe(Maybe.caster(castClass));
  }

  default FunFutureMaybe<T> filterMaybe(final Predicate<T> filter) {
    return mapMaybe(maybe -> maybe.filter(filter));
  }

  @Override
  default <E extends Exception> FunFutureMaybe<T> recover(Class<E> exceptionType, ThrowingFunction<? super E, ? extends Maybe<T>> alternateResultSource) {
    return (FunFutureMaybe<T>) FunFuture.super.recover(exceptionType, alternateResultSource);
  }

  @Override
  default FunFutureMaybe<T> recover(Predicate<? super Exception> exceptionFilter, final ThrowingFunction<? super Exception, ? extends Maybe<T>> alternateResultSource) {
    return (FunFutureMaybe<T>) FunFuture.super.recover(exceptionFilter, alternateResultSource);
  }

  default <E extends Exception> FunFutureMaybe<T> recoverWith(Class<E> exceptionType, AsyncFunction<? super E, ? extends Maybe<T>> alternateResultSource) {
    return (FunFutureMaybe<T>) FunFuture.super.recoverWith(exceptionType, alternateResultSource);
  }

  default FunFutureMaybe<T> recoverWith(Predicate<? super Exception> exceptionFilter, AsyncFunction<? super Exception, ? extends Maybe<T>> alternateResultSource) {
    return (FunFutureMaybe<T>) FunFuture.super.recoverWith(exceptionFilter, alternateResultSource);
  }

  default FunFutureMaybe<T> recoverWith(final AsyncFunction<? super Exception, ? extends Maybe<T>> exceptionHandler) {
    return new MaybePromise<T>().completeOrRecoverWith(this, exceptionHandler);
  }

  default <E extends Exception> FunFutureMaybe<T> recoverAsUndefined(Class<E> exceptionType) {
    return recover(exceptionType, Source.maybeNot());
  }

  default FunFutureMaybe<T> recoverAsUndefined(Predicate<? super Exception> exceptionFilter) {
    return recover(exceptionFilter, Source.maybeNot());
  }

  @Override default FunFutureMaybe<T> tap(ThrowingConsumer<? super Maybe<T>> sideEffect) {
    return (FunFutureMaybe<T>) FunFuture.super.tap(sideEffect);
  }

  @Override default <O> FunFutureMaybe<T> tapAsync(AsyncF<? super Maybe<T>, O> sideEffect) {
    return (FunFutureMaybe<T>) FunFuture.super.tapAsync(sideEffect);
  }

  @Override default FunFutureMaybe<T> uponCompletion2(
          Consumer<? super Maybe<T>> successObserver, Consumer<? super Exception> errorObserver
  ) {
    return (FunFutureMaybe<T>) FunFuture.super.uponCompletion2(successObserver, errorObserver);
  }

  @Override default FunFutureMaybe<T> uponCompletion(Runnable sideEffect) {
    return (FunFutureMaybe<T>) FunFuture.super.uponCompletion(sideEffect);
  }

  @Override default FunFutureMaybe<T> uponCompletion(final FutureCallback<? super Maybe<T>> callback) {
    return (FunFutureMaybe<T>) FunFuture.super.uponCompletion(callback);
  }

  @Override default FunFutureMaybe<T> uponSuccess(final Consumer<? super Maybe<T>> successObserver) {
    return (FunFutureMaybe<T>) FunFuture.super.uponSuccess(successObserver);
  }

  @Override default FunFutureMaybe<T> uponFailure(Consumer<? super Throwable> failureObserver) {
    return (FunFutureMaybe<T>) FunFuture.super.uponFailure(failureObserver);
  }

  @Override default MaybePromise<T> newCompletionPromise() {
    return new MaybePromise<>();
  }

  static <I, O> AsyncF<Maybe<I>, Maybe<O>> flatMapper(AsyncFunction<? super I, O> f) {
    return maybe -> asFutureMaybe(maybe.map(AsyncF.asyncF(f)));
  }

  static <I, O> AsyncF<Maybe<I>, Maybe<O>> maybeFlatMapper(AsyncFunction<? super I, Maybe<O>> f) {
    return maybe -> maybe.map(AsyncF.asyncF(f)).getOrElse(FunFutureMaybe.futureMaybeNot());
  }

  class Not extends ForwardingFunFutureMaybe {
    private Not() {
      super(Futures.immediateFuture(Maybe.not()));
    }

    @Override
    public boolean isDone() {
      return true;
    }

    @Override
    public FunFutureMaybe mapIfDefined(ThrowingFunction f) {
      return this;
    }

    @Override
    public FunFutureMaybe flatMapIfDefined(AsyncFunction f) {
      return this;
    }

    @Override
    public FunFutureMaybe mapMaybeIfDefined(ThrowingFunction f) {
      return this;
    }

    @Override
    public FunFutureMaybe flatMapMaybeIfDefined(AsyncFunction f) {
      return this;
    }

    @Override
    public FunFuture<Boolean> isEmpty() {
      return TRUE;
    }

    @Override
    public FunFuture<Boolean> isDefined() {
      return FALSE;
    }

    @Override
    public FunFuture getOrElse(Object alternateValue) {
      return FunFuture.immediateFuture(alternateValue);
    }

    @Override
    public FunFutureMaybe castMaybe(Class castClass) {
      return this;
    }

    @Override
    public FunFutureMaybe filterMaybe(Predicate filter) {
      return this;
    }
  }

  class MaybePromise<T> extends Promise<Maybe<T>> implements FunFutureMaybe<T> {
    @Override public FunFutureMaybe<T> completeOrRecoverWith(
            ListenableFuture<Maybe<T>> future,
            AsyncFunction<? super Exception, ? extends Maybe<T>> exceptionHandler
    ) {
      super.completeOrRecoverWith(future, exceptionHandler);
      return this;
    }
  }
}

class ForwardingFunFutureMaybe<T> extends FunFuture.ForwardingFunFuture<Maybe<T>> implements FunFutureMaybe<T> {
  protected ForwardingFunFutureMaybe(ListenableFuture<Maybe<T>> delegate) {
    super(delegate);
  }
}
