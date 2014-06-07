package joshng.util.collect;

import com.google.common.base.Objects;
import joshng.util.blocks.Source;

/**
 * User: josh
 * Date: Feb 16, 2011
 * Time: 2:50:24 PM
 */
public class Ref<T> implements Source<T>, MutableReference<T> {
  protected volatile T value;


  public static <T> Ref<T> create() {
    return new Ref<T>(null);
  }

  public static <T> Ref<T> create(T initialValue) {
    return new Ref<T>(initialValue);
  }

  public static <T> Ref<T> of(T initialValue) {
    return new Ref<T>(initialValue);
  }


  public Ref(T initialValue) {
    value = initialValue;
  }

  public T get() {
    return value;
  }

  @Override
  public void set(T value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "Ref{" + String.valueOf(value) + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Ref ref = (Ref) o;

    return Objects.equal(value, ref.value);
  }

  @Override
  public int hashCode() {
    return value != null ? value.hashCode() : 0;
  }
}
