package joshng.util.collect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import joshng.util.blocks.Source;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * User: josh
 * Date: 4/26/14
 * Time: 11:57 AM
 */
public interface Accumulator<I, O> extends Consumer<I>, Source<O> {
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

    interface BiAccumulator<T, U, V> extends Accumulator<Map.Entry<? extends T, ? extends U>, V>, BiConsumer<T, U> {
        @Override default void accept(Map.Entry<? extends T, ? extends U> entry) {
            accept(entry.getKey(), entry.getValue());
        }
    }

    class ImmutableMapAccumulator<K, V> implements BiAccumulator<K, V, ImmutableMap<K, V>> {
        private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

        @Override
        public void accept(Map.Entry<? extends K, ? extends V> i) {
            builder.put(i);
        }

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
