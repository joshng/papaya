package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.Consumer;
import joshng.util.blocks.Function2;
import joshng.util.concurrent.LazyReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * User: josh
 * Date: 4/17/12
 * Time: 12:37 PM
 */
public class MutableFunSet<T> extends HashSet<T> implements FunIterable<T> {
    public MutableFunSet() {
    }

    public MutableFunSet(Collection<? extends T> c) {
        super(c);
    }

    public MutableFunSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MutableFunSet(int initialCapacity) {
        super(initialCapacity);
    }

    public static <T> MutableFunSet<T> newMutableFunSet() {
        return new MutableFunSet<T>();
    }

    public static <T> MutableFunSet<T> newMutableFunSet(Iterable<T> items) {
        if (items instanceof Collection) return new MutableFunSet<T>((Collection<T>)items);
        MutableFunSet<T> result = newMutableFunSet();
        for (T item : items) {
            result.add(item);
        }
        return result;
    }
 
    public Maybe<T> head() {
        return getExtended().head();
    }

    public FunIterable<T> tail() {
        return getExtended().tail();
    }

    public Maybe<T> last() {
        return getExtended().last();
    }

    public <O> FunIterable<O> map(Function<? super T, ? extends O> transformer) {
        return getExtended().map(transformer);
    }

    public <K, V> FunPairs<K, V> mapPairs(Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> transformer) {
        return getExtended().mapPairs(transformer);
    }

    public <O> FunIterable<O> flatMap(Function<? super T, ? extends Iterable<O>> transformer) {
        return getExtended().flatMap(transformer);
    }

    public <K, V> FunPairs<K, V> flatMapPairs(Function<? super T, ? extends Iterable<? extends Map.Entry<? extends K, ? extends V>>> entryBuilder) {
        return getExtended().flatMapPairs(entryBuilder);
    }

    public <O> FunIterable<O> parallelMap(ExecutorService threadPool, Function<? super T, ? extends O> transformer) {
        return getExtended().parallelMap(threadPool, transformer);
    }

    public <O> FunIterable<O> parallelFlatMap(ExecutorService threadPool, Function<? super T, ? extends Iterable<? extends O>> transformer) {
        return getExtended().parallelFlatMap(threadPool, transformer);
    }

    public <O> FunList<ListenableFuture<O>> toParallelFutures(ExecutorService threadPool, Function<? super T, ? extends O> transformer) {
        return getExtended().toParallelFutures(threadPool, transformer);
    }

    public FunIterable<T> foreach(Consumer<? super T> visitor) {
        return getExtended().foreach(visitor);
    }

    public FunIterable<T> filter(Predicate<? super T> predicate) {
        return getExtended().filter(predicate);
    }

    public <U> FunIterable<U> filter(Class<U> filteredClass) {
        return getExtended().filter(filteredClass);
    }

    public <S> FunIterable<S> cast() {
        return getExtended().cast();
    }

    public int count(Predicate<? super T> predicate) {
        return getExtended().count(predicate);
    }

    public FunIterable<T> toSortedCopy(Ordering<? super T> ordering) {
        return getExtended().toSortedCopy(ordering);
    }

    public FunIterable<T> toSortedCopy() {
        return getExtended().toSortedCopy();
    }

    public boolean any(Predicate<? super T> predicate) {
        return getExtended().any(predicate);
    }

    public boolean all(Predicate<? super T> predicate) {
        return getExtended().all(predicate);
    }

    public Maybe<T> find(Predicate<? super T> predicate) {
        return getExtended().find(predicate);
    }

    public <U> FunPairs<T, U> zip(Iterable<U> values) {
        return getExtended().zip(values);
    }

    public <U, V> FunIterable<V> zipWith(Iterable<U> other, Function2<T, U, V> visitor) {
        return getExtended().zipWith(other, visitor);
    }

    public FunPairs<T, Integer> zipWithIndex() {
        return getExtended().zipWithIndex();
    }

    public <U> FunIterable<U> mapWithIndex(Function2<? super T, Integer, ? extends U> visitor) {
        return getExtended().mapWithIndex(visitor);
    }

    public <I> I foldLeft(I input, Function2<? super I, ? super T, ? extends I> folder) {
        return getExtended().foldLeft(input, folder);
    }

