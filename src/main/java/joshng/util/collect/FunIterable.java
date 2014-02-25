package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.Consumer;
import joshng.util.blocks.Function2;
import joshng.util.concurrent.LazyReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 4:29:32 PM
 */
/**
 * <p>
 * A wrapper around an Iterable that offers higher-order functions (eg, {@link #map}(..), {@link #filter}(..), etc).</p><p>
 *
 * A typical interaction will involve:
 * <ol>
 *     <li>obtaining a FunIterable by passing an existing Iterable or array to {@link Functional#extend}</li>
 *     <li>"chaining" together a program of {@link #map}s, {@link #flatMap}s, {@link #filter}s, and {@link #zip}s
 *     to transform the elements to the desired output, then finally</li>
 *     <li><em>reifying</em> the results by iterating the resulting FunIterable with a <em>{@code for}</em> loop,
 *     spooling them into an immutable {@link FunList} or {@link FunSet} by invoking {@link #toList}, {@link #toSet},
 *     or {@link FunPairs#toMap}, or evaluating the results by calling {@link #head}, {@link #size},
 *     {@link #any}/{@link #all}, {@link #min}/{@link #max}, etc</li>
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
 * @see FunPairs
 * @see FunList
 * @see FunSet
 * @see joshng.util.blocks.F F
 * @see joshng.util.blocks.F2 F2
 * @see joshng.util.blocks.Pred Pred
 * @see joshng.util.blocks.Pred2 Pred2
 */
public interface FunIterable<T> extends Iterable<T>, Runnable {
    /**
     * @return a {@link Maybe} containing the first element of this sequence, or {@link Maybe.Not} if the sequence
     * is empty.
     */
    Maybe<T> head();

    /**
     * @return a new FunIterable that skips the first element in the underlying sequence.
     */
    FunIterable<T> tail();

    /**
     * @return a {@link Maybe} containing the last element of this sequence, or {@link Maybe.Not} if the sequence
     * is empty.
     */
    Maybe<T> last();

    /**
     * @return the count of elements in this sequence.  Note that, unless this instance wraps a {@link Collection},
     * this method must iterate the elements (applying all chained computations) to count them.
     */
    int size();

    /**
     * @param transformer a {@link Function} to apply to each element in this sequence
     * @param <O> the type of the elements in the resulting FunIterable, as returned by the {@code transformer}
     * @return a new FunIterable that, when iterated, yields the result of applying the {@code transformer}
     * to each element of the underlying sequence
     */
    <O> FunIterable<O> map(Function<? super T, ? extends O> transformer);

    <K,V> FunPairs<K,V> mapPairs(Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> transformer);
    /**
     * Produces a new FunIterable that concatenates the results of applying the {@code transformer} to each element
     * in the underlying sequence.<br/><br/>
     *
     * Example:
     * <pre>{@code
     * FunList<String> list = funList(ImmutableList.of("come", "go"));
     * FunIterable<String> result = list.flatMap(new F<String, List<String>>() { public List<String> apply(String input) {
     *     return Arrays.asList("easy", input);
     * }});
     * result.join(" ");        // ==> easy come easy go
     * result.toList().get(3);  // ==> go
     * }</pre>
     * @param transformer a {@link Function} that will convert each value in this sequence to an Iterable of
     *                    {@code <O>} values
     * @return a new FunIterable that, when iterated, yields the concatenation of the results from applying
     * the {@code transformer} to each element of the underlying sequence
     */
    <O> FunIterable<O> flatMap(Function<? super T, ? extends Iterable<O>> transformer);

    <K,V> FunPairs<K,V> flatMapPairs(Function<? super T, ? extends Iterable<? extends Map.Entry<? extends K, ? extends V>>> entryBuilder);

    /**
     * Like {@link #map}, but applies the {@code transformer} to each element <em>in parallel</em> by submitting a task
     * for each transformation to the provided {@link ExecutorService}.<br/><br/>
     *
     * Important notes:
     * <ul>
     *   <li>This method will <b>immediately</b> submit tasks for every element to the provided {@code threadPool}</li>
     *   <li>Any work implied by previously-chained FunIterable transformations will immediately be reified
     *       <em>in the calling thread</em> (not in the ExecutorService) in order to submit the necessary input
     *       to the ExecutorService. Only the work contained within the provided transformer will be submitted for
     *       parallel execution.  To ensure that all desired work is performed in parallel, consider composing
     *       transformations together.
     *   <li>When iterating the resulting Iterable, each step of the iteration must block until the required parallel
     *       task is completed</li>
     *   <li>To exercise more control over the blocking behavior, consider using {@link #toParallelFutures} instead</li>
     * </ul>
     *
     * @param threadPool an ExecutorService to provide threads for parallel execution
     * @param transformer a Function to apply to each element of this Iterable in parallel
     * @param <O> the type of the elements in the resulting FunIterable, as returned by the {@code transformer}
     * @return a new FunIterable that, when iterated, yields the result of applying the {@code transformer} to each
     * element of the underlying sequence within the given ExecutorService, possibly blocking for each element's task
     * to complete.
     */
    <O> FunIterable<O> parallelMap(ExecutorService threadPool, Function<? super T, ? extends O> transformer);
    <O> FunIterable<O> parallelFlatMap(ExecutorService threadPool, Function<? super T, ? extends Iterable<? extends O>> transformer);
    <O> FunList<ListenableFuture<O>> toParallelFutures(ExecutorService threadPool, Function<? super T, ? extends O> transformer);

