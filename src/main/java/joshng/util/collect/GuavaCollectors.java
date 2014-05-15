package joshng.util.collect;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * User: josh
 * Date: 4/26/14
 * Time: 10:53 AM
 */
public class GuavaCollectors {
  public static <K, V> Collector<Map.Entry<K, V>, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> immutableMapCollector() {
    return new Collector<Map.Entry<K, V>, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>>() {
      @Override
      public Supplier<ImmutableMap.Builder<K, V>> supplier() {
        return ImmutableMap::<K, V>builder;
      }

      @Override
      public BiConsumer<ImmutableMap.Builder<K, V>, Map.Entry<K, V>> accumulator() {
        return (kvBuilder, kvEntry) -> kvBuilder.put(kvEntry);
      }

      @Override
      public BinaryOperator<ImmutableMap.Builder<K, V>> combiner() {
        return (kvBuilder, kvBuilder2) -> kvBuilder.putAll(kvBuilder2.build());
      }

      @Override
      public Function<ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> finisher() {
        return ImmutableMap.Builder::build;
      }

      @Override
      public Set<Characteristics> characteristics() {
        throw new UnsupportedOperationException(".characteristics has not been implemented");
      }
    };
  }
}
