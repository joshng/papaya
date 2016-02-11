package com.joshng.util.collect;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 11:18 AM
 */
public interface ForwardingMaybe<T> {
  Maybe<T> getMaybe();

  default T getOrThrow() {
    return getMaybe().getOrThrow();
  }

  default T getOrThrow(String format, Object... args) throws NoSuchElementException {
    return getMaybe().getOrThrow(format, args);
  }

  default <E extends Throwable> T getOrThrowFrom(Supplier<E> throwableSupplier) throws E {
    return getMaybe().getOrThrowFrom(throwableSupplier);
  }

  @Nullable
  default T orNull() {
    return getMaybe().orNull();
  }

  default T getOrElse(T alternateValue) {
    return getMaybe().getOrElse(alternateValue);
  }

  default T getOrElseFrom(Supplier<? extends T> alternateValueSupplier) {
    return getMaybe().getOrElseFrom(alternateValueSupplier);
  }

  default Maybe<T> orElse(Maybe<T> alternateValue) {
    return getMaybe().orElse(alternateValue);
  }

  default Maybe<T> orElseFrom(Supplier<? extends Maybe<? extends T>> alternateValueSupplier) {
    return getMaybe().orElseFrom(alternateValueSupplier);
  }

  default <U> Maybe<U> map(Function<? super T, ? extends U> transformer) {
    return getMaybe().map(transformer);
  }

  default <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> transformer) {
    return getMaybe().flatMap(transformer);
  }

  default <O> O map(MaybeFunction<? super T, O> transformer) {
    return getMaybe().map(transformer);
  }

  default <K, V> Maybe.Pair<K, V> mapPair(Function<? super T, ? extends Map.Entry<K, V>> pairComputer) {
    return getMaybe().mapPair(pairComputer);
  }

  default <K, V> Maybe.Pair<K, V> flatMapPair(Function<? super T, ? extends Maybe.Pair<K, V>> pairComputer) {
    return getMaybe().flatMapPair(pairComputer);
  }

  default Maybe<T> foreach(Consumer<? super T> handler) {
    return getMaybe().foreach(handler);
  }

  default Maybe<T> orElseRun(Runnable runnable) {
    return getMaybe().orElseRun(runnable);
  }

  default Maybe<T> filter(Predicate<? super T> filter) {
    return getMaybe().filter(filter);
  }

  default Maybe<T> filterNot(Predicate<? super T> filter) {
    return getMaybe().filterNot(filter);
  }

  default <V> Maybe.Pair<T, V> asKeyTo(Function<? super T, ? extends V> valueComputer) {
    return getMaybe().asKeyTo(valueComputer);
  }

  default <K> Maybe.Pair<K, T> asValueFrom(Function<? super T, ? extends K> keyComputer) {
    return getMaybe().asValueFrom(keyComputer);
  }

  default <U> Maybe<U> filter(Class<U> castClass) {
    return getMaybe().filter(castClass);
  }

  default <U> Maybe<U> cast(Class<U> castClass) {
    return getMaybe().cast(castClass);
  }

  default boolean valueMatches(Predicate<? super T> predicate) {
    return getMaybe().valueMatches(predicate);
  }

  default boolean valueMatchesNot(Predicate<? super T> predicate) {
    return getMaybe().valueMatchesNot(predicate);
  }

  default boolean isDefined() {
    return getMaybe().isDefined();
  }

  default boolean isEmpty() {
    return getMaybe().isEmpty();
  }
}
