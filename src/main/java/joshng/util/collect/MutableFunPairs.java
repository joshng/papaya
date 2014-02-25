package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import joshng.util.blocks.Consumer2;
import joshng.util.blocks.F2;
import joshng.util.blocks.Function2;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * User: josh
 * Date: 4/17/12
 * Time: 12:47 PM
 */
public class MutableFunPairs<K, V> extends MutableFunList<Map.Entry<K, V>> implements FunPairs<K, V> {
    public MutableFunPairs(int initialCapacity) {
        super(initialCapacity);
    }

    public MutableFunPairs() {
    }

    public MutableFunPairs(Collection<? extends Map.Entry<K, V>> c) {
        super(c);
    }

    @Override
    public Maybe.Pair<K, V> head() {
        return (Maybe.Pair<K, V>) super.head();
    }

    @Override
    public Maybe.Pair<K, V> last() {
        return (Maybe.Pair<K, V>) super.last();
    }

    @Override
    public Maybe.Pair<K, V> find(Predicate<? super Map.Entry<K, V>> predicate) {
        return (Maybe.Pair<K, V>) super.find(predicate);
    }

    @Override
    public Maybe.Pair<K, V> min(Ordering<? super Map.Entry<K, V>> ordering) {
        return (Maybe.Pair<K, V>) super.min(ordering);
    }

    @Override
    public Maybe.Pair<K, V> max(Ordering<? super Map.Entry<K, V>> ordering) {
        return (Maybe.Pair<K, V>) super.max(ordering);
    }

    @Override
    public Maybe.Pair<K, V> minBy(Function<? super Map.Entry<K, V>, ? extends Comparable> valueComputer) {
        return (Maybe.Pair<K, V>) super.minBy(valueComputer);
    }

    @Override
    public <N> Maybe.Pair<K, V> minBy(Ordering<? super N> ordering, Function<? super Map.Entry<K, V>, ? extends N> valueComputer) {
        return (Maybe.Pair<K, V>) super.minBy(ordering, valueComputer);
    }

    @Override
    public Maybe.Pair<K, V> maxBy(Function<? super Map.Entry<K, V>, ? extends Comparable> valueComputer) {
        return (Maybe.Pair<K, V>) super.maxBy(valueComputer);
    }

    @Override
    public <N> Maybe.Pair<K, V> maxBy(Ordering<? super N> ordering, Function<? super Map.Entry<K, V>, ? extends N> valueComputer) {
        return (Maybe.Pair<K, V>) super.maxBy(ordering, valueComputer);
    }

    @Override
    public Maybe.Pair<K, V> reduce(Function2<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer) {
        return (Maybe.Pair<K, V>) super.reduce(reducer);
    }

    public FunIterable<K> keys() {
        return getExtended().keys();
    }

    public FunIterable<V> values() {
        return getExtended().values();
    }

