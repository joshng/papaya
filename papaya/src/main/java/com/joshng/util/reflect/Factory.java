package com.joshng.util.reflect;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import com.joshng.util.blocks.Source;

/**
 * User: josh
 * Date: Sep 4, 2010
 * Time: 1:13:48 PM
 */
public class Factory<T> implements Source<T> {
  private final Class<T> instanceClass;

  private static LoadingCache<Class, Factory> cache = CacheBuilder.newBuilder()
          .weakKeys()
          .softValues()
          .build(CacheLoader.from(Generator.on(Class.class, Factory.class)));

  @SuppressWarnings({"unchecked"})
  public static <T> Factory<T> of(Class<? extends T> instanceClass) {
    return cache.getUnchecked(instanceClass);
  }

  public static <T> Factory<T> of(TypeToken<? extends T> type) {
    return of(Reflect.<Class<T>>blindCast(type.getRawType()));
  }

  public static Function<Class, Factory> generator() {
    return cache;
  }

  private Factory(Class<T> instanceClass) {
    try {
      instanceClass.getConstructor().setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw Throwables.propagate(e);
    }
    this.instanceClass = instanceClass;
  }

  public T get() {
    try {
      return instanceClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
