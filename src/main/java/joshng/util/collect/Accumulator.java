package joshng.util.collect;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import joshng.util.blocks.Sink;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * User: josh
 * Date: 4/26/14
 * Time: 11:57 AM
 */
public interface Accumulator<I, O> extends Sink<I>, Supplier<O> {
    static <I, A, O> Accumulator<I, O> from(Collector<I, A, O> collector) {
        A instance = collector.supplier().get();
        BiConsumer<A,I> accumulator = collector.accumulator();
        return new Accumulator<I, O>() {
            @Override
            public void accept(I i) {
                accumulator.accept(instance, i);
            }

            @Override
            public O get() {
                return collector.finisher().apply(instance);
            }
        };
    }

    static <I, O> Accumulator<I,O> of(Consumer<? super I> consumer, Supplier<? extends O> supplier) {
        return new Accumulator<I, O>() {
            @Override
            public void accept(I i) {
                consumer.accept(i);
            }

            @Override
            public O get() {
                return supplier.get();
            }
        };
    }

    static <T> Accumulator<T, ImmutableList<T>> immutableList() {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        return of(builder::add, builder::build);
    }

    static <T> Accumulator<T, ImmutableSet<T>> immutableSet() {
        ImmutableSet.Builder<T> builder = ImmutableSet.builder();
        return of(builder::add, builder::build);
    }

    static <K,V> BiAccumulator<K, V, ImmutableMap<K,V>> immutableMap() {
        return new ImmutableMapAccumulator<>();
    }

    static <K,V> BiAccumulator<K, V, ImmutableBiMap<K,V>> biMap() {
        return new BiAccumulator<K, V, ImmutableBiMap<K, V>>() {
          private final ImmutableBiMap.Builder<K, V> builder = ImmutableBiMap.builder();

          @Override
          public void accept(K k, V v) {
              builder.put(k, v);
          }

          @Override
          public ImmutableBiMap<K, V> get() {
              return builder.build();
          }
        };
    }

  static <K,V> BiAccumulator<K, V, HashMap<K, V>> mutableMap() {
    return new BiAccumulator<K, V, HashMap<K, V>>() {
      HashMap<K, V> map = new HashMap<>();
      @Override
      public void accept(K k, V v) {
        map.put(k, v);
      }

      @Override
      public HashMap<K, V> get() {
        return map;
      }
    };
  }

  class ImmutableMapAccumulator<K, V> implements BiAccumulator<K, V, ImmutableMap<K, V>> {
        private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

        @Override
        public void accept(K k, V v) {
            builder.put(k, v);
        }

        @Override
        public ImmutableMap<K, V> get() {
            return builder.build();
        }
    }
}
