package com.joshng.util.collect;

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.joshng.util.blocks.Pred;
import com.joshng.util.blocks.Pred2;
import com.joshng.util.reflect.Reflect;
import com.joshng.util.blocks.F;
import com.joshng.util.blocks.F2;
import com.joshng.util.blocks.Unzipper;
import com.joshng.util.concurrent.LazyReference;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 4:29:32 PM
 */

/**
 * <p>
 * A wrapper around an Iterable that offers higher-order functions (eg, {@link #map}(..), {@link #filter}(..), etc).</p><p>
 * <p>
 * A typical interaction will involve:
 * <ol>
 * <li>obtaining a FunIterable by passing an existing Iterable or array to {@link Functional#extend}</li>
 * <li>"chaining" together a program of {@link #map}s, {@link #flatMap}s, {@link #filter}s, and {@link #zip}s
 * to transform the elements to the desired output, then finally</li>
 * <li><em>reifying</em> the results by iterating the resulting FunIterable with a <em>{@code for}</em> loop,
 * spooling them into an immutable {@link FunList} or {@link FunSet} by invoking {@link #toList}, {@link #toSet},
 * or {@link FunPairs#toMap}, or evaluating the results by calling {@link #head}, {@link #size},
 * {@link #any}/{@link #all}, {@link #min}/{@link #max}, etc</li>
 * </ol>
 * </p><p>
 * <b>IMPORTANT NOTE</b>: all methods from this interface that return {@link FunIterable} or {@link FunPairs}
 * provide <em>lazy</em> iterables:
 * no work is performed until the result is later iterated or inspected by the calling application code.
 * To <em>reify</em> the results, you must iterate them (eg, with a <em>{@code for}</em> loop), convert to a concrete
 * collection (usually by calling {@link #toList()} or {@link #toSet()}), or otherwise investigate the resulting contents.
 * </p><p>
 * Methods whose names begin with "to" (eg, {@link #toSortedCopy}) or return specific elements or values ({@link #head},
 * {@link #size}) all perform computation prior to returning.  However, <em>no</em> methods on any FunIterable ever
 * modify the contents of the original Iterable; implementations of this interface are immutable and thread-safe as
 * long as the underlying Iterables are.
 * </p>
 *
 * @see FunPairs
 * @see FunList
 * @see FunSet
 * @see F F
 * @see F2 F2
 * @see Pred Pred
 * @see Pred2 Pred2
 */
public interface FunIterable<T> extends Iterable<T>, Runnable {
  Iterable<T> delegate();

  default Iterator<T> iterator() {
    return delegate().iterator();
  }

  /**
   * @return a {@link Maybe} containing the first element of this sequence, or {@link Maybe.Not} if the sequence
   * is empty.
   */
  default Maybe<T> head() {
    return head(delegate());
  }

  public static <T> Maybe<T> head(Iterable<? extends T> iterable) {
    Iterator<? extends T> iterator = iterable.iterator();
    return iterator.hasNext() ? Maybe.definitely(iterator.next()) : Maybe.<T>not();
  }

  /**
   * @return a new FunIterable that skips the first element in the underlying sequence.
   */
  default FunIterable<T> tail() {
    return tail(delegate());
  }

  public static <T> FunIterable<T> tail(Iterable<T> delegate) {
    return Iterables.isEmpty(delegate) ? Functional.<T>empty() : new FunctionalIterable<T>(Iterables.skip(delegate, 1));
  }

  @Nonnull default T first() {
    return head().getOrThrow();
  }

  /**
   * @return a {@link Maybe} containing the last element of this sequence, or {@link Maybe.Not} if the sequence
   * is empty.
   */
  default Maybe<T> foot() {
    return foot(delegate());
  }

  public static <T> Maybe<T> foot(Iterable<T> delegate) {
    return Maybe.of(Iterables.getLast(delegate, null));
  }

  @Nonnull default T last() {
    return foot().getOrThrow();
  }

  /**
   * @return the count of elements in this sequence.  Note that, unless this instance wraps a {@link Collection},
   * this method must iterate the elements (applying all chained computations) to count them.
   */
  default int size() {
    return Iterables.size(delegate());
  }

