package com.joshng.util.concurrent;

import com.google.common.base.Objects;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import com.joshng.util.collect.ForwardingMaybe;
import com.joshng.util.collect.Maybe;
import com.joshng.util.collect.MutableReference;
import com.joshng.util.context.TransientContext;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: josh
 * Date: 7/25/14
 * Time: 10:22 AM
 */
public class AsyncContext<T> implements ForwardingMaybe<T>, MutableReference<T> {
  private static final ThreadLocalRef<TLongObjectMap<Object>> CURRENT_VALUES = new ThreadLocalRef<>();
  private static final AtomicLong IDENTITY = new AtomicLong();

  private final long key = IDENTITY.getAndIncrement();
  private final T initialValue;

  public static <T> AsyncContext<T> newAsyncContext() {
    return new AsyncContext<>();
  }

  public AsyncContext() {
    this(null);
  }

  public AsyncContext(T initialValue) {
    this.initialValue = initialValue;
  }

  public static TransientContext snapshot() {
    TLongObjectMap<Object> values = CURRENT_VALUES.get();
    if (values == null) return TransientContext.NULL;
    return CURRENT_VALUES.contextWithValue(new TLongObjectHashMap<>(values));
  }

  @Override public void set(T value) {
    doSet(value, CURRENT_VALUES.get());
  }

  private void doSet(T value, TLongObjectMap<Object> values) {
    if (value == null) {
      doRemove(values);
    } else {
      setNonNull(value, values);
    }
  }

  @SuppressWarnings("unchecked") @Override public T get() {
    TLongObjectMap<Object> values = CURRENT_VALUES.get();
    if (values != null) {
      T value = (T) values.get(key);
      if (value != null) return value;
    }
    return initialValue();
  }

  protected T initialValue() {
    return initialValue;
  }

  public void remove() {
    doRemove(CURRENT_VALUES.get());
  }

  private void doRemove(TLongObjectMap<Object> values) {
    if (values != null) values.remove(key);
  }

  private void setNonNull(T value, TLongObjectMap<Object> values) {
    if (values == null) {
      values = new TLongObjectHashMap<>();
      CURRENT_VALUES.set(values);
    }
    values.put(key, value);
  }

  @Override public Maybe<T> getMaybe() {
    return MutableReference.super.getMaybe();
  }

  @Override public boolean compareAndSet(T expect, T update) {
    TLongObjectMap<Object> values = CURRENT_VALUES.get();
    boolean matched;
    if (values != null) {
      Object actual = values.get(key);
      matched = Objects.equal(expect, actual);
      if (matched) {
        if (update != null) {
          values.put(key, update);
        } else {
          values.remove(key);
        }
      }
    } else {
      matched = expect == null;
      if (matched) {
        if (update != null) {
          initialize(update);
        }
      }
    }
    return matched;
  }

  private void initialize(@Nonnull T update) {
    TLongObjectHashMap<Object> values = new TLongObjectHashMap<>();
    values.put(key, update);
    CURRENT_VALUES.set(values);
  }

  @SuppressWarnings("unchecked") @Override public T getAndSet(T value) {
    TLongObjectMap<Object> values = CURRENT_VALUES.get();
    T prev;
    if (values == null) {
      prev = null;
      if (value != null) initialize(value);
    } else if (value != null) {
      prev = (T) values.put(key, value);
    } else {
      prev = (T) values.remove(key);
    }
    return prev;
  }

  //TODO
//  @Override public T modify(Function<? super T, ? extends T> transformer) {
//
//  }
}
