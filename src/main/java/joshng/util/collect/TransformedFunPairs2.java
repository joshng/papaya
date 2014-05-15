package joshng.util.collect;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * User: josh
 * Date: 4/30/14
 * Time: 2:47 PM
 */
class TransformedFunPairs2<I, K, V> implements FunPairs<K, V> {
  private Iterable<I> input;
  private final F<? super I, ? extends K> keyTransformer;
  private final F<? super I, ? extends V> valueTransformer;

  TransformedFunPairs2(Iterable<I> input, F<? super I, ? extends K> keyTransformer, F<? super I, ? extends V> valueTransformer) {
    this.input = input;
    this.keyTransformer = keyTransformer;
    this.valueTransformer = valueTransformer;
  }

  @Override
  public Iterable<Map.Entry<K, V>> delegate() {
    return Iterables.transform(input, p -> Pair.of(keyTransformer.apply(p), valueTransformer.apply(p)));
  }

  @Override
  public Iterable<K> keysDelegate() {
    return Iterables.transform(input, keyTransformer);
  }

  @Override
  public Iterable<V> valuesDelegate() {
    return Iterables.transform(input, valueTransformer);
  }

  @Override
  public <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer) {
    return new TransformedFunPairs2<>(input, this.keyTransformer.andThen(keyTransformer), valueTransformer);
  }

  @Override
  public <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer) {
    return new TransformedFunPairs2<>(input, this.keyTransformer, this.valueTransformer.andThen(valueTransformer));
  }

  @Override
  public void foreach2(BiConsumer<? super K, ? super V> visitor) {
    for (I entry : input) {
      visitor.accept(keyTransformer.apply(entry), valueTransformer.apply(entry));
    }
  }

  @Override
  public <O> FunIterable<O> map2(F2<? super K, ? super V, ? extends O> transformer) {
    return new FunctionalIterable<>(() -> new AbstractIterator<O>() {
      Iterator<I> iterator = input.iterator();

      @Override
      protected O computeNext() {
        if (!iterator.hasNext()) return endOfData();
        I nextRaw = iterator.next();
        return transformer.apply(keyTransformer.apply(nextRaw), valueTransformer.apply(nextRaw));
      }
    });
  }

  @Override
  public ImmutableMap<K, V> toMap() {
    return accumulate2(Accumulator.immutableMap());
  }
}
