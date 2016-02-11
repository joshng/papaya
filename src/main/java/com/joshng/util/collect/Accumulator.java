package com.joshng.util.collect;

import com.google.common.collect.*;
import com.joshng.util.blocks.Sink;
import com.joshng.util.blocks.Source;

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
  static <I, A, O> Accumulator<I, O> fromCollector(Collector<I, A, O> collector) {
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

  static <I, O> Accumulator<I, O> into(Consumer<? super I> adder, O accumulation) {
    return of(adder, Source.ofInstance(accumulation));
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
    return into(list::add, list);
  }

  static <T, S extends Set<T>> Accumulator<T, S> toSet(S set) {
    return into(set::add, set);

  }


  static <T> Accumulator<T, ImmutableSet<T>> immutableSet() {
    ImmutableSet.Builder<T> builder = ImmutableSet.builder();
    return of(builder::add, builder::build);
  }

  static <T> Accumulator<T, Multiset<T>> hashMultiset() {
    Multiset<T> multiset = HashMultiset.create();
    return into(multiset::add, multiset);
  }

  static <K, V> BiAccumulator<K, V, HashMap<K, V>> hashMap() {
    return toMap(new HashMap<>());
  }

  static <K, V> BiAccumulator<K, V, LinkedHashMap<K, V>> linkedHashMap() {
    return toMap(new LinkedHashMap<>());
  }

  static <K, V> BiAccumulator<K, V, TreeMap<K, V>> treeMap() {
    return toMap(new TreeMap<>());
  }

  static <E extends Enum<E>, V> BiAccumulator<E, V, EnumMap<E, V>> enumMap(Class<E> enumClass) {
    return toMap(Maps.newEnumMap(enumClass));
  }

  static <K, V, M extends Map<K, V>> BiAccumulator<K, V, M> toMap(M map) {
    return new MutableMapAccumulator<>(map);
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
