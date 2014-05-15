package joshng.util.collect;

import com.google.common.collect.ForwardingMap;

import java.util.Map;

import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 4/24/12
 * Time: 9:12 PM
 */
public abstract class FunForwardingMap<K, V> extends ForwardingMap<K, V> {
  @Override
  protected abstract Map<K, V> delegate();

  public FunIterable<K> funKeys() {
    return extend(keySet());
  }

  public FunIterable<V> funValues() {
    return extend(values());
  }

  public FunPairs<K, V> funPairs() {
    return Functional.funPairs(delegate());
  }
}
