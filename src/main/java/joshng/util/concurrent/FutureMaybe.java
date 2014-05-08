package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.F;
import joshng.util.collect.Maybe;

import java.util.function.Function;
import java.util.function.Supplier;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 5/7/14
 * Time: 10:01 AM
 */
public interface FutureMaybe<T> extends FunFuture<Maybe<T>> {
  FutureMaybe EMPTY_FUTURE = immediateFutureMaybe(Maybe.not());
  F MAYBE_WRAPPER = FunFuture.maybeMapper(Maybe.of());

  static <T> FutureMaybe<T> immediateFutureMaybe(T value) {
    return new ForwardingFutureMaybe<>(FunFuture.immediateFuture(definitely(value)));
  }

  @SuppressWarnings("unchecked")
  static <T> FutureMaybe<T> futureMaybeNot() {
      return EMPTY_FUTURE;
  }

  @SuppressWarnings("unchecked")
  static <T> F<ListenableFuture<? extends T>, FunFuture<Maybe<T>>> maybeWrapper() {
      return MAYBE_WRAPPER;
  }

  static <T> FunFuture<Maybe<T>> asFutureOfMaybe(Maybe<? extends ListenableFuture<? extends T>> maybeOfFuture) {
      return maybeOfFuture.map(FutureMaybe.<T>maybeWrapper()).getOrElse(FutureMaybe.<T>futureMaybeNot());
  }

  default <O> FutureMaybe<O> mapIfDefined(Function<? super T, ? extends O> f) {
    return mapMaybe(Maybe.mapper(f));
  }

  default <O> FutureMaybe<O> flatMapIfDefined(AsyncFunction<? super T, O> f) {
    return flatMapMaybe(maybeFlatMapper(f));
  }

  default FunFuture<T> getOrThrow() {
    return map(Maybe.getter());
  }

  default FunFuture<T> getOrElse(T alternateValue) {
    return map(Maybe.getterWithDefault(alternateValue));
  }

  default FunFuture<T> getOrElseFrom(Supplier<? extends T> alternateValue) {
    return map(Maybe.getterWithDefaultFrom(alternateValue));
  }

  default FunFuture<T> getOrThrow(String format, Object... args) {
    return map(Maybe.getter(format, args));
  }

  default <U> FutureMaybe<U> cast(Class<U> castClass) {
    return mapMaybe(Maybe.caster(castClass));
  }

  static <I, O> AsyncF<Maybe<I>, Maybe<O>> maybeFlatMapper(AsyncFunction<? super I, O> f) {
    return maybe -> asFutureOfMaybe(maybe.map(AsyncF.asyncF(f)));
  }
}

class ForwardingFutureMaybe<T> extends FunFuture.ForwardingFunFuture<Maybe<T>> implements FutureMaybe<T> {
  protected ForwardingFutureMaybe(ListenableFuture<Maybe<T>> delegate) {
    super(delegate);
  }
}
