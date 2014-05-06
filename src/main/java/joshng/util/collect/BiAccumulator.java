package joshng.util.collect;

import joshng.util.blocks.Unzipper;

import java.util.Map;
import java.util.function.BiConsumer;

import static joshng.util.collect.Functional.funPairs;

/**
* User: josh
* Date: 4/30/14
* Time: 10:47 AM
*/
public interface BiAccumulator<K, V, O> extends Accumulator<Map.Entry<? extends K, ? extends V>, O>, BiConsumer<K, V> {
    default O accumulate2(Iterable<? extends Map.Entry<K, V>> pairs) {
      return funPairs(pairs).accumulate2(this);
    }

    @Override default void accept(Map.Entry<? extends K, ? extends V> entry) {
        accept(entry.getKey(), entry.getValue());
    }

    default <I> Accumulator<I, O> compose2(Unzipper<? super I, ? extends K, ? extends V> unzipper) {
      return new Accumulator<I, O>() {
        @Override
        public void accept(I i) {
          BiAccumulator.this.accept(unzipper.getKey(i), unzipper.getValue(i));
        }

        @Override
        public O get() {
          return BiAccumulator.this.get();
        }
      };
    }
}
