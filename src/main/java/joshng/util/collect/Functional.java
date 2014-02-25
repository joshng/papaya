package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.Reflect;
import joshng.util.ThreadLocalRef;
import joshng.util.ThreadLocals;
import joshng.util.blocks.Consumer;
import joshng.util.blocks.F;
import joshng.util.blocks.FBuilder;
import joshng.util.blocks.Function2;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Sink;
import joshng.util.concurrent.FunFutures;
import joshng.util.concurrent.LazyReference;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkState;
import static joshng.util.Reflect.blindCast;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 2:52:23 PM
 */

/**
 * <p>
 * The entry-point for {@link FunIterable}, {@link FunList}, {@link FunSet}, and {@link FunPairs}:
 * wrappers around Iterables that offer higher-order functions (eg, {@link #map}(..), {@link #filter}(..))
 * and other functional-style utilities.
 * @see FunList
 * @see FunSet
 * @see FunPairs
 */
public abstract class Functional<T> extends ForwardingObject implements FunIterable<T> {
    /**
     * Wraps an existing Iterable with a FunIterable.  Note that this method does not make a copy or invoke the provided
     * Iterable in any way: changes to the underlying Iterable will be visible via the returned FunIterable.
     * @param delegate the iterable to extend
     * @return a FunIterable wrapping the underlying iterable.
     */
    public static <T> FunIterable<T> extend(final Iterable<T> delegate) {
        if (delegate instanceof FunIterable) return (FunIterable<T>) delegate;
        if (MoreCollections.isCollectionThatIsEmpty(delegate)) return empty();
        return new FunctionalIterable<T>(delegate);
    }

    public static <T> FunList<T> extend(final Maybe<T> delegate) {
        return delegate.isDefined() ? funListOf(delegate.getOrThrow()) : Functional.<T>emptyList();
    }

    /**
     * Wraps an array in a FunIterable. Equivalent to calling {@link #extend}(Arrays.asList(items))
     */
    public static <T> FunIterable<T> extend(T[] items) {
        if (items.length == 0) return empty();
        return new FunctionalIterable<T>(Arrays.asList(items));
    }

    public static <T> FunIterable<T> extend(Iterator<T> iterator) {
        return extend(IteratorIterable.of(iterator));
    }

    public static <T> FunIterable<T> extend(Enumeration<T> enumeration) {
        return extend(Iterators.forEnumeration(enumeration));
    }

    /**
     * @return a function that wraps its parameter in a FunIterable by calling {@link #extend}
     */
    public static <T> F<Iterable<T>, FunIterable<T>> extender() {
        return Reflect.blindCast(EXTENDER);
    }
    private static final F EXTENDER = new F<Iterable<Object>, FunIterable<Object>>() { public FunIterable<Object> apply(Iterable<Object> input) {
        return extend(input);
    } };

    private static final ThreadLocalRef<ExecutorService> activeParallelThreadPool = ThreadLocals.newThreadLocalRef();

    /**
     * @return an empty FunIterable. All methods on the empty FunIterable are optimized to short-circuit computation.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> FunIterable<T> empty() {
        return EMPTY;
    }

    /**
     * Copies the provided Iterable into a new {@link FunList}. This is usually undesirable, because it
     * incurs a copy of the collection prior to performing operations on it.<br/><br/>
     *
     * Typically, functional traversals should instead begin with {@link #extend(Iterable)},
     * invoking {@link #toList()} or {@link #toSet()} (or simply iterating with a <em>{@code for}</em> loop) only after all
     * desired {@link #map}s and {@link #filter}s are expressed.
     *
     * @return a new FunList containing the items from the provided Iterable. Subsequent changes to the Iterable
     * will <b>not</b> be reflected in the FunList.
     */
    public static <T> FunList<T> funList(Iterable<T> delegate) {
        if (delegate instanceof FunList) return (FunList<T>) delegate;
        return FunctionalList.extend(ImmutableList.copyOf(delegate));
    }

    public static <T> FunList<T> funListOf(T singleton) {
        return new FunctionalList<T>(ImmutableList.of(singleton));
    }

    @SafeVarargs
    public static <T> FunList<T> funListOf(T item1, T... items) {
        return new FunctionalList<>(ImmutableList.copyOf(Lists.asList(item1, items)));
    }

    /**
     * Copies the contents of the array into a new {@link FunList}. To avoid the copy (but be exposed to
     * possible changes in the underlying array), consider using {@link #extend(T[])} instead.
     */
    public static <T> FunList<T> funListOf(T[] items) {
        return FunctionalList.copyOf(items);
    }