    /**
     * Immediately passes each element of this FunIterable to the provided visitor, then returns this FunIterable itself.
     * Due to the verbosity of java closures, this is generally only useful when the visitor is provided abstractly to
     * the calling code: when the visiting logic is defined in-line, simply iterating this FunIterable with a
     * <em>{@code for}</em> loop will tend to be more clear and efficient.
     * @param visitor a {@link joshng.util.blocks.Consumer} that may trigger some side-effect for each element from this sequence
     * @return this FunIterable
     */
    FunIterable<T> foreach(Consumer<? super T> visitor);

    /**
     * Eliminates elements that do not match a given {@link Predicate}.
     * @return a new FunIterable that, when iterated, applies the {@code predicate} to each element in this
     * sequence and yields only those elements for which the result is {@code true}.
     */
    FunIterable<T> filter(Predicate<? super T> predicate);

    /**
     * Returns a filtered and casted FunIterable containing the elements from this sequence that are instances
     * of the given type (ie, {@code filteredClass.isInstance(element)}).
     * @param filteredClass the type to allow in the resulting FunIterable
     */
    <U> FunIterable<U> filter(Class<U> filteredClass);

    /**
     * Returns this same FunIterable casted to the requested generic type.<br/><br/>
     *
     * Intended to be used ONLY for "widening" conversions
     * (eg, casting a FunIterable&lt;Fish&gt; to a FunIterable&lt;Animal&gt;).<br/><br/>
     *
     * Java, unfortunately, does not support contravariant type-restrictions; ideally, the signature
     * for this method would be:
     * <pre>
     * {@code <S super T> FunIterable<S> cast();}
     * </pre>
     * @see #filter(Class)
     */
    <S> FunIterable<S> cast();
    /**
     * Applies the given {@link Predicate} to each element in this sequence, and counts the number of matches.
     * @return the number of elements for which the given {@code predicate} returns {@code true}
     */
    int count(Predicate<? super T> predicate);

    /**
     * Returns a new FunIterable wrapping a sorted ArrayList of the elements from this sequence, as produced
     * by {@link Collections#sort(List, Comparator)}
     * @param ordering an {@link Ordering} to determine the sort-order.
     */
    FunIterable<T> toSortedCopy(Ordering<? super T> ordering);

    /**
     * Requires that the values be inherently {@link java.lang.Comparable}
     * @returns {@code toSortedCopy(Ordering.<T>natural());}
     * @see com.google.common.collect.Ordering#natural
     * @see #toSortedCopy(Ordering)
     */
    FunIterable<T> toSortedCopy();
    /**
     * @return true if any of the elements in this sequence match the given {@code predicate}.<br/>
     * If this FunIterable is empty, returns false.<br/><br/>
     * 
     * Note that this computation terminates as soon as a first element is found that matches the predicate;
     * subsequent elements will not be evaluated.
     */
    boolean any(Predicate<? super T> predicate);
    
    /**
     * @return true if all of the elements in this sequence match the given {@code predicate}, or if this
     * FunIterable is empty.<br/><br/>
     * 
     * Note that this computation terminates as soon as a first element is found that does not matches the predicate;
     * subsequent elements will not be evaluated.
     */
    boolean all(Predicate<? super T> predicate);

    /**
     * Iterates this sequence until an element is found that matches the given {@code predicate}, and returns a
     * {@link Maybe} containing that element (or {@link Maybe.Not} if no element matches).
     */
    Maybe<T> find(Predicate<? super T> predicate);

    /**
     * Produces a new {@link FunPairs} sequence yielding {@link java.util.Map.Entry Entry} pairs that contain keys from
     * this FunIterable, and values from the given {@code values} Iterable. The pairs are created by iterating both
     * input sequences simultaneously and pairing the elements together in the order they appear.<br/><br/>
     * 
     * The {@link #size} of the resulting sequence will be equal to the shorter of the two input sequences:
     * the resulting sequence will end as soon as either of the inputs does.
     * @param values the values to combine with keys from this FunIterable
     */
    <U> FunPairs<T,U> zip(Iterable<U> values);

