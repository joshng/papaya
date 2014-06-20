package joshng.util.collect;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.reflect.TypeToken;
import joshng.util.Reflect;
import joshng.util.StringUtils;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Sink;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.AsyncMaybeF;
import joshng.util.concurrent.FunFutureMaybe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: josh
 * Date: Aug 15, 2011
 * Time: 10:49:42 AM
 */
public abstract class Maybe<T> implements Iterable<T> {
  private static final Pair NOT = new Not();

  public static final Pred<Maybe<?>> IS_DEFINED = Maybe::isDefined;
  private static final F GET_OR_THROW = new F<Maybe, Object>() {
    public Object apply(Maybe from) {
      return from.getOrThrow();
    }
  };
  private static final F FLATTENER = new F<Iterable<Maybe<Object>>, FunIterable<Object>>() {
    public FunIterable<Object> apply(Iterable<Maybe<Object>> input) {
      return Maybe.<Object>flatten(input);
    }
  };

  public static <T> F<T, Maybe<T>> of() {
    return new F<T, Maybe<T>>() {
      public Maybe<T> apply(T t) {
        return Maybe.of(t);
      }
    };
  }

  public static <T> F<T, Maybe<T>> definitely() {
    return new F<T, Maybe<T>>() {
      @Override
      public Maybe<T> apply(T input) {
        return definitely(input);
      }
    };
  }

//    public static <I, O> Maybe<O> apply(Maybe<? extends Function<? super I, ? extends O>> f, final I input) {
//        return apply(f, Maybe.definitely(input));
//    }

  public static <I, O> Maybe<O> apply(Maybe<? extends Function<? super I, ? extends O>> f, final Maybe<? extends I> input) {
    return f.flatMap(new Function<Function<? super I, ? extends O>, Maybe<O>>() {
      public Maybe<O> apply(Function<? super I, ? extends O> from) {
        return input.map(from);
      }
    });
  }

  public static Maybe<String> filterNullOrEmpty(String value) {
    return exceptIf(Strings.isNullOrEmpty(value), value);
  }

  public static <T> Ordering<Maybe<? extends T>> orderingUndefinedLast(final Comparator<T> definedOrdering) {
    return new Ordering<Maybe<? extends T>>() {
      public int compare(Maybe<? extends T> left, Maybe<? extends T> right) {
        if (left == right) return 0;
        if (!left.isDefined()) return 1;
        if (!right.isDefined()) return -1;
        return definedOrdering.compare(left.getOrThrow(), right.getOrThrow());
      }
    };
  }

  @Nonnull
  public static <T> Maybe<T> nullToMaybeNot(@Nullable Maybe<T> nullOrMaybe) {
    return nullOrMaybe != null ? nullOrMaybe : Maybe.<T>not();
  }

  public static <T> Maybe<T> onlyIf(boolean condition, T value) {
    return condition ? definitely(value) : Maybe.<T>not();
  }

  public static <T> Maybe<T> exceptIf(boolean condition, T value) {
    return onlyIf(!condition, value);
  }

  public static <T> Maybe<T> onlyIfFrom(boolean condition, Supplier<T> value) {
    return condition ? definitely(value.get()) : Maybe.<T>not();
  }

  public abstract boolean isDefined();

  public abstract boolean isEmpty();

  public abstract T getOrThrow() throws NoSuchElementException;

  public abstract T getOrThrow(String format, Object... args) throws NoSuchElementException;

  public abstract <E extends Throwable> T getOrThrow(E throwable) throws E;

  public abstract <E extends Throwable> T getOrThrowFrom(Supplier<E> throwableSupplier) throws E;

  @Nullable
  public abstract T orNull();

  public abstract T getOrElse(T alternateValue);

  public abstract T getOrElseFrom(Supplier<? extends T> alternateValueSupplier);

  public abstract Maybe<T> orElse(Maybe<T> alternateValue);

  public abstract Maybe<T> orElseFrom(Supplier<? extends Maybe<? extends T>> alternateValueSupplier);

  public abstract <U> Maybe<U> map(Function<? super T, ? extends U> transformer);