    /**
     * Copies the provided Iterable into a new {@link FunSet}. This is usually undesirable unless an initial
     * deduplication of the elements is intended, because it incurs a copy of the collection prior to performing
     * operations on it.<br/><br/>
     *
     * Typically, functional traversals should instead begin with {@link #extend(Iterable)},
     * invoking {@link #toList()} or {@link #toSet()} (or simply iterating with a <em>{@code for}</em> loop) only after all
     * desired {@link #map}s and {@link #filter}s are expressed.
     *
     * @return a new FunSet containing the items from the provided Iterable. Subsequent changes to the Iterable
     * will <b>not</b> be reflected in the FunSet.
     */
    public static <T> FunSet<T> funSet(Iterable<T> items) {
        return FunctionalSet.copyOf(items);
    }

    /**
     * Wraps the provided Iterable of Entries (or Pairs) with a {@link FunPairs}. Changes to the underlying
     * Iterable (or to the source Map, if obtained from {@link java.util.Map#entrySet()}) will be reflected
     * in the returned FunPairs.
     * @see FunPairs
     * @see #funPairs(Map)
     */
    public static <K,V> FunPairs<K,V> funPairs(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        return FunctionalPairs.extendPairs(entries);
    }

    /**
     * Wraps the {@link Map#entrySet} from the provided Map with a {@link FunPairs}. Changes to the underlying Map
     * will be reflected in the returned FunPairs.
     * @see FunPairs
     */
    public static <K,V> FunPairs<K,V> funPairs(Map<K,V> map) {
        return FunctionalPairs.extend(map);
    }


    public static <K,V> MutableFunPairs<K,V> mutableFunPairs(Iterable<? extends Entry<K,V>> entries) {
        Collection<? extends Entry<K,V>> collection;
        if (entries instanceof Collection) {
            collection = (Collection<? extends Entry<K,V>>) entries;
        } else {
            collection = Lists.newArrayList(entries);
        }
        return new MutableFunPairs<K, V>(collection);
    }

    protected Functional() {
    }

    public abstract Iterable<T> delegate();

    @SuppressWarnings({"unchecked"})
    public static <T> FunList<T> emptyList() {
        return FunctionalList.EMPTY;
    }

    @SuppressWarnings({"unchecked"})
    public static <T> FunSet<T> emptySet() {
        return FunctionalSet.EMPTY;
    }

    public static <K,V> FunPairs<K,V> emptyPairs() {
        return blindCast(FunctionalPairs.EMPTY);
    }

    public Iterator<T> iterator() {
        return delegate().iterator();
    }

    public boolean isEmpty() {
        return Iterables.isEmpty(delegate());
    }

    public boolean contains(Object element) {
        return Iterables.contains(delegate(), element);
    }

    public int size() {
        return Iterables.size(delegate());
    }

    public Maybe<T> head() {
        return head(delegate());
    }

    public static <T> Maybe<T> head(Iterable<? extends T> iterable) {
        Iterator<? extends T> iterator = iterable.iterator();
        return iterator.hasNext() ? Maybe.definitely(iterator.next()) : Maybe.<T>not();
    }

    public Maybe<T> min(final Ordering<? super T> ordering) {
        return min(delegate(), ordering);
    }

    public Maybe<T> max(final Ordering<? super T> ordering) {
        return max(delegate(), ordering);
    }

    public Maybe<T> minBy(Function<? super T, ? extends Comparable> valueComputer) {
        return minBy(Ordering.natural(), valueComputer);
    }