  /**
   * @param transformer a {@link Function} to apply to each element in this sequence
   * @param <O>         the type of the elements in the resulting FunIterable, as returned by the {@code transformer}
   * @return a new FunIterable that, when iterated, yields the result of applying the {@code transformer}
   * to each element of the underlying sequence
   */
  default <O> FunIterable<O> map(Function<? super T, ? extends O> transformer) {
    return map(delegate(), transformer);
  }

  public static <I, O> FunIterable<O> map(Iterable<I> iterable, Function<? super I, ? extends O> transformer) {
    return new FunctionalIterable<O>(Iterables.transform(iterable, F.<I, O>function(transformer::apply)));
  }


  default <K, V> FunPairs<K, V> mapPairs(Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> transformer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), F.<T, Map.Entry<? extends K, ? extends V>>function(transformer::apply)));
  }

  default <K, V> FunPairs<K, V> unzip(Unzipper<? super T, K, V> unzipper) {
    return new FunPairs<K, V>() {
      @Override
      public Iterable<Map.Entry<K, V>> delegate() {
        return Iterables.transform(FunIterable.this.delegate(), unzipper);
      }

      @Override
      public Iterable<K> keysDelegate() {
        return Iterables.transform(FunIterable.this.delegate(), unzipper.keyTransformer());
      }

      @Override
      public Iterable<V> valuesDelegate() {
        return Iterables.transform(FunIterable.this.delegate(), unzipper.valueTransformer());
      }
    };
  }

  /**
   * Produces a new FunIterable that concatenates the results of applying the {@code transformer} to each element
   * in the underlying sequence.<br/><br/>
   * <p>
   * Example:
   * <pre>{@code
   * FunList<String> list = funList(ImmutableList.of("come", "go"));
   * FunIterable<String> result = list.flatMap(new F<String, List<String>>() { public List<String> apply(String input) {
   *     return Arrays.asList("easy", input);
   * }});
   * result.join(" ");        // ==> easy come easy go
   * result.toList().get(3);  // ==> go
   * }</pre>
   *
   * @param transformer a {@link Function} that will convert each value in this sequence to an Iterable of
   *                    {@code <O>} values
   * @return a new FunIterable that, when iterated, yields the concatenation of the results from applying
   * the {@code transformer} to each element of the underlying sequence
   */
  default <O> FunIterable<O> flatMap(Function<? super T, ? extends Iterable<O>> transformer) {
    return flatMap(delegate(), transformer);
  }

  public static <I, O> FunIterable<O> flatMap(Iterable<I> iterable, Function<? super I, ? extends Iterable<O>> transformer) {
    return concat(Iterables.transform(iterable, (com.google.common.base.Function<? super I, ? extends Iterable<O>>) transformer::apply));
  }

  public static <T> FunIterable<T> concat(Iterable<? extends Iterable<? extends T>> inputs) {
    return new FunctionalIterable<>(Iterables.concat(inputs));
  }

  public static <T> FunIterable<T> concat(Iterable<? extends T> input1, Iterable<? extends T> input2) {
    return new FunctionalIterable<>(Iterables.concat(input1, input2));
  }

  default <K, V> FunPairs<K, V> flatMapPairs(Function<? super T, ? extends Iterable<? extends Map.Entry<? extends K, ? extends V>>> entryBuilder) {
    return new FunctionalPairs<>(Iterables.concat(Iterables.transform(delegate(), (com.google.common.base.Function<? super T, ? extends Iterable<? extends Map.Entry<? extends K, ? extends V>>>) entryBuilder::apply)));
  }


  /**
   * Immediately passes each element of this FunIterable to the provided visitor, then returns this FunIterable itself.
   * Due to the verbosity of java closures, this is generally only useful when the visitor is provided abstractly to
   * the calling code: when the visiting logic is defined in-line, simply iterating this FunIterable with a
   * <em>{@code for}</em> loop will tend to be more clear and efficient.
   *
   * @param visitor a {@link Consumer} that may trigger some side-effect for each element from this sequence
   * @return this FunIterable
   */
  default FunIterable<T> foreach(Consumer<? super T> visitor) {
    forEach(visitor);
    return this;
  }

  default void forEach(Consumer<? super T> visitor) {
    delegate().forEach(visitor);
  }

  /**
   * Eliminates elements that do not match a given {@link Predicate}.
   *
   * @return a new FunIterable that, when iterated, applies the {@code predicate} to each element in this
   * sequence and yields only those elements for which the result is {@code true}.
   */
  default FunIterable<T> filter(Predicate<? super T> predicate) {
    return filter(delegate(), predicate);
  }

  public static <T> FunIterable<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
