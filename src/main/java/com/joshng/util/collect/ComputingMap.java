package com.joshng.util.collect;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

/**
 * User: josh
 * Date: Aug 4, 2011
 * Time: 5:40:27 PM
 *
 * @deprecated with java 8, just use {@link Map#computeIfAbsent}
 */
@Deprecated
public abstract class ComputingMap<K, V> extends ForwardingMap<K, V> implements Function<K, V> {
  private final Map<K, V> delegate;

  public static <K, V> ComputingMap<K, V> newHashMap(Function<? super K, ? extends V> defaultValueComputer) {
    return over(Maps.<K, V>newHashMap(), defaultValueComputer);
  }

  public static <K, V> ComputingMap<K, V> over(Map<K, V> realMap, Function<? super K, ? extends V> defaultValueComputer) {
    return new FromFunction<K, V>(realMap, defaultValueComputer);
  }

  public static <K, V> Wrapper<K, V> wrapper(Function<? super K, ? extends V> defaultValueComputer) {
    return new Wrapper<K, V>(defaultValueComputer);
  }

  public static <K, V> ComputingMap<K, V> of(Class<V> defaultValueClass) {
    return new FromDefaultConstructor<K, V>(defaultValueClass);
  }

  public static <K, V> ComputingMap<K, V> of(Class<K> constructorParameterClass, Class<V> valueClass) {
    return newHashMap(Generator.on(constructorParameterClass, valueClass));
  }

  public ComputingMap(Map<K, V> realMap) {
    this.delegate = realMap;
  }

  public ComputingMap() {
    this(Maps.<K, V>newHashMap());
  }

  @Override
  protected Map<K, V> delegate() {
    return delegate;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public V get(Object k) {
    V value = getIfPresent(k);
    if (value == null) {
      K key = (K) k;
      value = computeDefaultValue(key);
      put(key, value);
    }
    return value;
  }

  public V get(K k, Function<? super K, ? extends V> defaultValueComputer) {
    V value = getIfPresent(k);
    if (value == null) {
      value = defaultValueComputer.apply(k);
      put(k, value);
    }
    return value;
  }

  @Nullable
  public V getIfPresent(Object k) {
    return super.get(k);
  }

  public Maybe<V> getMaybe(K k) {
    return Maybe.of(getIfPresent(k));
  }

  @Override
  public V apply(K input) {
    return get(input);
  }

  protected abstract V computeDefaultValue(K key);

  public static class Wrapper<K, V> {
    private final Function<? super K, ? extends V> defaultValueFactory;

    public Wrapper(Function<? super K, ? extends V> defaultValueFactory) {
      this.defaultValueFactory = defaultValueFactory;
    }

    public V get(K key, Map<K, V> realMap) {
      V value = realMap.get(key);
      if (value == null) {
        value = defaultValueFactory.apply(key);
        realMap.put(key, value);
      }
      return value;
    }

    public ComputingMap<K, V> wrap(Map<K, V> realMap) {
      return over(realMap, defaultValueFactory);
    }
  }

  public static class FromFunction<K, V> extends ComputingMap<K, V> {
    private final Function<? super K, ? extends V> defaultValueFactory;

    FromFunction(Map<K, V> realMap, Function<? super K, ? extends V> defaultValueFactory) {
      super(realMap);
      this.defaultValueFactory = defaultValueFactory;
    }

    FromFunction(Function<? super K, ? extends V> defaultValueFactory) {
      this(Maps.<K, V>newHashMap(), defaultValueFactory);
    }

    protected V computeDefaultValue(K key) {
      return defaultValueFactory.apply(key);
    }
  }


  public static class FromConstructor<K, V> extends FromFunction<K, V> {
    FromConstructor(Map<K, V> realMap, Class<K> keyClass, Class<V> valueClass) {
      super(realMap, Generator.on(keyClass, valueClass));
    }

    FromConstructor(Class<K> keyClass, Class<V> valueClass) {
      this(Maps.<K, V>newHashMap(), keyClass, valueClass);
    }
  }

  public static class FromDefaultConstructor<K, V> extends FromFunction<K, V> {
    public FromDefaultConstructor(Map<K, V> realMap, Class<V> defaultValueClass) {
      super(realMap, Factory.of(defaultValueClass));
    }

    public FromDefaultConstructor(Class<V> defaultValueClass) {
      this(Maps.<K, V>newHashMap(), defaultValueClass);
    }
  }
}
