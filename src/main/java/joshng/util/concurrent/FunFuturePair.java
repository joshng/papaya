package joshng.util.concurrent;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.Sink2;
import joshng.util.collect.Nothing;
import joshng.util.collect.Pair;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: 6/23/14
 * Time: 12:07 PM
 */
public interface FunFuturePair<T,U> extends FunFuture<Map.Entry<T,U>>, Map.Entry<FunFuture<T>, FunFuture<U>> {
  default FunFuture<T> getKey() {
    return map(Pair.getFirstFromPair());
  }

  default FunFuture<U> getValue() {
    return map(Pair.getSecondFromPair());
  }

  default <V> FunFuture<V> map2(BiFunction<? super T, ? super U, V> bifunction) {
    return map(pair -> bifunction.apply(pair.getKey(), pair.getValue()));
  }

  default <V> FunFuture<V> flatMap2(BiFunction<? super T, ? super U, ? extends ListenableFuture<V>> bifunction) {
    return flatMap(pair -> bifunction.apply(pair.getKey(), pair.getValue()));
  }

  default FunFuture<Nothing> foreach2(BiConsumer<? super T, ? super U> consumer) {
    return map2(Sink2.extendBiConsumer(consumer));
  }

  @Override
  default FunFuture<U> setValue(FunFuture<U> value) {
    throw new UnsupportedOperationException();
  }

  @Override default PairPromise<T, U> newCompletionPromise() {
    return new PairPromise<>();
  }

  @Override default FunFuturePair<T, U> uponCompletion(Runnable sideEffect) {
    return (FunFuturePair<T, U>) FunFuture.super.uponCompletion(sideEffect);
  }

  @Override default FunFuturePair<T, U> uponCompletion(final FutureCallback<? super Map.Entry<T, U>> callback) {
    return (FunFuturePair<T, U>) FunFuture.super.uponCompletion(callback);
  }

  @Override default FunFuturePair<T, U> uponSuccess(final Consumer<? super Map.Entry<T, U>> successObserver) {
    return (FunFuturePair<T, U>) FunFuture.super.uponSuccess(successObserver);
  }

  @Override default FunFuturePair<T, U> uponFailure(Consumer<? super Throwable> failureObserver) {
    return (FunFuturePair<T, U>) FunFuture.super.uponFailure(failureObserver);
  }

  class PairPromise<T,U> extends Promise<Map.Entry<T,U>> implements FunFuturePair<T,U> {}

  class ForwardingFunFuturePair<T, U> extends FunFuture.ForwardingFunFuture<Map.Entry<T,U>> implements FunFuturePair<T,U> {
    @SuppressWarnings("unchecked")
    public ForwardingFunFuturePair(ListenableFuture<? extends Map.Entry<T, U>> delegate) {
      super((ListenableFuture<Map.Entry<T,U>>) delegate);
    }
  }
}