    public <V> Maybe<T> minBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
        // we create k/v pairs to associate the (possibly expensive) results of the valueComputer with each element
        return asKeysTo(valueComputer)
                .minByValues(ordering)
                .map(Pair.<T>getFirstFromPair());
    }

    public Maybe<T> maxBy(Function<? super T, ? extends Comparable> valueComputer) {
        return maxBy(Ordering.natural(), valueComputer);
    }

    public <V> Maybe<T> maxBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
        return minBy(ordering.reverse(), valueComputer);
    }

    public static <T> F<Iterable<T>, Maybe<T>> minimizer(final Ordering<? super T> ordering) {
        return new F<Iterable<T>, Maybe<T>>() {
            public Maybe<T> apply(Iterable<T> from) {
                return min(from, ordering);
            }
        };
    }

    public static <T> Maybe<T> min(Iterable<? extends T> items, Ordering<? super T> ordering) {
        Iterator<? extends T> iterator = items.iterator();
        return iterator.hasNext() ? Maybe.of(ordering.min(iterator)) : Maybe.<T>not();
    }

    public static <T> Maybe<T> max(Iterable<? extends T> items, Ordering<? super T> ordering) {
        Iterator<? extends T> iterator = items.iterator();
        return iterator.hasNext() ? Maybe.of(ordering.max(iterator)) : Maybe.<T>not();
    }

    public int sum(Function<? super T, Integer> valueComputer) {
        return sum(delegate(), valueComputer);
    }

    public static <T> int sum(Iterable<T> delegate, Function<? super T, Integer> valueComputer) {
        // yes, we could do some fancy functional foldLeft here ...  but this'll be faster
        int sum = 0;
        for (T item : delegate) {
            sum += valueComputer.apply(item);
        }

        return sum;
    }

    public String join(String separator) {
        return join(delegate(), separator);
    }

    public static String join(Iterable<?> items, String separator) {
        return Joiner.on(separator).join(items);
    }

    public FunIterable<T> tail() {
        return tail(delegate());
    }

    public Maybe<T> last() {
        return last(delegate());
    }

    public static <T> Maybe<T> last(Iterable<T> delegate) {
        return Maybe.of(Iterables.getLast(delegate, null));
    }

    public static <T> FunIterable<T> tail(Iterable<T> delegate) {
        return Iterables.isEmpty(delegate) ? FunctionalIterable.<T>empty() : new FunctionalIterable<T>(Iterables.skip(delegate, 1));
    }

    public <O> FunIterable<O> map(Function<? super T, ? extends O> transformer) {
        return map(delegate(), transformer);
    }

    public <K, V> FunPairs<K, V> mapPairs(Function<? super T, ? extends Entry<? extends K, ? extends V>> transformer) {
        return new FunctionalPairs<K, V>(Iterables.transform(delegate(), transformer));
    }

    public <O> FunIterable<O> parallelMap(final ExecutorService threadPool, Function<? super T, ? extends O> transformer) {
        return toParallelFutures(threadPool, transformer).map(FunFutures.<O>getFromFuture());
    }

    public <O> FunIterable<O> parallelFlatMap(ExecutorService threadPool, Function<? super T, ? extends Iterable<? extends O>> transformer) {
        return concat(parallelMap(threadPool, transformer));
    }

    public <O> FunList<ListenableFuture<O>> toParallelFutures(final ExecutorService threadPool, final Function<? super T, ? extends O> transformer) {
        checkState(threadPool != activeParallelThreadPool.get(), "Tried to execute nested parallel traversals in the same thread-pool. This can cause deadlock! Restructure the algorithm, or use a different threadpool.");
        final ListeningExecutorService listeningPool = MoreExecutors.listeningDecorator(threadPool);
        return map(new F<T, ListenableFuture<O>>() { public ListenableFuture<O> apply(final T from) {
            return listeningPool.submit(new ParallelMapTask<T, O>(threadPool, from, transformer));
        } }).toList();
    }

    public static <I, O> F<Iterable<? extends I>, FunIterable<O>> mapper(final Function<? super I, ? extends O> transformer) {
        return new F<Iterable<? extends I>, FunIterable<O>>() {
            public FunIterable<O> apply(Iterable<? extends I> from) {
                return Functional.map(from, transformer);
            }
        };
    }
    public static <I, O> F<Iterable<? extends I>, FunIterable<O>> flatMapper(final Function<? super I, ? extends Iterable<O>> transformer) {
        return new F<Iterable<? extends I>, FunIterable<O>>() {
            public FunIterable<O> apply(Iterable<? extends I> from) {
                return Functional.flatMap(from, transformer);
            }
        };
    }
    public static <T> F<Iterable<? extends T>, FunIterable<T>> filterer(final Predicate<? super T> filter) {
        return new F<Iterable<? extends T>, FunIterable<T>>() {
            @Override
            public FunIterable<T> apply(Iterable<? extends T> input) {
                return Functional.filter(input, filter);
            }
        };
    }
    public static <I, O> F<Iterable<? extends I>, O> folder(final O input, final Function2<? super O, ? super I, ? extends O> folder) {
        return new F<Iterable<? extends I>, O>() {
            @Override
            public O apply(Iterable<? extends I> iterable) {
                return foldLeft(input, iterable, folder);
            }
        };
    }

    public static <T> Sink<Iterable<? extends T>> foreacher(final Consumer<T> visitor) {
        return new Sink<Iterable<? extends T>>() {
            @Override
            public void handle(Iterable<? extends T> value) {
                foreach(value, visitor);
            }
        };
    }
    public static F<Iterable<?>, String> joiner(String separator) {
        return joiner(Joiner.on(separator));
    }

    public static F<Iterable<?>, String> joiner(final Joiner joiner) {
        return new F<Iterable<?>, String>() {
            @Override
            public String apply(Iterable<?> input) {
                return joiner.join(input);
            }
        };
    }

    public static <T> F<Iterable<? extends T>, Maybe<T>> header() {
        return new F<Iterable<? extends T>, Maybe<T>>() {
            @Override
            public Maybe<T> apply(Iterable<? extends T> input) {
                return head(input);
            }
        };
    }

    public static <I, O> FunIterable<O> map(Iterable<I> iterable, Function<? super I, ? extends O> transformer) {
        return new FunctionalIterable<O>(Iterables.transform(iterable, transformer));
    }

    public <O> FunIterable<O> flatMap(Function<? super T, ? extends Iterable<O>> transformer) {
        return flatMap(delegate(), transformer);
    }

    public <K, V> FunPairs<K, V> flatMapPairs(Function<? super T, ? extends Iterable<? extends Entry<? extends K, ? extends V>>> entryBuilder) {
        return new FunctionalPairs<K, V>(Iterables.concat(Iterables.transform(delegate(), entryBuilder)));
    }

    public FunIterable<T> foreach(Consumer<? super T> visitor) {
        foreach(delegate(), visitor);
        return this;
    }

    public static <T> void foreach(Iterable<T> items, Consumer<? super T> visitor) {
        for (T item : items) {
            visitor.handle(item);
        }
    }

    public static <I, O> FunIterable<O> flatMap(Iterable<I> iterable, Function<? super I, ? extends Iterable<O>> transformer) {
        return concat(Iterables.transform(iterable, transformer));
    }

    public FunIterable<T> cons(T firstElement) {
        return prepend(firstElement, delegate());
    }

    @Override
    public FunIterable<T> append(T lastElement) {
        return append(delegate(), lastElement);
    }

    @SuppressWarnings("unchecked")
    public FunIterable<T> plus(Iterable<? extends T> moreElements) {
        if (moreElements instanceof FunIterable) moreElements = ((FunIterable<? extends T>) moreElements).delegate();
        return concat(delegate(), moreElements);
    }

    public FunIterable<T> minus(Collection<?> excluded) {
        return difference(delegate(), excluded);
    }

    public static <T> FunIterable<T> difference(Iterable<T> items, Collection<?> excluded) {
        return filter(items, Predicates.not(Predicates.in(excluded)));
    }

    public static <T> FunIterable<T> concat(Iterable<? extends Iterable<? extends T>> inputs) {
        return new FunctionalIterable<T>(Iterables.concat(inputs));
    }

    public static <T> FunIterable<T> concat(Iterable<? extends T> input1, Iterable<? extends T> input2) {
        return new FunctionalIterable<T>(Iterables.concat(input1, input2));
    }

    private static final FBuilder<Iterable> FB = FBuilder.on(Iterable.class);
    private static final F GET_ITERATOR = FB.returning(FB.input.iterator());

    @SuppressWarnings("unchecked")
    public static <T> F<Iterable<? extends T>, Iterator<T>> iteratorGetter() {
        return GET_ITERATOR;
    }

    public static <T> FunIterable<T> interleave(final Iterable<? extends Iterable<? extends T>> streams) {
        return new AbstractFunIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return interleavedIterator(Iterables.transform(streams, Functional.<T>iteratorGetter()));
            }
        };
    }

    public static <T> Iterator<T> interleavedIterator(final Iterable<? extends Iterator<? extends T>> parts) {
        return new AbstractIterator<T>() {
            private final Iterator<Iterator<? extends T>> cycle = Iterators.cycle(Lists.newArrayList(parts));

            @Override
            protected T computeNext() {
                while (cycle.hasNext()) {
                    Iterator<? extends T> current = cycle.next();
                    if (current.hasNext()) {
                        return current.next();
                    } else {
                        cycle.remove();
                    }
                }
                return endOfData();
            }
        };
    }

    public static <T> FunIterable<T> prepend(T first, Iterable<? extends T> rest) {
        return new FunctionalIterable<T>(PrependedIterable.of(first, rest));
    }

    public static <T> FunIterable<T> append(Iterable<? extends T> init, T last) {
        return new FunctionalIterable<T>(AppendedIterable.of(init, last));
    }

    public FunIterable<T> filter(Predicate<? super T> predicate) {
        return filter(delegate(), predicate);
    }

    public <U> FunIterable<U> filter(Class<U> filteredClass) {
        return filter(delegate(), filteredClass);
    }

    /**
     * Intended to be used ONLY for "widening" conversions
     * (eg, casting a FunIterable&lt;Fish&gt; to a FunIterable&lt;Animal&gt;).
     *
     * Java, unfortunately, does not support contravariant type-restrictions; ideally, the signature
     * for this method would be:
     * <pre>
     * {@code <U super T> FunIterable<U> cast();}
     * </pre>
     * @param <U>
     * @return
     */
    public <U> FunIterable<U> cast() {
        return Reflect.blindCast(this);
    }

    public FunIterable<T> compact() {
        return compact(delegate());
    }

    public static <T> FunIterable<T> compact(Iterable<T> iterable) {
        return filter(iterable, Predicates.notNull());
    }

    public int count(Predicate<? super T> predicate) {
        return count(delegate(), predicate);
    }

    public static <T> int count(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterables.size(Iterables.filter(iterable, predicate));
    }

    public static <T, U> FunIterable<U> filter(Iterable<T> iterable, Class<U> filteredClass) {
        return new FunctionalIterable<U>(Iterables.filter(iterable, filteredClass));
    }

    @SuppressWarnings({"unchecked"})
    public static <T> FunIterable<T> filter(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        return new FunctionalIterable<T>(Iterables.filter((Iterable<T>) iterable, predicate));
    }

    @SuppressWarnings({"unchecked"})
    public FunIterable<T> toSortedCopy() {
        return (FunIterable<T>) sort((Iterable<? extends Comparable>)delegate(), Ordering.natural());
    }

    public FunIterable<T> toSortedCopy(Ordering<? super T> ordering) {
        return sort(delegate(), ordering);
    }

    public static <T> FunIterable<T> sort(Iterable<T> iterable, Ordering<? super T> ordering) {
        return new FunctionalIterable<T>(ordering.<T>sortedCopy(iterable));
    }

    public boolean any(Predicate<? super T> predicate) {
        return Iterables.any(delegate(), predicate);
    }

    public boolean all(Predicate<? super T> predicate) {
        return Iterables.all(delegate(), predicate);
    }

    public Maybe<T> find(Predicate<? super T> predicate) {
        return find(delegate(), predicate);
    }

    public static <T> Maybe<T> find(Iterable<T> iterable, Predicate<? super T> predicate) {
        for (T item : iterable) {
            if (predicate.apply(item)) return Maybe.definitely(item);
        }
        return Maybe.not();
    }

    public <U> FunPairs<T,U> zip(final Iterable<U> second) {
        return zip(delegate(), second);
    }

    public FunPairs<T, Integer> zipWithIndex() {
        return zipWithIndex(delegate());
    }

    public <U> FunIterable<U> mapWithIndex(Function2<? super T, Integer, ? extends U> visitor) {
        return mapWithIndex(delegate(), visitor);
    }

    public static <T, U> FunIterable<U> mapWithIndex(Iterable<T> delegate, Function2<? super T, Integer, ? extends U> visitor) {
        return zipWith(delegate, IncrementingIterable.countFromZero(), visitor);
    }

    public static <T, U> FunPairs<T, U> zip(final Iterable<? extends T> first, final Iterable<? extends U> second) {
        return new FunctionalPairs.ZippedPairs<T, U>(first, second);
    }

    public static <T> FunPairs<T, Integer> zipWithIndex(Iterable<T> items) {
        return zip(items, IncrementingIterable.countFromZero());
    }


    public <U, V> FunIterable<V> zipWith(Iterable<U> other, Function2<T, U, V> visitor) {
        return zipWith(delegate(), other, visitor);
    }

    public static <T, U, V> FunIterable<V> zipWith(final Iterable<? extends T> first, final Iterable<? extends U> second, final Function2<? super T, ? super U, ? extends V> visitor) {
        return new FunctionalIterable<V>(ZippedIterable.of(first, second, visitor));
    }

    public FunList<T> toList() {
        return FunctionalList.extend(ImmutableList.copyOf(delegate()));
    }

    public MutableFunList<T> toMutableList() {
        return MutableFunList.newMutableFunList(delegate());
    }

    public LazyReference<FunList<T>> lazyListSupplier() {
        return new LazyReference<FunList<T>>() { protected FunList<T> supplyValue() {
            return toList();
        } };
    }

    public FunSet<T> toSet() {
        return FunctionalSet.copyOf(delegate());
    }

    public MutableFunSet<T> toMutableSet() {
        return MutableFunSet.newMutableFunSet(delegate());
    }

    public <B extends ImmutableCollection.Builder<? super T>> B addTo(B builder) {
        builder.addAll(delegate());
        return builder;
    }

    public <I> I foldLeft(I input, Function2<? super I, ? super T, ? extends I> folder) {
        return foldLeft(input, delegate(), folder);
    }

    public Maybe<T> reduce(Function2<? super T, ? super T, ? extends T> reducer) {
        Iterator<T> iterator = iterator();
        if (!iterator.hasNext()) return Maybe.not();
        T reduced = iterator.next();
        while (iterator.hasNext()) {
            reduced = reducer.apply(reduced, iterator.next());
        }
        return Maybe.definitely(reduced);
    }

    public static <I, T> I foldLeft(I input, Iterable<T> iterable, Function2<? super I, ? super T, ? extends I> folder) {
        I iter = input;
        for (T t : iterable) iter = folder.apply(iter, t);
        return iter;
    }

    public FunIterable<T> takeWhile(final Predicate<? super T> predicate) {
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
                        return predicate.apply(next) ? next : endOfData();
                    }
                };
            }
        });
    }

    public FunIterable<T> limit(int maxElements) {
        return new FunctionalIterable<T>(Iterables.limit(delegate(), maxElements));
    }

    public FunIterable<T> dropWhile(final Predicate<? super T> predicate) {
        return dropWhile(delegate(), predicate);
    }

    public FunIterable<T> unique() {
        return filter(Pred.newDeduplicator());
    }
    
    public FunIterable<T> skip(int skippedElements) {
        return new FunctionalIterable<T>(Iterables.skip(delegate(), skippedElements));
    }

    public static <T> FunIterable<T> dropWhile(final Iterable<T> source, final Predicate<? super T> predicate) {
        return filter(source, new Predicate<T>() {
            boolean stillDropping = true;

            public boolean apply(T input) {
                if (stillDropping) stillDropping = predicate.apply(input);
                return !stillDropping;
            }
        });
    }

    public <V> FunPairs<T, V> asKeysTo(final Function<? super T, ? extends V> valueComputer) {
        return new FunctionalPairs<T,V>(Iterables.transform(delegate(), new F<T, Entry<? extends T, ? extends V>>() {
            public Entry<? extends T, ? extends V> apply(T input) {
                return Pair.of(input, valueComputer.apply(input));
            }
        }));
    }

    public <V> FunPairs<T, V> asKeysToFlattened(final Function<? super T, ? extends Iterable<? extends V>> valuesComputer) {
        return flatMapPairs(new F<T, Iterable<Pair<T, V>>>() {
            public Iterable<Pair<T, V>> apply(T input) {
                return Iterables.transform(valuesComputer.apply(input), Pair.<T, V>creator().bindFirst(input));
            }
        });
    }

    public <K> FunPairs<K, T> asValuesFrom(final Function<? super T, ? extends K> keyComputer) {
        return new FunctionalPairs<K,T>(Iterables.transform(delegate(), new F<T, Entry<? extends K, ? extends T>>() {
            public Entry<? extends K, ? extends T> apply(T input) {
                return Pair.of(keyComputer.apply(input), input);
            }
        }));
    }

    public <K> FunPairs<K, T> asValuesFromFlattened(final Function<? super T, ? extends Iterable<? extends K>> keysComputer) {
        return flatMapPairs(new F<T, Iterable<Pair<K, T>>>() { public Iterable<Pair<K, T>> apply(T input) {
            return Iterables.transform(keysComputer.apply(input), Pair.<K,T>creator().bindSecond(input));
        } });
    }

    public <K> ImmutableListMultimap<K, T> groupBy(Function<? super T, K> mapper) {
        return groupBy(delegate(), mapper);
    }

    public FunIterable<? extends FunIterable<T>> partition(int size) {
        return partition(delegate(), size);
    }

    public static <T> FunIterable<FunIterable<T>> partition(Iterable<T> items, int size) {
        return map(Iterables.partition(items, size), Functional.<T>extender());
    }

    public T getOnlyElement() {
        return Iterables.getOnlyElement(delegate());
    }

    public static <K, V> ImmutableListMultimap<K, V> groupBy(Iterable<V> items, Function<? super V, K> mapper) {
        return Multimaps.index(items, mapper);
    }

    public <U> FunPairs<T, U> crossProduct(Iterable<U> innerItems) {
        return crossProduct(delegate(), innerItems);
    }

    /**
     * Just iterates the sequence, triggering any side-effects that may be implied by its construction.
     */
    public void run() {
        run(delegate());
    }

    public static void run(Iterable<?> delegate) {
        final Iterator<?> iterator = delegate.iterator();
        while (iterator.hasNext()) iterator.next();
    }

    public static <T, U> FunPairs<T, U> crossProduct(Iterable<T> outerItems, final Iterable<U> innerItems) {
        return new FunctionalPairs<T, U>(
                Iterables.concat(
                        Iterables.transform(outerItems, new F<T, Iterable<Pair<T, U>>>() { public Iterable<Pair<T, U>> apply(T outerItem) {
                            return Iterables.transform(innerItems, FunctionalPairs.<T, U>entryCreator().bindFirst(outerItem));
                        } })
                )
        );
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    @Override public int hashCode() {
        return delegate().hashCode();
    }

    static EmptyIterable EMPTY = new EmptyIterable();
    @SuppressWarnings({"unchecked"})
    static class EmptyIterable<T> implements FunIterable<T> {
        private static final LazyReference LAZY_LIST_SUPPLIER = new LazyReference() {
            public Object get() { return emptyList(); }

            protected Object supplyValue() { throw new UnsupportedOperationException(".supplyValue has not been implemented"); }
        };
        private static final FunctionalPairs.EmptyPairs NO_PAIRS = FunctionalPairs.EMPTY;

        public Iterable delegate() {
            return ImmutableList.of();
        }

        public Maybe head() {
            return Maybe.not();
        }

        public EmptyIterable tail() {
            return this;
        }

        public Maybe last() {
            return Maybe.not();
        }

        public FunList toParallelFutures(ExecutorService threadPool, Function transformer) {
            return emptyList();
        }

        public int count(Predicate predicate) {
            return 0;
        }

        public EmptyIterable foreach(Consumer visitor) {
            return this;
        }

        public FunIterable map(Function transformer) {
            return this;
        }

        public FunIterable flatMap(Function transformer) {
            return this;
        }

        public FunIterable parallelMap(ExecutorService threadPool, Function transformer) {
            return this;
        }

        public FunIterable filter(Predicate predicate) {
            return this;
        }

        public FunIterable filter(Class filteredClass) {
            return this;
        }

        public FunIterable cast() {
            return this;
        }

        public FunIterable toSortedCopy(Ordering ordering) {
            return this;
        }

        public FunIterable toSortedCopy() {
            return this;
        }

        public boolean any(Predicate predicate) {
            return false;
        }

        public boolean all(Predicate predicate) {
            return true;
        }

        public Maybe find(Predicate predicate) {
            return Maybe.not();
        }

        public FunIterable zipWith(Iterable other, Function2 visitor) {
            return this;
        }

        public FunPairs zip(Iterable second) {
            return NO_PAIRS;
        }

        public FunIterable mapWithIndex(Function2 visitor) {
            return this;
        }

        public FunPairs zipWithIndex() {
            return NO_PAIRS;
        }

        public Maybe<T> reduce(Function2<? super T, ? super T, ? extends T> reducer) {
            return Maybe.not();
        }

        public FunPairs asKeysToFlattened(Function valuesComputer) {
            return NO_PAIRS;
        }

        public FunPairs asValuesFromFlattened(Function keysComputer) {
            return NO_PAIRS;
        }

        public Object foldLeft(Object input, Function2 folder) {
            return input;
        }

        public FunIterable takeWhile(Predicate predicate) {
            return this;
        }

        public FunIterable dropWhile(Predicate predicate) {
            return this;
        }

        public ImmutableListMultimap groupBy(Function mapper) {
            return ImmutableListMultimap.of();
        }

        public FunIterable partition(int size) {
            return this;
        }

        public FunIterable compact() {
            return this;
        }

        public FunIterable unique() {
            return this;
        }
        
        public FunIterable limit(int maxElements) {
            return this;
        }

        public FunIterable skip(int skippedElements) {
            return this;
        }

        public ImmutableCollection.Builder addTo(ImmutableCollection.Builder builder) {
            return builder;
        }

        public T getOnlyElement() {
            throw new NoSuchElementException();
        }

        public FunPairs asKeysTo(Function mapper) {
            return NO_PAIRS;
        }

        public FunPairs asValuesFrom(Function mapper) {
            return NO_PAIRS;
        }

        public FunList toList() {
            return FunctionalList.EMPTY;
        }

        public MutableFunList<T> toMutableList() {
            return MutableFunList.newMutableFunList();
        }

        public FunSet toSet() {
            return FunctionalSet.EMPTY;
        }

        public MutableFunSet<T> toMutableSet() {
            return MutableFunSet.newMutableFunSet();
        }

        public int size() {
            return 0;
        }

        public FunIterable parallelFlatMap(ExecutorService threadPool, Function transformer) {
            return this;
        }

        public FunPairs flatMapPairs(Function entryBuilder) {
            return NO_PAIRS;
        }

        public FunPairs mapPairs(Function transformer) {
            return NO_PAIRS;
        }

        public boolean isEmpty() {
            return true;
        }

        public Maybe min(Ordering ordering) {
            return Maybe.not();
        }

        public Maybe max(Ordering ordering) {
            return Maybe.not();
        }

        public Maybe minBy(Function valueComputer) {
            return Maybe.not();
        }

        public Maybe minBy(Ordering ordering, Function valueComputer) {
            return Maybe.not();
        }

        public Maybe maxBy(Function valueComputer) {
            return Maybe.not();
        }

        public Maybe maxBy(Ordering ordering, Function valueComputer) {
            return Maybe.not();
        }

        public int sum(Function valueAccessor) {
            return 0;
        }

        public String join(String separator) {
            return "";
        }

        public Iterator iterator() {
            return Iterators.emptyIterator();
        }

        public LazyReference lazyListSupplier() {
            return LAZY_LIST_SUPPLIER;
        }

        @Override
        public FunIterable<T> cons(T firstElement) {
            return funListOf(firstElement);
        }

        @Override
        public FunIterable<T> append(T firstElement) {
            return cons(firstElement);
        }

        public FunIterable plus(Iterable more) {
            return FunctionalPairs.extendPairs(more);
        }

        public FunPairs crossProduct(Iterable innerItems) {
            return NO_PAIRS;
        }

        public FunIterable minus(Collection excluded) {
            return this;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        @Override public boolean equals(@Nullable Object object) {
            return object == this || (object instanceof Iterable && (Iterables.isEmpty((Iterable)object)));
        }

        @Override public int hashCode() {
            return delegate().hashCode();
        }

        @Override
        public void run() {
        }
    }


    static class ZippedIterable<T, U, V> implements Iterable<V> {
        private final Iterable<? extends T> first;
        private final Iterable<? extends U> second;
        private final Function2<? super T, ? super U, ? extends V> visitor;

        static <T,U,V> ZippedIterable<T,U,V> of(Iterable<? extends T> first, Iterable<? extends U> second, Function2<? super T, ? super U, ? extends V> visitor) {
            return new ZippedIterable<T,U,V>(first, second, visitor);
        }

        private ZippedIterable(Iterable<? extends T> first, Iterable<? extends U> second, Function2<? super T, ? super U, ? extends V> visitor) {
            this.first = first;
            this.second = second;
            this.visitor = visitor;
        }

        public Iterator<V> iterator() {
            return new ZipWithIterator<T,U,V>(first.iterator(), second.iterator(), visitor);
        }

        private static class ZipWithIterator<T, U, V> extends AbstractIterator<V> {
            private final Iterator<? extends T> firstIterator;
            private final Iterator<? extends U> secondIterator;
            private final Function2<? super T, ? super U, ? extends V> visitor;

            public ZipWithIterator(Iterator<? extends T> firstIterator, Iterator<? extends U> secondIterator, Function2<? super T, ? super U, ? extends V> visitor) {
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

    private static class ParallelMapTask<I, O> implements Callable<O> {
        private final ExecutorService threadPool;
        private final I input;
        private final Function<? super I, ? extends O> transformer;

        public ParallelMapTask(ExecutorService threadPool, I input, Function<? super I, ? extends O> transformer) {
            this.threadPool = threadPool;
            this.input = input;
            this.transformer = transformer;
        }

        public O call() throws Exception {
            checkState(activeParallelThreadPool.compareAndSet(null, threadPool), "activeThreadpool was already populated??");
            try {
                return transformer.apply(input);
            } finally {
                activeParallelThreadPool.remove();
            }
        }
    }

}

class FunctionalIterable<T> extends Functional<T> {
    private final Iterable<T> delegate;

    public FunctionalIterable(Iterable<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Iterable<T> delegate() {
        return delegate;
    }
}

abstract class AbstractFunIterable<T> extends Functional<T> {
    @Override
    public Iterable<T> delegate() {
        return this;
    }

    public abstract Iterator<T> iterator();
}
