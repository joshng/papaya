package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.Sink2;
import joshng.util.collect.Nothing;
import joshng.util.collect.Pair;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * User: josh
 * Date: 6/23/14
 * Time: 12:07 PM
 */
public interface FunFuturePair<T,U> extends FunFuture<Map.Entry<T,U>> {
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

  class ForwardingFunFuturePair<T, U> extends FunFuture.ForwardingFunFuture<Map.Entry<T,U>> implements FunFuturePair<T,U> {
    @SuppressWarnings("unchecked")
    public ForwardingFunFuturePair(ListenableFuture<? extends Map.Entry<T, U>> delegate) {
      super((ListenableFuture<Map.Entry<T,U>>) delegate);
    }
  }
}

