package joshng.util.concurrent;

import com.google.common.base.Objects;
import com.google.common.collect.ForwardingMap;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

/**
 * User: josh
 * Date: 7/31/14
 * Time: 8:08 AM
 */
public class AtomicMap<K, V> extends ForwardingMap<K,V> implements ConcurrentMap<K, V> {
  private static final AtomicReferenceFieldUpdater<AtomicMap, PMap> storageUpdater
          = AtomicReferenceFieldUpdater.newUpdater(AtomicMap.class, PMap.class, "storage");
  private volatile PMap<K, V> storage = HashTreePMap.empty();

  public static <K, V> AtomicMap<K, V> newAtomicMap() {
    return new AtomicMap<>();
  }

  @Override protected Map<K, V> delegate() {
    return storage;
  }

  public V put(K key, V value) {
    return (V) storageUpdater.getAndUpdate(this, s -> s.plus(key, value)).get(key);
  }

  @Override public void putAll(Map<? extends K, ? extends V> values) {
    if (!values.isEmpty()) {
      storageUpdater.getAndUpdate(this, s -> s.plusAll(values));
    }
  }

  @Override public V putIfAbsent(K key, V value) {
    PMap<K, V> currentStorage = storage;
    V currentValue;
    while ((currentValue = currentStorage.get(key)) == null && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.plus(key, value))) {
      currentStorage = storage;
    }
    return currentValue;
  }

  @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    PMap<K, V> currentStorage = storage;
    V value;
    while ((value = currentStorage.get(key)) == null
            && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.plus(key, value = mappingFunction.apply(key)))) {
      currentStorage = storage;
    }
    return value;

  }

  @Override public V remove(Object key) {
    K k = (K) key;
    V removed;
    PMap<K, V> currentStorage;
    do {
      currentStorage = storage;
      removed = currentStorage.get(k);
    } while (removed != null && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.minus(k)));
    return removed;
  }

  @Override public boolean remove(Object key, Object value) {
    PMap<K, V> currentStorage;
    boolean matched;
    do {
      currentStorage = storage;
    } while ((matched = Objects.equal(currentStorage.get(key), value))
            && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.minus(key)));
    return matched;
  }

  @Override public boolean replace(K key, V oldValue, V newValue) {
    PMap<K, V> currentStorage;
    boolean matched;
    do {
      currentStorage = storage;
    } while ((matched = Objects.equal(currentStorage.get(key), oldValue))
            && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.plus(key, newValue)));
    return matched;
  }

  @Override public V replace(K key, V value) {
    PMap<K, V> currentStorage;
    V currentValue;
    do {
      currentStorage = storage;
    } while ((currentValue = currentStorage.get(key)) != null && !storageUpdater.compareAndSet(this, currentStorage, currentStorage.plus(key, value)));
    return currentValue;
  }
}