//        return new FunctionalIterable<>(Iterables.<T>filter(iterable, predicate::test)); // javac rejects the method reference..?
    return new FunctionalIterable<>(Iterables.filter(iterable, t -> predicate.test(t)));
  }

  default FunIterable<T> filterNot(Predicate<? super T> predicate) {
    return filter(predicate.negate());
  }

  /**
   * Returns a filtered and casted FunIterable containing the elements from this sequence that are instances
   * of the given type (ie, {@code filteredClass.isInstance(element)}).
   *
   * @param filteredClass the type to allow in the resulting FunIterable
   */
  default <U> FunIterable<U> filter(Class<U> filteredClass) {
    return filter(delegate(), filteredClass);
  }

  public static <T, U> FunIterable<U> filter(Iterable<T> iterable, Class<U> filteredClass) {
    return new FunctionalIterable<U>(Iterables.filter(iterable, filteredClass));
  }

  /**
   * Returns this same FunIterable casted to the requested generic type.<br/><br/>
   * <p>
   * Intended to be used ONLY for "widening" conversions
   * (eg, casting a FunIterable&lt;Fish&gt; to a FunIterable&lt;Animal&gt;).<br/><br/>
   * <p>
   * Java, unfortunately, does not support contravariant type-restrictions; ideally, the signature
   * for this method would be:
   * <pre>
   * {@code <S super T> FunIterable<S> cast();}
   * </pre>
   *
   * @see #filter(Class)
   */
  default <U> FunIterable<U> cast() {
    return Reflect.blindCast(this);
  }


  /**
   * Applies the given {@link Predicate} to each element in this sequence, and counts the number of matches.
   *
   * @return the number of elements for which the given {@code predicate} returns {@code true}
   */
  default int count(Predicate<? super T> predicate) {
    return count(delegate(), predicate);
  }

  public static <T> int count(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterables.size(Iterables.filter(iterable, predicate::test));
  }

  /**
   * Returns a new FunIterable wrapping a sorted ArrayList of the elements from this sequence, as produced
   * by {@link Collections#sort(List, Comparator)}
   *
   * @param ordering an {@link Ordering} to determine the sort-order.
   */
  default FunIterable<T> toSortedCopy(Ordering<? super T> ordering) {
    return sort(delegate(), ordering);
  }

  /**
   * Requires that the values be inherently {@link java.lang.Comparable}
   *
   * @returns {@code toSortedCopy(Ordering.<T>natural());}
   * @see com.google.common.collect.Ordering#natural
   * @see #toSortedCopy(Ordering)
   */
  @SuppressWarnings({"unchecked"})
  default FunIterable<T> toSortedCopy() {
    return (FunIterable<T>) sort((Iterable<? extends Comparable>) delegate(), Ordering.<Comparable>natural());
  }

  public static <T> FunIterable<T> sort(Iterable<T> iterable, Ordering<? super T> ordering) {
    return new FunctionalIterable<T>(ordering.sortedCopy(iterable));
  }

  default FunIterable<T> toShuffledCopy() {
    List<T> list = accumulate(Accumulator.arrayList());
    Collections.shuffle(list);
    return new FunctionalIterable<>(list);
  }

  /**
   * @return true if any of the elements in this sequence match the given {@code predicate}.<br/>
   * If this FunIterable is empty, returns false.<br/><br/>
   * <p>
   * Note that this computation terminates as soon as a first element is found that matches the predicate;
   * subsequent elements will not be evaluated.
   */
  default boolean any(Predicate<? super T> predicate) {
    return Iterables.any(delegate(), predicate::test);
  }

  /**
   * @return true if all of the elements in this sequence match the given {@code predicate}, or if this
   * FunIterable is empty.<br/><br/>
   * <p>
   * Note that this computation terminates as soon as a first element is found that does not matches the predicate;
   * subsequent elements will not be evaluated.
   */
  default boolean all(Predicate<? super T> predicate) {
    return Iterables.all(delegate(), predicate::test);
  }


  /**
   * Iterates this sequence until an element is found that matches the given {@code predicate}, and returns a
   * {@link Maybe} containing that element (or {@link Maybe.Not} if no element matches).
   */
  default Maybe<T> find(Predicate<? super T> predicate) {
    return find(delegate(), predicate);
  }

  public static <T> Maybe<T> find(Iterable<T> iterable, Predicate<? super T> predicate) {
    for (T item : iterable) {
      if (predicate.test(item)) return Maybe.definitely(item);
    }
    return Maybe.not();
  }

  /**
   * Produces a new {@link FunPairs} sequence yielding {@link java.util.Map.Entry Entry} pairs that contain keys from
   * this FunIterable, and values from the given {@code values} Iterable. The pairs are created by iterating both
   * input sequences simultaneously and pairing the elements together in the order they appear.<br/><br/>
   * <p>
   * The {@link #size} of the resulting sequence will be equal to the shorter of the two input sequences:
   * the resulting sequence will end as soon as either of the inputs does.
   *
   * @param values the values to combine with keys from this FunIterable
   */
  default <U> FunPairs<T, U> zip(final Iterable<U> values) {
    return zip(delegate(), values);
  }

  public static <T, U> FunPairs<T, U> zip(final Iterable<? extends T> first, final Iterable<? extends U> second) {
    return new FunctionalPairs.ZippedPairs<>(first, second);
  }

  /**
   * Produces a new FunIterable containing elements obtained by passing elements from this sequence,
   * combined with elements of the given Iterable, to the provided {@link F2} {@code visitor}.<br/><br/>
   * <p>
   * The effect is illustrated by the following example. The zipWith form is more efficient because it avoids creating
   * {@link java.util.Map.Entry Map.Entry} elements for each pair in the sequence:
   * <pre>{@code
   * FunIterable<T> items = ...;
   * Iterable<U> otherItems = ...;
   * F2<T,U,V> visitor = ...;
   * <p>
   * // these next two lines are equivalent, but zipWith is more efficient:
   * // items.zip(otherItems).map(visitor.tupled());
   * items.zipWith(otherItems, visitor);
   * }</pre>
   *
   * @param other   the elements to combine with elements from this FunIterable to pass as inputs to the {@code visitor}
   * @param visitor a function to apply to pairs from this sequence and the {@code other} sequence
   * @see #zip
   */
  default <U, V> FunIterable<V> zipWith(Iterable<U> other, F2<? super T, ? super U, ? extends V> visitor) {
    return zipWith(delegate(), other, visitor);
  }

  public static <T, U, V> FunIterable<V> zipWith(final Iterable<? extends T> first, final Iterable<? extends U> second, final F2<? super T, ? super U, ? extends V> visitor) {
    return new FunctionalIterable<>(ZippedIterable.of(first, second, visitor));
  }

  /**
   * Produces a new FunPairs sequence with {@link java.util.Map.Entry Entry} pairs generated by combining keys from
   * this sequence with their 0-based index in the sequence.
   * <p>
   * Example:
   * <pre>{@code
   * Functional.funList(ImmutableList.of("a", "b", "c")).zipWithIndex();
   * // ==> [("a", 0), ("b", 1), ("c", 2)]
   * }</pre>
   */
  default FunPairs<T, Integer> zipWithIndex() {
    return zipWithIndex(delegate());
  }

  public static <T> FunPairs<T, Integer> zipWithIndex(Iterable<T> items) {
    return zip(items, Count.FromZero);
  }

  /**
   * Produces a new FunIterable containing elements that are obtained by passing elements from this sequence,
   * combined with their 0-based index in the sequence, to the provided {@link F2} {@code visitor}.
   *
   * @see #zipWithIndex
   * @see #zipWith
   */
  default <U> FunIterable<U> mapWithIndex(F2<? super T, ? super Integer, ? extends U> visitor) {
    return mapWithIndex(delegate(), visitor);
  }

  public static <T, U> FunIterable<U> mapWithIndex(Iterable<T> delegate, F2<? super T, ? super Integer, ? extends U> visitor) {
    return zipWith(delegate, Count.FromZero, visitor);
  }


  default <I> I foldLeft(I input, F2<? super I, ? super T, ? extends I> folder) {
    return foldLeft(input, delegate(), folder);
  }

  public static <I, T> I foldLeft(I input, Iterable<T> iterable, F2<? super I, ? super T, ? extends I> folder) {
    I iter = input;
    for (T t : iterable) iter = folder.apply(iter, t);
    return iter;
  }


  default Maybe<T> reduce(F2<? super T, ? super T, ? extends T> reducer) {
    Iterator<T> iterator = iterator();
    if (!iterator.hasNext()) return Maybe.not();
    T reduced = iterator.next();
    while (iterator.hasNext()) {
      reduced = reducer.apply(reduced, iterator.next());
    }
    return Maybe.definitely(reduced);
  }


  /**
   * Produces a new FunIterable sequence that yields all initial elements from this sequence that match the given
   * {@code predicate}.  The resulting sequence ends just before the first element from this sequence that does not
   * match the predicate.
   */
  default FunIterable<T> takeWhile(final Predicate<? super T> predicate) {
    return takeWhile(delegate(), predicate);
  }

  public static <T> FunIterable<T> takeWhile(final Iterable<T> source, final Predicate<? super T> predicate) {
    return new FunctionalIterable<T>(new Iterable<T>() {
      public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
          private final Iterator<T> unfiltered = source.iterator();

          @Override
          protected T computeNext() {
            if (!unfiltered.hasNext()) return endOfData();
            T next = unfiltered.next();
            return predicate.test(next) ? next : endOfData();
          }
        };
      }
    });
  }

  /**
   * Produces a new FunIterable sequence that yields all trailing elements from this sequence that appear after
   * (and including) the first element that does not match the given {@code predicate}.<br/><br/>
   * <p>
   * Essentially, the resulting sequence skips all initial elements from this sequence that match the predicate,
   * and then yields all remaining elements.
   */
  default FunIterable<T> dropWhile(final Predicate<? super T> predicate) {
    return dropWhile(delegate(), predicate);
  }

  public static <T> FunIterable<T> dropWhile(final Iterable<T> source, final Predicate<? super T> predicate) {
    return filter(source, new Pred<T>() {
      boolean stillDropping = true;

      public boolean test(T input) {
        if (stillDropping) stillDropping = predicate.test(input);
        return !stillDropping;
      }
    });
  }

  /**
   * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating keys from
   * this sequence with values computed by passing them to the provided {@code valueComputer}.
   *
   * @param valueComputer a function to compute the values to be associated with each element from this sequence
   */
  default <V> FunPairs<T, V> asKeysTo(final Function<? super T, ? extends V> valueComputer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), input -> Pair.of(input, valueComputer.apply(input))));
  }


  /**
   * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating each key from
   * this sequence with each of a sequence of values computed by passing that key to the provided {@code valuesComputer}.<br/><br/>
   * <p>
   * This is particularly useful for computing {@link Maybe} values for each element, and filtering to produce only
   * those pairs for which the result {@link Maybe#isDefined isDefined}
   *
   * @param valuesComputer a function to compute the sequence of values to be associated with each element from this sequence
   */
  default <V> FunPairs<T, V> asKeysToFlattened(final Function<? super T, ? extends Iterable<? extends V>> valuesComputer) {
    return flatMapPairs(input -> Iterables.transform(valuesComputer.apply(input), Pair.<T, V>creator().bindFirst(input)));
  }

  /**
   * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating values from
   * this sequence with keys computed by passing them to the provided {@code valueComputer}.
   *
   * @param keyComputer a function to compute the values to be associated with each element from this sequence
   * @see com.google.common.collect.Maps#uniqueIndex(Iterable, com.google.common.base.Function)
   */
  default <K> FunPairs<K, T> asValuesFrom(final Function<? super T, ? extends K> keyComputer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), input -> Pair.of(keyComputer.apply(input), input)));
  }


  /**
   * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating each value from
   * this sequence with each of a sequence of keys computed by passing that value to the provided {@code keysComputer}.<br/><br/>
   * <p>
   * This is particularly useful for computing {@link Maybe} values for each element, and filtering to produce only
   * those pairs for which the result {@link Maybe#isDefined isDefined}
   *
   * @param keysComputer a function to compute the sequence of values to be associated with each element from this sequence
   */
  default <K> FunPairs<K, T> asValuesFromFlattened(final Function<? super T, ? extends Iterable<? extends K>> keysComputer) {
    return flatMapPairs(new F<T, Iterable<Pair<K, T>>>() {
      public Iterable<Pair<K, T>> apply(T input) {
        return Iterables.transform(keysComputer.apply(input), Pair.<K, T>creator().bindSecond(input));
      }
    });
  }


  /**
   * Applies the given {@link Function} to each element in this sequence, and builds a
   * {@link com.google.common.collect.Multimap Multimap} consisting of the resulting outputs as keys associated with
   * the groups of elements that yielded the same output (key).
   *
   * @param mapper
   * @param <K>
   * @return
   */
  default <K> ImmutableListMultimap<K, T> groupBy(Function<? super T, K> mapper) {
    return groupBy(delegate(), mapper);
  }

  public static <K, V> ImmutableListMultimap<K, V> groupBy(Iterable<V> items, Function<? super V, K> mapper) {
    return Multimaps.index(items, mapper::apply);
  }


  /**
   * Returns the only element contained in this sequence.
   *
   * @throws NoSuchElementException   if the iterable is empty
   * @throws IllegalArgumentException if the iterable contains multiple elements
   */
  default T getOnlyElement() {
    return Iterables.getOnlyElement(delegate());
  }


  /**
   * <em>Reifies</em> this FunIterable by applying all previously chained transformations, and places the result in a new
   * {*link FunList}.
   *
   * @return a new FunList wrapping an ImmutableList containing the elements of this sequence
   */
  default FunList<T> toList() {
    return FunctionalList.extendList(ImmutableList.copyOf(delegate()));
  }


  /**
   * <em>Reifies</em> this FunIterable by applying all previously chained transformations, and places the result in a new
   * {*link FunSet}.
   *
   * @return a new FunSet wrapping an ImmutableSet containing the (deduplicated) elements of this sequence
   */
  default FunSet<T> toSet() {
    return FunctionalSet.copyOf(delegate());
  }

  default Stream<T> asStream(boolean parallel) {
    return StreamSupport.stream(spliterator(), parallel);
  }

  default <A> A accumulate(Accumulator<? super T, ? extends A> accumulator) {
    foreach(accumulator);
    return accumulator.get();
  }

  default <A, R> R collect(Collector<? super T, A, R> collector) {
    A container = collector.supplier().get();
    BiConsumer<A, ? super T> accumulator = collector.accumulator();
    forEach(u -> accumulator.accept(container, u));
    return collector.finisher().apply(container);
  }

  /**
   * Checks if this sequence is empty.
   *
   * @return if the underlying Iterable is a {@link Collection}, returns the result of delegate().isEmpty();
   * otherwise, {@code iterator().hasNext()}
   */
  default boolean isEmpty() {
    return Iterables.isEmpty(delegate());
  }

  default boolean contains(Object element) {
    return Iterables.contains(delegate(), element);
  }

  /**
   * @return A {@link Maybe} containing the minimum element of this sequence, as determined by the provided
   * {@code ordering}, or {@link Maybe.Not} if this sequence is empty.
   */
  default Maybe<T> min(final Ordering<? super T> ordering) {
    return min(delegate(), ordering);
  }

  public static <T> Maybe<T> min(Iterable<? extends T> items, Ordering<? super T> ordering) {
    Iterator<? extends T> iterator = items.iterator();
    return iterator.hasNext() ? Maybe.of(ordering.min(iterator)) : Maybe.<T>not();
  }

  /**
   * @return A {@link Maybe} containing the maximum element of this sequence, as determined by the provided
   * {@code ordering}, or {@link Maybe.Not} if this sequence is empty.
   */
  default Maybe<T> max(final Ordering<? super T> ordering) {
    return max(delegate(), ordering);
  }

  public static <T> Maybe<T> max(Iterable<? extends T> items, Ordering<? super T> ordering) {
    Iterator<? extends T> iterator = items.iterator();
    return iterator.hasNext() ? Maybe.of(ordering.max(iterator)) : Maybe.<T>not();
  }

  default Maybe<T> minBy(Function<? super T, ? extends Comparable<?>> valueComputer) {
    return minBy(Ordering.natural(), valueComputer);
  }

  default <V> Maybe<T> minBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
    // we create k/v pairs to associate the (possibly expensive) results of the valueComputer with each element
    return asKeysTo(valueComputer)
            .minByValues(ordering)
            .map(Pair.<T>getFirstFromPair());
  }

  default Maybe<T> maxBy(Function<? super T, ? extends Comparable<?>> valueComputer) {
    return maxBy(Ordering.natural(), valueComputer);
  }

  default <V> Maybe<T> maxBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
    return minBy(ordering.reverse(), valueComputer);
  }

  /**
   * Iterates this sequence, applying the provided {@code valueComputer} on each element, and sums the results.
   *
   * @param valueComputer
   * @return the sum of the values returned by {@code valueComputer} for each element in this sequence
   */
  default int sum(ToIntFunction<? super T> valueComputer) {
    return sum(delegate(), valueComputer);
  }

  public static <T> int sum(Iterable<T> delegate, ToIntFunction<? super T> valueComputer) {
    // yes, we could do some fancy functional foldLeft here ...  but this'll be faster
    int sum = 0;
    for (T item : delegate) {
      sum += valueComputer.applyAsInt(item);
    }

    return sum;
  }

  /**
   * @return {@link Joiner}.on(separator).join(this);
   * @see Joiner
   */
  default String join(String separator) {
    return Joiner.on(separator).join(delegate());
  }

  /**
   * @return {@link Joiner}.on(separator).join(this);
   * @see Joiner
   */
  default String join(char separator) {
    return Joiner.on(separator).join(delegate());
  }

  /**
   * @return a {@link LazyReference} that lazily calls and returns the result of #toList the first time it's requested
   */
  default LazyReference<FunList<T>> lazyListSupplier() {
    return new LazyReference<FunList<T>>() {
      protected FunList<T> supplyValue() {
        return toList();
      }
    };
  }


  /**
   * @param firstElement an element to prepend to the front of this Iterable
   * @return a new FunIterable that will yield the {@code firstElement} followed by the elements in this sequence
   * @see PrependedIterable
   */
  default FunIterable<T> prepend(T firstElement) {
    return prepend(firstElement, delegate());
  }

  public static <T> FunIterable<T> prepend(T first, Iterable<? extends T> rest) {
    return new FunctionalIterable<>(PrependedIterable.of(first, rest));
  }

  /**
   * @param lastElement an element to append to the end of this Iterable
   * @return a new FunIterable that will yield the elements in this sequence followed by the {@code lastElement}
   * @see AppendedIterable
   */
  default FunIterable<T> append(T lastElement) {
    return append(delegate(), lastElement);
  }

  public static <T> FunIterable<T> append(Iterable<? extends T> init, T last) {
    return new FunctionalIterable<>(AppendedIterable.of(init, last));
  }

  /**
   * @return A new FunIterable consisting of the elements of this sequence followed by the elements of {@code moreElements}
   * @see Functional#concat
   */
  @SuppressWarnings("unchecked")
  default FunIterable<T> plus(Iterable<? extends T> moreElements) {
    if (moreElements instanceof FunIterable) moreElements = ((FunIterable<? extends T>) moreElements).delegate();
    return concat(delegate(), moreElements);
  }

  /**
   * @param excluded a {@link Collection} of items to be excluded from the resulting sequence
   * @return a new FunIterable that, when iterated, yields all elements of this sequence for which
   * <pre>{@code excluded.contains(element) == false}</pre>
   */
  default FunIterable<T> minus(Collection<? super T> excluded) {
    return difference(delegate(), excluded);
  }

  public static <T> FunIterable<T> difference(Iterable<T> items, Collection<? super T> excluded) {
    return filter(items, Pred.in(excluded).negate());
  }

  /**
   * Produces a new FunPairs sequence that yields Entries with all elements of this sequence as keys paired with
   * all elements of the given {@code innerItems} as values.  If java supported some sort of "yield" keyword, the
   * implementation for this method might look something like this:
   * <p>
   * <pre>{@code
   * for (T outerElement : this) {
   *     for (U innerElement : innerItems) {
   *         yield Pair.of(outerElement, innerElement);
   *     }
   * }
   * }</pre>
   *
   * @param innerItems
   * @param <U>
   * @return
   */
  default <U> FunPairs<T, U> crossProduct(Iterable<U> innerItems) {
    return crossProduct(delegate(), innerItems);
  }

  public static <T, U> FunPairs<T, U> crossProduct(Iterable<T> outerItems, final Iterable<U> innerItems) {
    return new FunctionalPairs<>(
            Iterables.concat(
                    Iterables.transform(outerItems,
                            (T outerItem) -> Iterables.transform(innerItems,
                                    FunctionalPairs.<T, U>entryCreator().bindFirst(outerItem)))
            )
    );
  }

  /**
   * Just iterates the sequence, triggering any side-effects that may be implied by its construction.
   */
  default void run() {
    run(delegate());
  }

  public static void run(Iterable<?> delegate) {
    final Iterator<?> iterator = delegate.iterator();
    while (iterator.hasNext()) iterator.next();
  }

  /**
   * Divides this sequence into subsequences of the given size (the final subsequence may be smaller).
   *
   * @see com.google.common.collect.Iterables#partition
   */
  default FunIterable<? extends FunIterable<T>> partition(int size) {
    return partition(delegate(), size);
  }

  public static <T> FunIterable<FunIterable<T>> partition(Iterable<T> items, int size) {
    return map(Iterables.partition(items, size), Functional.<T>extender());
  }

  default <B extends ImmutableCollection.Builder<? super T>> B addTo(B builder) {
    builder.addAll(delegate());
    return builder;
  }

  default FunIterable<T> unique() {
    return filter(Pred.newDeduplicator());
  }

  default FunIterable<T> limit(int maxElements) {
    return new FunctionalIterable<>(Iterables.limit(delegate(), maxElements));
  }

  default FunIterable<T> skip(int skippedElements) {
    return new FunctionalIterable<T>(Iterables.skip(delegate(), skippedElements));
  }


  static class ZippedIterable<T, U, V> implements Iterable<V> {
    private final Iterable<? extends T> first;
    private final Iterable<? extends U> second;
    private final F2<? super T, ? super U, ? extends V> visitor;

    static <T, U, V> ZippedIterable<T, U, V> of(Iterable<? extends T> first, Iterable<? extends U> second, F2<? super T, ? super U, ? extends V> visitor) {
      return new ZippedIterable<>(first, second, visitor);
    }

    private ZippedIterable(Iterable<? extends T> first, Iterable<? extends U> second, F2<? super T, ? super U, ? extends V> visitor) {
      this.first = first;
      this.second = second;
      this.visitor = visitor;
    }

    public Iterator<V> iterator() {
      return new ZipWithIterator<T, U, V>(first.iterator(), second.iterator(), visitor);
    }

    private static class ZipWithIterator<T, U, V> extends AbstractIterator<V> {
      private final Iterator<? extends T> firstIterator;
      private final Iterator<? extends U> secondIterator;
      private final F2<? super T, ? super U, ? extends V> visitor;

      public ZipWithIterator(Iterator<? extends T> firstIterator, Iterator<? extends U> secondIterator, F2<? super T, ? super U, ? extends V> visitor) {
        this.visitor = visitor;
        this.firstIterator = firstIterator;
        this.secondIterator = secondIterator;
      }

      @Override
      protected V computeNext() {
        return firstIterator.hasNext() && secondIterator.hasNext()
                ? visitor.apply(firstIterator.next(), secondIterator.next())
                : endOfData();
      }
    }
  }
}
