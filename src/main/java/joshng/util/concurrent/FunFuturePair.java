package joshng.util.concurrent;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.ThrowingBiConsumer;
import joshng.util.blocks.ThrowingBiFunction;
import joshng.util.collect.Nothing;
import joshng.util.collect.Pair;

import java.util.Map;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: 6/23/14
 * Time: 12:07 PM
 */
public interface FunFuturePair<K, V> extends FunFuture<Map.Entry<K, V>>, Map.Entry<FunFuture<K>, FunFuture<V>> {
  public static <K, V> FunFuturePair<K, V> asFuturePairFromKey(Map.Entry<? extends ListenableFuture<? extends K>, V> entry) {
    return FunFuture.extendFuture(entry.getKey()).mapPair((K key) -> Pair.of(key, entry.getValue()));
  }

  public static <K, V> FunFuturePair<K, V> asFuturePairFromValue(Map.Entry<K, ? extends ListenableFuture<? extends V>> entry) {
    return FunFuture.extendFuture(entry.getValue()).mapPair((V value) -> Pair.of(entry.getKey(), value));
  }

  public static <K, V> FunFuturePair<K, V> asFuturePair(Map.Entry<? extends ListenableFuture<K>, ? extends ListenableFuture<V>> entry) {
    return FunFuture.extendFuture(entry.getKey()).zip(entry.getValue());
  }

  default FunFuture<K> getKey() {
    return map(Pair.getFirstFromPair());
  }

  default FunFuture<V> getValue() {
    return map(Pair.getSecondFromPair());
  }

  default <O> FunFuture<O> map2(ThrowingBiFunction<? super K, ? super V, O> bifunction) {
    return map(pair -> bifunction.apply(pair.getKey(), pair.getValue()));
  }

  default <O> FunFuture<O> flatMap2(ThrowingBiFunction<? super K, ? super V, ? extends ListenableFuture<O>> bifunction) {
    return flatMap(pair -> bifunction.apply(pair.getKey(), pair.getValue()));
  }

  default FunFuture<Nothing> foreach2(ThrowingBiConsumer<? super K, ? super V> consumer) {
    return map2(consumer.returningNothing());
  }

  @Override
  default FunFuture<V> setValue(FunFuture<V> value) {
    throw new UnsupportedOperationException();
  }

  @Override default PairPromise<K, V> newCompletionPromise() {
    return new PairPromise<>();
  }

  @Override default FunFuturePair<K, V> uponCompletion(Runnable sideEffect) {
    return (FunFuturePair<K, V>) FunFuture.super.uponCompletion(sideEffect);
  }

  @Override default FunFuturePair<K, V> uponCompletion(final FutureCallback<? super Map.Entry<K, V>> callback) {
    return (FunFuturePair<K, V>) FunFuture.super.uponCompletion(callback);
  }

  @Override default FunFuturePair<K, V> uponSuccess(final Consumer<? super Map.Entry<K, V>> successObserver) {
    return (FunFuturePair<K, V>) FunFuture.super.uponSuccess(successObserver);
  }

  @Override default FunFuturePair<K, V> uponFailure(Consumer<? super Throwable> failureObserver) {
    return (FunFuturePair<K, V>) FunFuture.super.uponFailure(failureObserver);
  }

  class PairPromise<T,U> extends Promise<Map.Entry<T,U>> implements FunFuturePair<T,U> {
    public boolean setSuccess(T result1, U result2) {
      return setSuccess(Pair.of(result1, result2));
    }

    @Override public FunFuturePair<T, U> completeWith(ListenableFuture<? extends Map.Entry<T, U>> futureResult) {
      return (FunFuturePair<T, U>) super.completeWith(futureResult);
    }
  }

  class ForwardingFunFuturePair<T, U> extends FunFuture.ForwardingFunFuture<Map.Entry<T,U>> implements FunFuturePair<T,U> {
    @SuppressWarnings("unchecked")
    public ForwardingFunFuturePair(ListenableFuture<? extends Map.Entry<T, U>> delegate) {
      super((ListenableFuture<Map.Entry<T,U>>) delegate);
    }
  }
}

