package com.google.common.collect;

/**
 * User: josh
 * Date: 3/18/12
 * Time: 6:03 PM
 */

/**
 * Guava's {@link com.google.common.collect.ImmutableEntry} class is package-local, so we
 * need this public subclass inside the package to enable further subclasses outside
 * of the package.<p>
 * <p>
 * Subclassing their ImmutableEntry is desirable to reduce the number of copies involved
 * in creating ImmutableMaps.
 */
public class PublicImmutableEntry<K, V> extends ImmutableEntry<K, V> {
  public PublicImmutableEntry(K key, V value) {
    super(key, value);
  }
}