  public abstract <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> transformer);

  public abstract <O> O map(MaybeFunction<? super T, O> transformer);

  public <O> FunFutureMaybe<O> mapFuture(AsyncF<? super T, O> async) {
    return FunFutureMaybe.asFutureMaybe(map(async));
  }

  public <O> FunFutureMaybe<O> flatMapFuture(AsyncF<? super T, Maybe<O>> async) {
    return isDefined() ? AsyncMaybeF.extendMaybeF(async).apply(getOrThrow()) : FunFutureMaybe.futureMaybeNot();
  }

  public abstract <K, V> Pair<K, V> mapPair(Function<? super T, ? extends Map.Entry<K, V>> pairComputer);

  public abstract <K, V> Pair<K, V> flatMapPair(Function<? super T, ? extends Pair<K, V>> pairComputer);

  public abstract Maybe<T> foreach(Consumer<? super T> handler);

  public abstract Maybe<T> orElseRun(Runnable runnable);

  public abstract Maybe<T> filter(Predicate<? super T> filter);

  public abstract Maybe<T> filterNot(Predicate<? super T> filter);

  public abstract <V> Pair<T, V> asKeyTo(Function<? super T, ? extends V> valueComputer);

  public abstract <K> Pair<K, T> asValueFrom(Function<? super T, ? extends K> keyComputer);

  /**
   * @returns Maybe.Not unless the value is an instance of castClass
   */
  public abstract <U> Maybe<U> filter(Class<U> castClass);

  /**
   * @throws ClassCastException if the value is defined, but is not an instance of the requested castClass
   * @returns if isDefined(), a casted Maybe; otherwise, Maybe.Not
   */
  public abstract <U> Maybe<U> cast(Class<U> castClass);

  @SuppressWarnings("unchecked")
  /**
   * Returns true if this Maybe is defined (has a value) and the value matches the given predicate.
   * Returns false otherwise (ie, if this isEmpty, or if the value does not match the predicate).
   * <br/><br/>
   *
   * To illustrate, given these variables...
   * <pre>{@code
   *   Maybe<T> maybe = something();
   *
   *   Predicate<T> myPredicate = ...;
   * }</pre>
   *
   * ... the following two lines are equivalent:
   *
   * <pre>{@code
   *   boolean matches = maybe.map(Functions.forPredicate(myPredicate)).getOrElse(false);
   *       // -- same as -- //
   *   boolean matches = maybe.valueMatches(myPredicate);
   * }</pre>
   */
  public abstract boolean valueMatches(Predicate<? super T> predicate);

  public boolean valueMatchesNot(Predicate<? super T> predicate) {
    return valueMatches(predicate.negate());
  }

  private Maybe() {
  }

  public static <T> FunIterable<T> flatten(Iterable<? extends Maybe<? extends T>> maybes) {
    // this way is a bit more efficient than using concat:
    return FunIterable.filter(maybes, IS_DEFINED).map(Maybe.<T>getter());
  }

  @SuppressWarnings("unchecked")
  public static <T> F<Iterable<? extends Maybe<T>>, FunIterable<T>> flattener() {
    return FLATTENER;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> F<Maybe<? extends T>, T> getter() {
    return GET_OR_THROW;
  }

  public static <T> F<Maybe<T>, T> getter(String format, Object... args) {
    return m -> m.getOrThrow(format, args);
  }

  public static <T> F<Maybe<T>, T> getterWithDefault(final T alternateValue) {
    return input -> input.getOrElse(alternateValue);
  }

  public static <T> F<Maybe<T>, T> getterWithDefaultFrom(Supplier<? extends T> alternateValueSupplier) {
    return input -> input.getOrElseFrom(alternateValueSupplier);
  }

  public static <T> Maybe<T> from(Supplier<T> supplier) {
    try {
      return of(supplier.get());
    } catch (NoSuchElementException e) {
      return not();
    }
  }

  @SuppressWarnings("unchecked")
  public static <U> F<Maybe<?>, Maybe<U>> caster(TypeToken<U> typeLiteral) {
    return caster((Class<U>) typeLiteral.getRawType());
  }

  public static <U> F<Maybe<?>, Maybe<U>> caster(final Class<U> castClass) {
    return new F<Maybe<?>, Maybe<U>>() {
      @Override
      public Maybe<U> apply(Maybe<?> input) {
        return input.cast(castClass);
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static <T> Maybe<T> definitely(@Nonnull T value) {
    return Reflect.blindCast(value instanceof Map.Entry ? definitely((Map.Entry) value) : new Definitely<>(value));
  }

  public static <K, V> Pair<K, V> definitely(@Nonnull Map.Entry<? extends K, ? extends V> entry) {
    return new Pair.DefinitePair<K, V>(entry);
  }

  public static <K, V> Pair<K, V> definitePair(K key, V value) {
    return new Pair.DefinitePair<K, V>(joshng.util.collect.Pair.of(key, value));
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Pair<K, V> noPair() {
    return NOT;
  }

  public static <T> Maybe<T> maybe(@Nullable T value) {
    return value != null ? definitely(value) : Maybe.<T>not();
  }

  @SuppressWarnings("unchecked")
  public static <T> Maybe<T> of(@Nullable T value) {
    if (value == null) return NOT;
    return definitely(value);
  }

  public static <K, V> Pair<K, V> of(@Nullable Map.Entry<? extends K, ? extends V> entry) {
    if (entry == null) return noPair();
    return new Pair.DefinitePair<K, V>(entry);
  }

  public static <T> Maybe<T> asInstance(@Nullable Object value, Class<T> castClass) {
    return castClass.isInstance(value) ? Maybe.definitely(castClass.cast(value)) : Maybe.<T>not();
  }

  public static <T> Maybe<Class<? extends T>> asSubclass(Class<?> maybeSubclass, Class<T> superclass) {
    return Maybe.onlyIfFrom(superclass.isAssignableFrom(maybeSubclass), () -> maybeSubclass.asSubclass(superclass));
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Maybe<T> not() {
    return NOT;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Maybe<T> maybeNot(Class<T> type) {
    return NOT;
  }

  public static <T, U> F<Maybe<T>, Maybe<U>> mapper(final Function<? super T, ? extends U> transformer) {
    return new F<Maybe<T>, Maybe<U>>() {
      public Maybe<U> apply(Maybe<T> maybe) {
        return maybe.map(transformer);
      }
    };
  }

  public static <T, U> F<Maybe<T>, Maybe<U>> flatMapper(final Function<? super T, Maybe<U>> transformer) {
    return new F<Maybe<T>, Maybe<U>>() {
      public Maybe<U> apply(Maybe<T> maybe) {
        return maybe.flatMap(transformer);
      }
    };
  }

  public static <T> F<Maybe<T>, Maybe<T>> filterer(final Predicate<? super T> predicate) {
    return new F<Maybe<T>, Maybe<T>>() {
      @Override
      public Maybe<T> apply(Maybe<T> input) {
        return input.filter(predicate);
      }
    };
  }

  public static <T> Sink<Maybe<? extends T>> foreacher(final Consumer<? super T> sink) {
    return value -> value.foreach(sink);
  }


  public static <T, U, V> F2<Maybe<? extends T>, U, Maybe<V>> firstMapper(final F2<T, U, V> f2) {
    return new F2<Maybe<? extends T>, U, Maybe<V>>() {
      @Override
      public Maybe<V> apply(Maybe<? extends T> input1, U input2) {
        return input1.map(f2.bindSecond(input2));
      }
    };
  }

  /**
   * @param valuePredicate a predicate to apply to the value contained by input-Maybes, if defined
   * @param <T>
   * @return a predicate on {@code Maybe<T>} that returns true if the given {@link Maybe} is defined, AND contains
   * a value which matches the given {@valuePredicate}.
   */
  public static <T> Pred<Maybe<? extends T>> valueMatcher(final Predicate<? super T> valuePredicate) {
    return input -> input.valueMatches(valuePredicate);
  }

  public static <T> Maybe<T> join(Maybe<Maybe<T>> nestedMaybe) {
    return nestedMaybe.getOrElse(Maybe.<T>not());
  }

  private static class Definitely<T> extends Maybe<T> {
    @Nonnull
    protected final T value;

    private Definitely(@Nonnull T value) {
      this.value = checkNotNull(value, "Tried to create Maybe(null); should use Maybe.of() or Maybe.not() instead");
    }

    @Override
    public boolean isDefined() {
      return true;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public T getOrThrow() throws NullPointerException {
      return value;
    }

    @Override
    public T orNull() {
      return value;
    }

    @Override
    public T getOrThrow(String format, Object... args) {
      return value;
    }

    @Override
    public <E extends Throwable> T getOrThrow(E throwable) throws E {
      return value;
    }

    @Override
    public <E extends Throwable> T getOrThrowFrom(Supplier<E> throwable) throws E {
      return value;
    }

    @Override
    public T getOrElse(T alternateValue) {
      return value;
    }

    @Override
    public T getOrElseFrom(Supplier<? extends T> alternateValueSupplier) {
      return value;
    }

    @Override
    public Maybe<T> orElse(Maybe<T> alternateValue) {
      return this;
    }

    @Override
    public Maybe<T> orElseFrom(Supplier<? extends Maybe<? extends T>> alternateValueSupplier) {
      return this;
    }

    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> transformer) {
      return Maybe.of(transformer.apply(value));
    }

    @Override
    public <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> transformer) {
      return transformer.apply(value);
    }

    @Override
    public <O> O map(MaybeFunction<? super T, O> transformer) {
      return transformer.whenDefined(value);
    }

    @Override
    public <K, V> Pair<K, V> mapPair(Function<? super T, ? extends Map.Entry<K, V>> pairComputer) {
      return definitely(pairComputer.apply(value));
    }

    @Override
    public <K, V> Pair<K, V> flatMapPair(Function<? super T, ? extends Pair<K, V>> pairComputer) {
      return pairComputer.apply(value);
    }

    @Override
    public Maybe<T> foreach(Consumer<? super T> handler) {
      handler.accept(value);
      return this;
    }

    @Override
    public Maybe<T> orElseRun(Runnable runnable) {
      return this;
    }

    @Override
    public Maybe<T> filter(Predicate<? super T> filter) {
      return filter.test(value) ? this : Maybe.<T>not();
    }

    @Override
    public Maybe<T> filterNot(Predicate<? super T> filter) {
      return !filter.test(value) ? this : Maybe.<T>not();
    }

    @Override
    public <V> Pair<T, V> asKeyTo(Function<? super T, ? extends V> valueComputer) {
      return definitePair(value, valueComputer.apply(value));
    }

    @Override
    public <K> Pair<K, T> asValueFrom(Function<? super T, ? extends K> keyComputer) {
      return definitePair(keyComputer.apply(value), value);
    }

    @Override
    public boolean valueMatches(Predicate<? super T> predicate) {
      return predicate.test(value);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <U> Maybe<U> filter(Class<U> castClass) {
      return castClass.isInstance(value) ? (Maybe) this : Maybe.<U>not();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <U> Maybe<U> cast(Class<U> castClass) {
      castClass.cast(value);
      return (Maybe) this;
    }

    public Iterator<T> iterator() {
      return Iterators.singletonIterator(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Definitely)) return false;

      Definitely that = (Definitely) o;

      return value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }


  public abstract static class Pair<K, V> extends Maybe<Map.Entry<K, V>> implements Map.Entry<Maybe<K>, Maybe<V>> {

    private Pair() {
    }

    public abstract Maybe<K> getKey();

    public abstract Maybe<V> getValue();

    public abstract <K2> Pair<K2, V> mapKey(Function<? super K, ? extends K2> keyMapper);

    public abstract <V2> Pair<K, V2> mapValue(Function<? super V, ? extends V2> valueMapper);

    public abstract <O> Maybe<O> map2(F2<? super K, ? super V, ? extends O> mapper);

    public abstract <K2, V2> Pair<K2, V2> map2Pair(F2<? super K, ? super V, ? extends Map.Entry<? extends K2, ? extends V2>> mapper);

    @Override
    public abstract Pair<K, V> foreach(Consumer<? super Map.Entry<K, V>> handler);

    @Override
    public abstract Pair<K, V> orElseRun(Runnable runnable);

    @Override
    public abstract Pair<K, V> filter(Predicate<? super Map.Entry<K, V>> filter);

    public abstract Pair<K, V> filterKey(Predicate<? super K> filter);

    public abstract Pair<K, V> filterValue(Predicate<? super V> filter);

    @Override
    public Maybe<V> setValue(Maybe<V> value) {
      throw new UnsupportedOperationException();
    }

    private static class DefinitePair<K, V> extends Pair<K, V> {
      private final Map.Entry<K, V> entry;

      @SuppressWarnings("unchecked")
      private DefinitePair(Map.Entry<? extends K, ? extends V> entry) {
        this.entry = (Map.Entry<K, V>) checkNotNull(entry);
      }

      @Override
      public boolean isDefined() {
        return true;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }

      @Override
      public Maybe<K> getKey() {
        return of(entry.getKey());
      }

      @Override
      public Maybe<V> getValue() {
        return of(entry.getValue());
      }

      @Override
      public <K2> Pair<K2, V> mapKey(Function<? super K, ? extends K2> keyMapper) {
        return definitePair(keyMapper.apply(entry.getKey()), entry.getValue());
      }

      @Override
      public <V2> Pair<K, V2> mapValue(Function<? super V, ? extends V2> valueMapper) {
        return definitePair(entry.getKey(), valueMapper.apply(entry.getValue()));
      }

      @Override
      public <O> Maybe<O> map2(F2<? super K, ? super V, ? extends O> mapper) {
        return of(mapper.apply(entry.getKey(), entry.getValue()));
      }

      @Override
      public <K2, V2> Pair<K2, V2> map2Pair(F2<? super K, ? super V, ? extends Map.Entry<? extends K2, ? extends V2>> mapper) {
        return of(mapper.apply(entry.getKey(), entry.getValue()));
      }

      @Override
      public Map.Entry<K, V> getOrThrow() throws NoSuchElementException {
        return entry;
      }

      @Override
      public Map.Entry<K, V> getOrThrow(String format, Object... args) throws NoSuchElementException {
        return entry;
      }

      @Override
      public <E extends Throwable> Map.Entry<K, V> getOrThrow(E throwable) throws E {
        return entry;
      }

      @Override
      public <E extends Throwable> Map.Entry<K, V> getOrThrowFrom(Supplier<E> throwableSupplier) throws E {
        return entry;
      }

      @Override
      public Map.Entry<K, V> orNull() {
        return entry;
      }

      @Override
      public Map.Entry<K, V> getOrElse(Map.Entry<K, V> alternateValue) {
        return entry;
      }

      @Override
      public Map.Entry<K, V> getOrElseFrom(Supplier<? extends Map.Entry<K, V>> alternateValueSupplier) {
        return entry;
      }

      @Override
      public Pair<K, V> orElse(Maybe<Map.Entry<K, V>> alternateValue) {
        return this;
      }

      @Override
      public Pair<K, V> orElseFrom(Supplier<? extends Maybe<? extends Map.Entry<K, V>>> alternateValueSupplier) {
        return this;
      }

      @Override
      public <U> Maybe<U> map(Function<? super Map.Entry<K, V>, ? extends U> transformer) {
        return definitely(transformer.apply(entry));
      }

      @Override
      public <U> Maybe<U> flatMap(Function<? super Map.Entry<K, V>, Maybe<U>> transformer) {
        return transformer.apply(entry);
      }

      @Override
      public <O> O map(MaybeFunction<? super Map.Entry<K, V>, O> transformer) {
        return transformer.whenDefined(entry);
      }

      @Override
      public <K2, V2> Pair<K2, V2> flatMapPair(Function<? super Map.Entry<K, V>, ? extends Pair<K2, V2>> pairComputer) {
        return pairComputer.apply(entry);
      }

      @Override
      public <K2, V2> Pair<K2, V2> mapPair(Function<? super Map.Entry<K, V>, ? extends Map.Entry<K2, V2>> pairComputer) {
        return definitely(pairComputer.apply(entry));
      }

      @Override
      public Pair<K, V> foreach(Consumer<? super Map.Entry<K, V>> handler) {
        handler.accept(entry);
        return this;
      }

      @Override
      public Pair<K, V> orElseRun(Runnable runnable) {
        return this;
      }

      @Override
      public Pair<K, V> filter(Predicate<? super Map.Entry<K, V>> filter) {
        return filter.test(entry) ? this : Maybe.<K, V>noPair();
      }

      @Override
      public Maybe<Map.Entry<K, V>> filterNot(Predicate<? super Map.Entry<K, V>> filter) {
        return !filter.test(entry) ? this : Maybe.<K, V>noPair();
      }

      @Override
      public Pair<K, V> filterKey(Predicate<? super K> filter) {
        return filter.test(entry.getKey()) ? this : Maybe.<K, V>noPair();
      }

      @Override
      public Pair<K, V> filterValue(Predicate<? super V> filter) {
        return filter.test(entry.getValue()) ? this : Maybe.<K, V>noPair();
      }

      @Override
      public <K2> Pair<K2, Map.Entry<K, V>> asValueFrom(Function<? super Map.Entry<K, V>, ? extends K2> keyComputer) {
        return definitePair(keyComputer.apply(entry), entry);
      }

      @Override
      public <V2> Pair<Map.Entry<K, V>, V2> asKeyTo(Function<? super Map.Entry<K, V>, ? extends V2> valueComputer) {
        return definitePair(entry, valueComputer.apply(entry));
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Maybe<U> filter(Class<U> castClass) {
        return (Maybe<U>) (castClass.isInstance(entry) ? this : noPair());
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Maybe<U> cast(Class<U> castClass) {
        castClass.cast(entry);
        return (Maybe<U>) this;
      }

      @Override
      public boolean valueMatches(Predicate<? super Map.Entry<K, V>> predicate) {
        return predicate.test(entry);
      }

      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return Iterators.singletonIterator(entry);
      }
    }
  }

  private static class Not extends Pair {
    @Override
    public boolean isDefined() {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public Object getOrThrow() throws NoSuchElementException {
      throw new NoSuchElementException("Maybe.Not.getOrThrow");
    }

    @Override
    public Object orNull() {
      return null;
    }

    @Override
    public Object getOrThrow(String format, Object... args) throws NoSuchElementException {
      throw new NoSuchElementException(StringUtils.format(format, args));
    }

    @Override
    public Object getOrThrowFrom(Supplier throwableSupplier) throws Throwable {
      throw (Throwable) throwableSupplier.get();
    }

    @Override
    public Object getOrThrow(Throwable throwable) throws Throwable {
      throw throwable;
    }

    @Override
    public Object getOrElse(Object alternateValue) {
      return alternateValue;
    }

    @Override
    public Object getOrElseFrom(Supplier alternateValueSupplier) {
      return alternateValueSupplier.get();
    }

    @Override
    public Maybe orElse(Maybe alternateValue) {
      return alternateValue;
    }

    @Override
    public Maybe orElseFrom(Supplier alternateValueSupplier) {
      return (Maybe) alternateValueSupplier.get();
    }

    @Override
    public Object map(MaybeFunction transformer) {
      return transformer.whenEmpty();
    }

    @Override
    public Pair mapPair(Function pairComputer) {
      return this;
    }

    @Override
    public Pair flatMapPair(Function pairComputer) {
      return this;
    }

    @Override
    public Pair filter(Predicate filter) {
      return this;
    }

    @Override
    public Maybe filterNot(Predicate filter) {
      return this;
    }

    @Override
    public Pair filterKey(Predicate filter) {
      return this;
    }

    @Override
    public Pair filterValue(Predicate filter) {
      return this;
    }

    @Override
    public Pair asKeyTo(Function valueComputer) {
      return this;
    }

    @Override
    public Pair asValueFrom(Function keyComputer) {
      return this;
    }

    @Override
    public Maybe map(Function transformer) {
      return this;
    }

    @Override
    public Maybe flatMap(Function transformer) {
      return this;
    }

    @Override
    public Maybe getKey() {
      return this;
    }

    @Override
    public Maybe getValue() {
      return this;
    }

    @Override
    public Object setValue(Object value) {
      throw new UnsupportedOperationException("Not.setValue has not been implemented");
    }

    @Override
    public Pair mapKey(Function keyMapper) {
      return this;
    }

    @Override
    public Pair mapValue(Function valueMapper) {
      return this;
    }

    @Override
    public Maybe map2(F2 mapper) {
      return this;
    }

    @Override
    public Pair map2Pair(F2 mapper) {
      return this;
    }

    @Override
    public Pair foreach(Consumer handler) {
      return this;
    }

    @Override
    public Pair orElseRun(Runnable runnable) {
      runnable.run();
      return this;
    }

    @Override
    public boolean valueMatches(Predicate objectPredicate) {
      return false;
    }

    @Override
    public Maybe cast(Class castClass) {
      return this;
    }

    @Override
    public Maybe filter(Class castClass) {
      return not();
    }

    @Override
    public Iterator iterator() {
      return Iterators.emptyIterator();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object obj) {
      return this == obj; // hooray for singletons!
    }

    @Override
    public String toString() {
      return "{undefined}";
    }
  }
}
