package com.joshng.util.blocks;

import com.google.common.collect.Multiset;
import com.joshng.util.collect.Accumulator;
import com.joshng.util.collect.Pair;
import com.joshng.util.collect.BiAccumulator;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * User: josh
 * Date: 4/30/14
 * Time: 9:53 AM
 */
public interface Unzipper<I, K, V> extends F<I, Pair<K, V>> {
  K getKey(I input);

  V getValue(I input);

  static <I, K, V> Unzipper<I, K, V> of(F<? super I, K> keyMapper, F<? super I, V> valueMapper) {
    return new Unzipper<I, K, V>() {
      @Override
      public K getKey(I input) {
        return keyMapper.apply(input);
      }

      @Override
      public V getValue(I input) {
        return valueMapper.apply(input);
      }

      @Override
      public F<? super I, K> keyTransformer() {
        return keyMapper;
      }

      @Override
      public F<? super I, V> valueTransformer() {
        return valueMapper;
      }
    };
  }

  default <K2> Unzipper<I, K2, V> mapKeys(Function<? super K, K2> keyMapper) {
    return of(keyTransformer().andThen(keyMapper), valueTransformer());
  }

  default <V2> Unzipper<I, K, V2> mapValues(Function<? super V, ? extends V2> valueMapper) {
    return of(keyTransformer(), valueTransformer().andThen(valueMapper));
  }

  default Pair<K, V> apply(I input) {
    return Pair.of(getKey(input), getValue(input));
  }

  default Sink<I> andThenSink2(BiConsumer<? super K, ? super V> sink) {
    return input -> sink.accept(getKey(input), getValue(input));
  }

  default <O> Accumulator<I, O> andThenAccumulate(BiAccumulator<? super K, ? super V, O> accumulator) {
    return accumulator.compose2(this);
  }

  static <K> Unzipper<Multiset.Entry<K>, K, Integer> multisetEntry() {
    return Unzipper.<Multiset.Entry<K>, K, Integer>of((Multiset.Entry<K> entry) -> entry.getElement(), (Multiset.Entry<K> entry) -> entry.getCount());
  }

  default F<? super I, K> keyTransformer() {
    return F.function(this::getKey);
  }

  default F<? super I, V> valueTransformer() {
    return F.function(this::getValue);
  }
}