    public Maybe<T> reduce(Function2<? super T, ? super T, ? extends T> reducer) {
        return getExtended().reduce(reducer);
    }

    public FunIterable<T> takeWhile(Predicate<? super T> predicate) {
        return getExtended().takeWhile(predicate);
    }

    public FunIterable<T> dropWhile(Predicate<? super T> predicate) {
        return getExtended().dropWhile(predicate);
    }

    public <V> FunPairs<T, V> asKeysTo(Function<? super T, ? extends V> valueComputer) {
        return getExtended().asKeysTo(valueComputer);
    }

    public <V> FunPairs<T, V> asKeysToFlattened(Function<? super T, ? extends Iterable<? extends V>> valuesComputer) {
        return getExtended().asKeysToFlattened(valuesComputer);
    }

    public <K> FunPairs<K, T> asValuesFrom(Function<? super T, ? extends K> keyComputer) {
        return getExtended().asValuesFrom(keyComputer);
    }

    public <K> FunPairs<K, T> asValuesFromFlattened(Function<? super T, ? extends Iterable<? extends K>> keysComputer) {
        return getExtended().asValuesFromFlattened(keysComputer);
    }

    public <K> ImmutableListMultimap<K, T> groupBy(Function<? super T, K> mapper) {
        return getExtended().groupBy(mapper);
    }

    public T getOnlyElement() {
        return getExtended().getOnlyElement();
    }

    public FunList<T> toList() {
        return getExtended().toList();
    }

    public MutableFunList<T> toMutableList() {
        return new MutableFunList<T>(this);
    }

    public FunSet<T> toSet() {
        return getExtended().toSet();
    }

    public MutableFunSet<T> toMutableSet() {
        return new MutableFunSet<T>(this);
    }

    public Maybe<T> min(Ordering<? super T> ordering) {
        return getExtended().min(ordering);
    }

    public Maybe<T> max(Ordering<? super T> ordering) {
        return getExtended().max(ordering);
    }

    public Maybe<T> minBy(Function<? super T, ? extends Comparable> valueComputer) {
        return getExtended().minBy(valueComputer);
    }

    public <V> Maybe<T> minBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
        return getExtended().minBy(ordering, valueComputer);
    }

    public Maybe<T> maxBy(Function<? super T, ? extends Comparable> valueComputer) {
        return getExtended().maxBy(valueComputer);
    }

    public <V> Maybe<T> maxBy(Ordering<? super V> ordering, Function<? super T, ? extends V> valueComputer) {
        return getExtended().maxBy(ordering, valueComputer);
    }

    public int sum(Function<? super T, Integer> valueComputer) {
        return getExtended().sum(valueComputer);
    }

    public String join(String separator) {
        return getExtended().join(separator);
    }

    public Iterable<T> delegate() {
        return this;
    }

    public LazyReference<FunList<T>> lazyListSupplier() {
        return getExtended().lazyListSupplier();
    }

    @Override
    public FunIterable<T> cons(T firstElement) {
        return getExtended().cons(firstElement);
    }

    @Override
    public FunIterable<T> append(T lastElement) {
        return getExtended().append(lastElement);
    }

    public FunIterable<T> plus(Iterable<? extends T> moreElements) {
        return getExtended().plus(moreElements);
    }

    public FunIterable<T> minus(Collection<?> excluded) {
        return getExtended().minus(excluded);
    }

    public <U> FunPairs<T, U> crossProduct(Iterable<U> innerItems) {
        return getExtended().crossProduct(innerItems);
    }

    public FunIterable<? extends FunIterable<T>> partition(int size) {
        return getExtended().partition(size);
    }

    public <B extends ImmutableCollection.Builder<? super T>> B addTo(B builder) {
        return getExtended().addTo(builder);
    }

    public FunIterable<T> compact() {
        return getExtended().compact();
    }

    public FunIterable<T> unique() {
        return getExtended().unique();
    }
    
    public FunIterable<T> limit(int maxElements) {
        return getExtended().limit(maxElements);
    }

    public FunIterable<T> skip(int skippedElements) {
        return getExtended().skip(skippedElements);
    }

    protected FunIterable<T> getExtended() {
        return new FunctionalIterable<T>(this);
    }

    @Override
    public void run() {
        Functional.run(this);
    }
}
