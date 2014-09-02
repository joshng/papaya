package joshng.util.collect;

import com.google.common.collect.*;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;

import java.util.*;
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
    BiConsumer<A, I> accumulator = collector.accumulator();
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

  static <I, O> Accumulator<I, O> of(Consumer<? super I> consumer, Supplier<? extends O> supplier) {
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

  static <T> Accumulator<T, ArrayList<T>> arrayListWithCapacity(int capacity) {
    return toList(new ArrayList<>(capacity));
  }

  static <T> Accumulator<T, ArrayList<T>> arrayList() {
    return toList(new ArrayList<>());
  }

  static <T, L extends List<T>> Accumulator<T, L> toList(L list) {
    return of(list::add, Source.ofInstance(list));
  }

  static <T> Accumulator<T, ImmutableSet<T>> immutableSet() {
    ImmutableSet.Builder<T> builder = ImmutableSet.builder();
    return of(builder::add, builder::build);
  }

  static <T> Accumulator<T, Multiset<T>> hashMultiset() {
    Multiset<T> multiset = HashMultiset.create();
    return of(multiset::add, Source.ofInstance(multiset));
  }

  static <K, V> BiAccumulator<K, V, HashMap<K, V>> hashMap() {
    return new MutableMapAccumulator<>(new HashMap<>());
  }

  static <K, V> BiAccumulator<K, V, LinkedHashMap<K, V>> linkedHashMap() {
    return new MutableMapAccumulator<>(new LinkedHashMap<>());
  }

  static <K, V> BiAccumulator<K, V, TreeMap<K, V>> treeMap() {
    return new MutableMapAccumulator<>(new TreeMap<>());
  }

  static <K, V> BiAccumulator<K, V, ImmutableMap<K, V>> immutableMap() {
    return new ImmutableMapAccumulator<>();
  }

  static <K, V> BiAccumulator<K, V, ImmutableBiMap<K, V>> biMap() {
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


  default O accumulate(Iterable<? extends I> items) {
    for (I item : items) {
      accept(item);
    }
    return get();
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

  class MutableMapAccumulator<K, V, M extends Map<K, V>> implements BiAccumulator<K, V, M> {
    private final M map;

    public MutableMapAccumulator(M map) {
      this.map = map;
    }

    @Override
    public void accept(K k, V v) {
      map.put(k, v);
    }

    @Override
    public M get() {
      return map;
    }
  }
}