    /**
     * Produces a new FunIterable containing elements obtained by passing elements from this sequence,
     * combined with elements of the given Iterable, to the provided {@link Function2} {@code visitor}.<br/><br/>
     * 
     * The effect is illustrated by the following example. The zipWith form is more efficient because it avoids creating 
     * {@link java.util.Map.Entry Map.Entry} elements for each pair in the sequence:
     * <pre>{@code
     * FunIterable<T> items = ...;
     * Iterable<U> otherItems = ...;
     * F2<T,U,V> visitor = ...;
     * 
     * // these next two lines are equivalent, but zipWith is more efficient:
     * // items.zip(otherItems).map(visitor.tupled());
     * items.zipWith(otherItems, visitor); 
     * }</pre>
     * @param other the elements to combine with elements from this FunIterable to pass as inputs to the {@code visitor}
     * @param visitor a function to apply to pairs from this sequence and the {@code other} sequence
     * @see #zip
     */
    <U,V> FunIterable<V> zipWith(Iterable<U> other, Function2<T,U,V> visitor);

    /**
     * Produces a new FunPairs sequence with {@link java.util.Map.Entry Entry} pairs generated by combining keys from
     * this sequence with their 0-based index in the sequence.
     * 
     * Example:
     * <pre>{@code
     * Functional.funList(ImmutableList.of("a", "b", "c")).zipWithIndex();
     * // ==> [("a", 0), ("b", 1), ("c", 2)]
     * }</pre>
     */
    FunPairs<T, Integer> zipWithIndex();

    /**
     * Produces a new FunIterable containing elements that are obtained by passing elements from this sequence,
     * combined with their 0-based index in the sequence, to the provided {@link Function2} {@code visitor}.
     * @see #zipWithIndex
     * @see #zipWith
     */
    <U> FunIterable<U> mapWithIndex(Function2<? super T, Integer, ? extends U> visitor);
    <I> I foldLeft(I input, Function2<? super I, ? super T, ? extends I> folder);
    Maybe<T> reduce(Function2<? super T, ? super T, ? extends T> reducer);

    /**
     * Produces a new FunIterable sequence that yields all initial elements from this sequence that match the given
     * {@code predicate}.  The resulting sequence ends just before the first element from this sequence that does not
     * match the predicate.
     */
    FunIterable<T> takeWhile(Predicate<? super T> predicate);

    /**
     * Produces a new FunIterable sequence that yields all trailing elements from this sequence that appear after
     * (and including) the first element that does not match the given {@code predicate}.<br/><br/>
     *
     * Essentially, the resulting sequence skips all initial elements from this sequence that match the predicate,
     * and then yields all remaining elements.
     */
    FunIterable<T> dropWhile(Predicate<? super T> predicate);

    /**
     * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating keys from
     * this sequence with values computed by passing them to the provided {@code valueComputer}.
     * @param valueComputer a function to compute the values to be associated with each element from this sequence
     */
    <V> FunPairs<T, V> asKeysTo(Function<? super T, ? extends V> valueComputer);

    /**
     * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating each key from
     * this sequence with each of a sequence of values computed by passing that key to the provided {@code valuesComputer}.<br/><br/>
     *
     * This is particularly useful for computing {@link Maybe} values for each element, and filtering to produce only
     * those pairs for which the result {@link Maybe#isDefined isDefined}
     * @param valuesComputer a function to compute the sequence of values to be associated with each element from this sequence
     */
    <V> FunPairs<T, V> asKeysToFlattened(Function<? super T, ? extends Iterable<? extends V>> valuesComputer);

    /**
     * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating values from
     * this sequence with keys computed by passing them to the provided {@code valueComputer}.
     * @param keyComputer a function to compute the values to be associated with each element from this sequence
     * @see com.google.common.collect.Maps#uniqueIndex(Iterable, com.google.common.base.Function)
     */
    <K> FunPairs<K, T> asValuesFrom(Function<? super T, ? extends K> keyComputer);

    /**
     * Produces a new FunPairs sequence that yields {@link java.util.Map.Entry Entry} pairs associating each value from
     * this sequence with each of a sequence of keys computed by passing that value to the provided {@code keysComputer}.<br/><br/>
     *
     * This is particularly useful for computing {@link Maybe} values for each element, and filtering to produce only
     * those pairs for which the result {@link Maybe#isDefined isDefined}
     * @param keysComputer a function to compute the sequence of values to be associated with each element from this sequence
     */
    <K> FunPairs<K, T> asValuesFromFlattened(Function<? super T, ? extends Iterable<? extends K>> keysComputer);

