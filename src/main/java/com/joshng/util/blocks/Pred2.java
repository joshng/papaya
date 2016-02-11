package com.joshng.util.blocks;

import com.google.common.base.Objects;

import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: 3/16/12
 * Time: 9:04 AM
 */
public interface Pred2<K, V> extends Pred<Entry<? extends K, ? extends V>>, BiPredicate<K,V> {
  public static final Pred2<Object, Object> EQUAL_PARAMS = new Pred2<Object, Object>() {
    public boolean test(Object key, Object value) {
      return Objects.equal(key, value);
    }
  };

  public static <K,V> Pred2<K,V> pred2(Pred2<K,V> pred) {
    return pred;
  }

  public static <K,V> Pred2<K,V> extendBiPredicate(BiPredicate<K,V> pred) {
    return pred instanceof Pred2 ? (Pred2<K, V>) pred : pred::test;
  }

  public static <K> Pred2<K, Object> ignoringSecond(Predicate<? super K> firstPredicate) {
    return (k, v) -> firstPredicate.test(k);
  }

  public static <V> Pred2<Object, V> ignoringFirst(Predicate<? super V> firstPredicate) {
    return (k, v) -> firstPredicate.test(v);
  }

  default boolean test(Entry<? extends K, ? extends V> input) {
    return test(input.getKey(), input.getValue());
  }

  boolean test(K key, V value);

  default Pred2<V, K> flip() {
    return new Pred2<V, K>() {
      public boolean test(V key, K value) {
        return Pred2.this.test(value, key);
      }
    };
  }

  default Pred<V> bindFirst(final K key) {
    return new Pred<V>() {
      public boolean test(V input) {
        return Pred2.this.test(key, input);
      }
    };
  }

  default Pred<K> bindSecond(final V value) {
    return new Pred<K>() {
      public boolean test(K input) {
        return Pred2.this.test(input, value);
      }
    };
  }

  default Pred2<K, V> negate() {
    return (key, value) -> !test(key, value);
  }
}
