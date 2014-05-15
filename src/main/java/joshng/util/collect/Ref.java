package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.blocks.Source;
import joshng.util.context.StackContext;

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

  public static <T> StackContext contextWithValue(final T value, final MutableReference<T> ref) {
    return new RefValueStackContext<T>(value, ref);
  }

  public static <T> boolean nonAtomicCompareAndSet(MutableReference<T> ref, T expect, T update) {
    boolean matched = Objects.equal(ref.get(), expect);
    if (matched) ref.set(update);
    return matched;
  }

  public static <T> T nonAtomicGetAndSet(MutableReference<T> ref, T value) {
    T prev = ref.get();
    ref.set(value);
    return prev;
  }

  public static <T> T modify(MutableReference<T> ref, Function<? super T, ? extends T> transformer) {
    T currentValue;
    T newValue;
    do {
      currentValue = ref.get();
      newValue = transformer.apply(currentValue);
    } while (!ref.compareAndSet(currentValue, newValue));
    return newValue;
  }

  public static <I, T> F<I, T> modifier(final MutableReference<T> ref, final F2<? super I, ? super T, ? extends T> reducer) {
    return new F<I, T>() {
      public T apply(I input) {
        return modify(ref, reducer.bindFirst(input));
      }
    };
  }

  public Ref(T initialValue) {
    value = initialValue;
  }

  @Override
  public boolean compareAndSet(T expect, T update) {
    return nonAtomicCompareAndSet(this, expect, update);
  }

  public T get() {
    return value;
  }

  @Override
  public void set(T value) {
    this.value = value;
  }

  @Override
  public T getAndSet(T value) {
    return nonAtomicGetAndSet(this, value);
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

  private static class RefValueStackContext<T> extends StackContext {
    private final T value;
    private final MutableReference<T> ref;

    public RefValueStackContext(T value, MutableReference<T> ref) {
      this.value = value;
      this.ref = ref;
    }

    public State enter() {
      final T existingValue = ref.getAndSet(value);

      return new State() {
        public void exit() {
          ref.set(existingValue);
        }
      };
    }
  }
}