    /**
     * Applies the given {@link Function} to each element in this sequence, and builds a
     * {@link com.google.common.collect.Multimap Multimap} consisting of the resulting outputs as keys associated with
     * the groups of elements that yielded the same output (key).
     * @param mapper
     * @param <K>
     * @return
     */
    <K> ImmutableListMultimap<K, T> groupBy(Function<? super T, K> mapper);

    /**
     * Returns the only element contained in this sequence.
     * @throws NoSuchElementException if the iterable is empty
     * @throws IllegalArgumentException if the iterable contains multiple elements
     */
    T getOnlyElement();

    /**
     * <em>Reifies</em> this FunIterable by applying all previously chained transformations, and places the result in a new
     * {*link FunList}.
     * @return a new FunList wrapping an ImmutableList containing the elements of this sequence
     */
    FunList<T> toList();

    MutableFunList<T> toMutableList();

    /**
     * <em>Reifies</em> this FunIterable by applying all previously chained transformations, and places the result in a new
     * {*link FunSet}.
     * @return a new FunSet wrapping an ImmutableSet containing the (deduplicated) elements of this sequence
     */
    FunSet<T> toSet();

    MutableFunSet<T> toMutableSet();

    /**
     * Checks if this sequence is empty.
     * @return if the underlying Iterable is a {@link Collection}, returns the result of delegate().isEmpty();
     * otherwise, {@code iterator().hasNext()}
     */
    boolean isEmpty();

    /**
     * @return A {@link Maybe} containing the minimum element of this sequence, as determined by the provided
     * {@code ordering}, or {@link Maybe.Not} if this sequence is empty.
     */
    Maybe<T> min(Ordering<? super T> ordering);

    /**
     * @return A {@link Maybe} containing the maximum element of this sequence, as determined by the provided
     * {@code ordering}, or {@link Maybe.Not} if this sequence is empty.
     */
    Maybe<T> max(Ordering<? super T> ordering);

    Maybe<T> minBy(Function<? super T, ? extends Comparable> valueComputer);
    <V> Maybe<T> minBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer);
    Maybe<T> maxBy(Function<? super T, ? extends Comparable> valueComputer);
    <V> Maybe<T> maxBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer);

    /**
     * Iterates this sequence, applying the provided {@code valueComputer} on each element, and sums the results.
     * @param valueComputer
     * @return the sum of the values returned by {@code valueComputer} for each element in this sequence
     */
    int sum(Function<? super T, Integer> valueComputer);

    /**
     * @return {@link Joiner}.on(separator).join(this);
     * @see Joiner
     */
    String join(String separator);
    Iterable<T> delegate();
    /**
     * @return a {@link LazyReference} that lazily calls and returns the result of #toList the first time it's requested
     */
    LazyReference<FunList<T>> lazyListSupplier();

    /**
     * @param firstElement an element to prepend to the front of this Iterable
     * @return a new FunIterable that will yield the {@code firstElement} followed by the elements in this sequence
     * @see PrependedIterable
     */
    FunIterable<T> cons(T firstElement);

    /**
     * @param lastElement an element to append to the end of this Iterable
     * @return a new FunIterable that will yield the elements in this sequence followed by the {@code lastElement}
     * @see AppendedIterable
     */
    FunIterable<T> append(T lastElement);

    /**
     * @return A new FunIterable consisting of the elements of this sequence followed by the elements of {@code moreElements}
     * @see Functional#concat
     */
    FunIterable<T> plus(Iterable<? extends T> moreElements);

    /**
     * @param excluded a {@link Collection} of items to be excluded from the resulting sequence
     * @return a new FunIterable that, when iterated, yields all elements of this sequence for which
     * <pre>{@code excluded.contains(element) == false}</pre>
     */
    FunIterable<T> minus(Collection<?> excluded);

    /**
     * Produces a new FunPairs sequence that yields Entries with all elements of this sequence as keys paired with
     * all elements of the given {@code innerItems} as values.  If java supported some sort of "yield" keyword, the
     * implementation for this method might look something like this:
     *
     * <pre>{@code
     * for (T outerElement : this) {
     *     for (U innerElement : innerItems) {
     *         yield Pair.of(outerElement, innerElement);
     *     }
     * }
     * }</pre>
     * @param innerItems
     * @param <U>
     * @return
     */
    <U> FunPairs<T, U> crossProduct(Iterable<U> innerItems);

    /**
     * Divides this sequence into subsequences of the given size (the final subsequence may be smaller).
     * @see com.google.common.collect.Iterables#partition
     */
    FunIterable<? extends FunIterable<T>> partition(int size);
    <B extends ImmutableCollection.Builder<? super T>> B addTo(B builder);

    /**
     * @return this sequence with null elements omitted (equivalent to {@link #filter}({@link Predicates#notNull()})
     */
    FunIterable<T> compact();
    FunIterable<T> unique();
    FunIterable<T> limit(int maxElements);
    FunIterable<T> skip(int skippedElements);
}
