package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.context.TransientContext;

import java.util.function.Supplier;

/**
 * User: josh
 * Date: 2/22/13
 * Time: 1:53 PM
 */
public interface MutableReference<T> extends Supplier<T> {
  void set(T value);

  default boolean compareAndSet(T expect, T update) {
    boolean matched = Objects.equal(get(), expect);
    if (matched) set(update);
    return matched;
  }

  default T getAndSet(T value) {
    T prev = get();
    set(value);
    return prev;
  }

  default Maybe<T> getMaybe() {
    return Maybe.of(get());
  }

  default T computeIfAbsent(Supplier<? extends T> defaultComputer) {
    T value;
    do {
      value = get();
    } while (value == null && !compareAndSet(null, value = defaultComputer.get()));
    return value;
  }

  default T modify(Function<? super T, ? extends T> transformer) {
    T currentValue;
    T newValue;
    do {
      currentValue = get();
      newValue = transformer.apply(currentValue);
    } while (!compareAndSet(currentValue, newValue));
    return newValue;
  }

  default <I> F<I, T> modifier(final F2<? super I, ? super T, ? extends T> reducer) {
    return input -> modify(reducer.bindFirst(input));
  }

  default TransientContext contextWithValue(T value) {
    return () -> {
      T existingValue = getAndSet(value);
      return () -> set(existingValue);
    };
  }
}