    public <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer) {
        return getExtended().mapKeys(keyTransformer);
    }

    public <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer) {
        return getExtended().mapValues(valueTransformer);
    }

    public <K2> FunPairs<K2, V> flatMapKeys(Function<? super K, ? extends Iterable<? extends K2>> keyTransformer) {
        return getExtended().flatMapKeys(keyTransformer);
    }

    public <V2> FunPairs<K, V2> flatMapValues(Function<? super V, ? extends Iterable<? extends V2>> valueTransformer) {
        return getExtended().flatMapValues(valueTransformer);
    }

    public <O> FunIterable<O> map2(F2<? super K, ? super V, ? extends O> transformer) {
        return getExtended().map2(transformer);
    }

    public void foreach2(Consumer2<? super K, ? super V> visitor) {
        getExtended().foreach2(visitor);
    }

    public FunPairs<K, V> filter(Predicate<? super Map.Entry<K, V>> predicate) {
        return getExtended().filter(predicate);
    }

    public FunPairs<K, V> filterKeys(Predicate<? super K> predicate) {
        return getExtended().filterKeys(predicate);
    }

    public FunPairs<K, V> uniqueKeys() {
        return getExtended().uniqueKeys();
    }

    public FunPairs<K, V> filterValues(Predicate<? super V> predicate) {
        return getExtended().filterValues(predicate);
    }

    public FunPairs<K, V> uniqueValues() {
        return getExtended().uniqueValues();
    }

    public FunPairs<K, V> toSortedCopy(Ordering<? super Map.Entry<K, V>> ordering) {
        return getExtended().toSortedCopy(ordering);
    }

    public FunPairs<K, V> sortByKeys(Ordering<? super K> keyOrdering) {
        return getExtended().sortByKeys(keyOrdering);
    }

    public FunPairs<K, V> sortByValues(Ordering<? super V> valueOrdering) {
        return getExtended().sortByValues(valueOrdering);
    }

    public Maybe.Pair<K, V> minByKeys(Ordering<? super K> keyOrdering) {
        return getExtended().minByKeys(keyOrdering);
    }

    public Maybe.Pair<K, V> maxByKeys(Ordering<? super K> keyOrdering) {
        return getExtended().maxByKeys(keyOrdering);
    }

    public Maybe.Pair<K, V> minByValues(Ordering<? super V> valueOrdering) {
        return getExtended().minByValues(valueOrdering);
    }

    public Maybe.Pair<K, V> maxByValues(Ordering<? super V> valueOrdering) {
        return getExtended().maxByValues(valueOrdering);
    }

    public <K2> Maybe.Pair<K, V> minByKeys(Ordering<? super K2> ordering, Function<? super K, K2> keyValueComputer) {
        return getExtended().minByKeys(ordering, keyValueComputer);
    }

    public <K2> Maybe.Pair<K, V> maxByKeys(Ordering<? super K2> ordering, Function<? super K, K2> keyValueComputer) {
        return getExtended().maxByKeys(ordering, keyValueComputer);
    }

    public <V2> Maybe.Pair<K, V> minByValues(Ordering<? super V2> ordering, Function<? super V, V2> valueComputer) {
        return getExtended().minByValues(ordering, valueComputer);
    }

    public <V2> Maybe.Pair<K, V> maxByValues(Ordering<? super V2> ordering, Function<? super V, V2> valueComputer) {
        return getExtended().maxByValues(ordering, valueComputer);
    }

    @Override
    public FunPairs<K, V> cons(Map.Entry<K, V> firstElement) {
        return getExtended().cons(firstElement);
    }

    @Override
    public FunPairs<K, V> append(Map.Entry<K, V> firstElement) {
        return getExtended().append(firstElement);
    }

    public FunPairs<K, V> plus(Iterable<? extends Map.Entry<K, V>> more) {
        return getExtended().plus(more);
    }

    public FunIterable<Map.Entry<K, V>> unique() {
        return getExtended().unique();
    }

    public <K2, V2> FunPairs<K2, V2> mapPairs2(F2<? super K, ? super V, ? extends Map.Entry<K2, V2>> transformer) {
        return getExtended().mapPairs2(transformer);
    }

    public <K2, V2> FunPairs<K2, V2> flatMapPairs2(F2<? super K, ? super V, ? extends Iterable<? extends Map.Entry<K2, V2>>> transformer) {
        return getExtended().flatMapPairs2(transformer);
    }

    public FunPairs<V, K> swap() {
        return getExtended().swap();
    }

    @Override
    public Maybe<V> findValueForKey(K key) {
        return getExtended().findValueForKey(key);
    }

    @Override
    public String joinPairs(String pairSeparator, String keyValueSeparator) {
        return getExtended().joinPairs(pairSeparator, keyValueSeparator);
    }

    @Override
    public FunPairs<K, V> limit(int maxElements) {
        return getExtended().limit(maxElements);
    }

    @Override
    public FunPairs<K, V> skip(int skippedElements) {
        return getExtended().skip(skippedElements);
    }

    @Override
    public FunIterable<FunPairs<K, V>> partition(int size) {
        return getExtended().partition(size);
    }

    public ImmutableMap<K, V> toMap() {
        return getExtended().toMap();
    }

    public ImmutableListMultimap<K, V> toMultimap() {
        return getExtended().toMultimap();
    }

    public ImmutableBiMap<K, V> toBimap() {
        return getExtended().toBimap();
    }

    public ComparingMap<K, V> toComparingMap(Comparator<? super V> ordering) {
        return getExtended().toComparingMap(ordering);
    }

    public HashMap<K, V> toMutableMap() {
        return getExtended().toMutableMap();
    }

    @Override
    protected FunPairs<K, V> getExtended() {
        return new FunctionalPairs<K, V>(this);
    }
}
