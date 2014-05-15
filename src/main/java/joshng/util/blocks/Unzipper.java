package joshng.util.blocks;

import joshng.util.collect.Accumulator;
import joshng.util.collect.BiAccumulator;
import joshng.util.collect.Pair;

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

  static <I, K, V> Unzipper<I, K, V> of(F<? super I, ? extends K> keyMapper, F<? super I, ? extends V> valueMapper) {
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
      public F<? super I, ? extends K> keyTransformer() {
        return keyMapper;
      }

      @Override
      public F<? super I, ? extends V> valueTransformer() {
        return valueMapper;
      }
    };
  }

  default <K2> Unzipper<I, K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
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

  default F<? super I, ? extends K> keyTransformer() {
    return F.method(this::getKey);
  }

  default F<? super I, ? extends V> valueTransformer() {
    return F.method(this::getValue);
  }
}
