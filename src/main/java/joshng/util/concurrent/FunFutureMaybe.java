package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.F;
import joshng.util.blocks.Source;
import joshng.util.blocks.Tapper;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Maybe;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 5/7/14
 * Time: 10:01 AM
 */
public interface FunFutureMaybe<T> extends FunFuture<Maybe<T>> {
  FunFutureMaybe EMPTY_FUTURE = new Not();
  F MAYBE_WRAPPER = FunFuture.maybeMapper(Maybe.of());
  F<Maybe<?>, Boolean> IS_DEFINED = Maybe.IS_DEFINED.asFunction();

//  static <T> FunFutureMaybe<T> immediateFutureMaybeOf(@Nullable T value) {
//    return value == null ? futureMaybeNot() : immediateFutureMaybe(value);
//  }

  static <T> FunFutureMaybe<T> immediateFutureOfMaybe(Maybe<T> maybe) {
    return maybe.isDefined() ? new ForwardingFunFutureMaybe<>(Futures.immediateFuture(maybe)) : futureMaybeNot();
  }

  static <T> FunFutureMaybe<T> immediateDefiniteFuture(@Nonnull T value) {
    return new ForwardingFunFutureMaybe<>(Futures.immediateFuture(definitely(value)));
  }

  @SuppressWarnings("unchecked")
  static <T> FunFutureMaybe<T> futureMaybeNot() {
    return EMPTY_FUTURE;
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

  default <O> FunFutureMaybe<O> mapIfDefined(Function<? super T, ? extends O> f) {
    return mapMaybe(Maybe.mapper(f));
  }

  default <O> FunFutureMaybe<O> mapMaybeIfDefined(Function<? super T, Maybe<O>> f) {
    return mapMaybe(Maybe.flatMapper(f));
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

  default FunFuture<T> getOrFail(String format, Object... args) {
    return map(Maybe.getter(format, args));
  }

  default FunFuture<T> getOrElse(T alternateValue) {
    return map(Maybe.getterWithDefault(alternateValue));
  }

  default FunFuture<T> getOrElseFrom(Supplier<? extends T> alternateValue) {
    return map(Maybe.getterWithDefaultFrom(alternateValue));
  }

  default FunFutureMaybe<T> orElseFrom(Supplier<? extends ListenableFuture<Maybe<T>>> alternateFutureSupplier) {
    return flatMapMaybe(maybe -> maybe.isDefined() ? this : alternateFutureSupplier.get());
  }

  default <U> FunFutureMaybe<U> cast(Class<U> castClass) {
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
    public FunFutureMaybe mapIfDefined(Function f) {
      return this;
    }

    @Override
    public FunFutureMaybe flatMapIfDefined(AsyncFunction f) {
      return this;
    }

    @Override
    public FunFutureMaybe mapMaybeIfDefined(Function f) {
      return this;
    }

    @Override
    public FunFutureMaybe flatMapMaybeIfDefined(AsyncFunction f) {
      return this;
    }

    @Override
    public FunFuture<Boolean> isEmpty() {
      return FunFuture.TRUE;
    }

    @Override
    public FunFuture<Boolean> isDefined() {
      return FunFuture.FALSE;
    }

    @Override
    public FunFuture getOrElse(Object alternateValue) {
      return FunFuture.immediateFuture(alternateValue);
    }

    @Override
    public FunFutureMaybe cast(Class castClass) {
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
