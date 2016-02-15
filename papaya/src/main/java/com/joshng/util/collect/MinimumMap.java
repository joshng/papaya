package com.joshng.util.collect;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.joshng.util.blocks.Comparison;

import java.util.Comparator;
import java.util.Map;

/**
 * User: josh
 * Date: 2/10/12
 * Time: 8:55 AM
 */
public class MinimumMap<K, V> extends FunForwardingMap<K, V> {
  private final Map<K, V> values;
  private final Comparator<? super V> comparator;

  public static <K, V extends Comparable> MinimumMap<K, V> withNaturalOrder(Iterable<? extends Entry<K, V>> entries) {
    return build(Ordering.<V>natural(), entries);
  }

  public static <K, V> MinimumMap<K, V> build(Comparator<? super V> comparator, Iterable<? extends Entry<K, V>> entries) {
    MinimumMap<K, V> result = new MinimumMap<K, V>(Maps.<K, V>newHashMap(), comparator);
    for (Entry<K, V> entry : entries) {
      result.putEntryIfMinimum(entry);
    }
    return result;
  }

  public MinimumMap(Map<K, V> values, Comparator<? super V> comparator) {
    this.values = values;
    this.comparator = comparator;
  }

  @Override
  protected Map<K, V> delegate() {
    return values;
  }

  public V putEntryIfMinimum(Map.Entry<? extends K, ? extends V> newEntry) {
    return putIfMinimum(newEntry.getKey(), newEntry.getValue());
  }

  public V putIfMinimum(K key, V value) {
    V min = get(key);
    if (min == null || Comparison.Less.compare(value, min, comparator)) {
      min = value;
      put(key, value);
    }
    return min;
  }
}
