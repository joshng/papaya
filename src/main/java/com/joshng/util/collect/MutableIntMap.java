package com.joshng.util.collect;

import com.google.common.collect.Maps;
import com.joshng.util.blocks.F;

import java.util.Map;

import static com.joshng.util.collect.Functional.funPairs;

/**
 * User: josh
 * Date: 4/29/13
 * Time: 12:54 PM
 */
public class MutableIntMap<T> extends ComputingMap<T, MutableInt> {
  public static final F<MutableInt, Integer> INT_VALUE = new F<MutableInt, Integer>() {
    public Integer apply(MutableInt input) {
      return input.intValue();
    }
  };

  public static <T> MutableIntMap<T> newIntMap() {
    return new MutableIntMap<T>();
  }

  public Map<T, Integer> intView() {
    return Maps.transformValues(this, INT_VALUE);
  }

  public FunPairs<T, Integer> pairs() {
    return funPairs(this).mapValues(INT_VALUE);
  }

  public int add(T key, int addend) {
    MutableInt mutableInt = get(key);
    mutableInt.add(addend);
    return mutableInt.intValue();
  }

  public int increment(T key) {
    return add(key, 1);
  }

  /**
   * sets the value associated with the given key
   *
   * @return the previous value, or 0 if the key was not present
   */
  public int putInt(T key, int newValue) {
    MutableInt mutableInt = get(key);
    int oldValue = mutableInt.intValue();
    mutableInt.setValue(newValue);
    return oldValue;
  }

  /**
   * gets the value associated with the given key, <em>without</em> creating an entry if the key is not already present
   *
   * @return the int value associated with the given key, or 0 if the key is not present
   */
  public int getInt(T key) {
    MutableInt mutableInt = getIfPresent(key);
    if (mutableInt == null) return 0;
    return mutableInt.intValue();
  }

  @Override
  protected MutableInt computeDefaultValue(T key) {
    return new MutableInt();
  }
}
