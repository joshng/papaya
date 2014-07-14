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
class TransformedFunPairs<K, V, K1, V1> implements FunPairs<K1, V1> {
  private FunPairs<K, V> input;
  private final F<? super K, ? extends K1> keyTransformer;
  private final F<? super V, ? extends V1> valueTransformer;

  TransformedFunPairs(FunPairs<K, V> input, F<? super K, ? extends K1> keyTransformer, F<? super V, ? extends V1> valueTransformer) {
    this.input = input;
    this.keyTransformer = keyTransformer;
    this.valueTransformer = valueTransformer;
  }

  @Override
  public Iterable<Map.Entry<K1, V1>> delegate() {
    return Iterables.transform(input.delegate(), p -> Pair.of(keyTransformer.apply(p.getKey()), valueTransformer.apply(p.getValue())));
  }

  @Override
  public Iterable<K1> keysDelegate() {
    return Iterables.transform(input.keysDelegate(), keyTransformer);
  }

  @Override
  public Iterable<V1> valuesDelegate() {
    return Iterables.transform(input.valuesDelegate(), valueTransformer);
  }

  @Override
  public <K2> FunPairs<K2, V1> mapKeys(Function<? super K1, ? extends K2> keyTransformer) {
    return new TransformedFunPairs<>(input, this.keyTransformer.andThen(keyTransformer), valueTransformer);
  }

  @Override
  public <V2> FunPairs<K1, V2> mapValues(Function<? super V1, ? extends V2> valueTransformer) {
    return new TransformedFunPairs<>(input, this.keyTransformer, this.valueTransformer.andThen(valueTransformer));
  }

  @Override
  public void foreach2(BiConsumer<? super K1, ? super V1> visitor) {
    for (Map.Entry<K, V> entry : input) {
      visitor.accept(keyTransformer.apply(entry.getKey()), valueTransformer.apply(entry.getValue()));
    }
  }

  @Override
  public <O> FunIterable<O> map2(F2<? super K1, ? super V1, O> transformer) {
    return new FunctionalIterable<>(() -> new AbstractIterator<O>() {
      Iterator<Map.Entry<K, V>> iterator = input.iterator();

      @Override
      protected O computeNext() {
        if (!iterator.hasNext()) return endOfData();
        Map.Entry<K, V> nextRaw = iterator.next();
        return transformer.apply(keyTransformer.apply(nextRaw.getKey()), valueTransformer.apply(nextRaw.getValue()));
      }
    });
  }

  @Override
  public ImmutableMap<K1, V1> toMap() {
    return accumulate2(Accumulator.immutableMap());
  }
}
