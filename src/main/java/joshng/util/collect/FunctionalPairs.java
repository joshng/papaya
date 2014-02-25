package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import joshng.util.blocks.Consumer2;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.blocks.Function2;
import joshng.util.blocks.Pred;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static joshng.util.Reflect.blindCast;
import static joshng.util.collect.MoreCollections.*;

/**
 * User: josh
 * Date: 3/15/12
 * Time: 9:33 AM
 */
public class FunctionalPairs<K,V> extends Functional<Entry<K,V>> implements FunPairs<K, V> {
    private final Iterable<Entry<K,V>> delegate;
    private static final F2 ENTRY_CREATOR = Pair.creator();

    public static <K,V> FunPairs<K,V> extendPairs(Iterable<? extends Entry<? extends K, ? extends V>> delegate) {
        if (delegate instanceof FunPairs) return blindCast(delegate);
        if (MoreCollections.isCollectionThatIsEmpty(delegate)) return Functional.emptyPairs();
        return new FunctionalPairs<>(delegate);
    }

    public static <K, V> FunPairs<K, V> extend(Map<K, V> map) {
        if (map.isEmpty()) return emptyPairs();
        return new FunMapEntries<>(map);
    }

    public static <K,V> F<Iterable<Entry<K, V>>, FunPairs<K,V>> pairExtender() {
        return new F<Iterable<Entry<K, V>>, FunPairs<K, V>>() { public FunPairs<K, V> apply(Iterable<Entry<K, V>> input) {
            return extendPairs(input);
        } };
    }

    public static <K,V> F<Map<K, V>, FunPairs<K,V>> mapExtender() {
        return new F<Map<K, V>, FunPairs<K, V>>() { public FunPairs<K, V> apply(Map<K, V> input) {
            return extend(input);
        } };
    }

    public static <K,V> F2<K, V, Pair<K,V>> entryCreator() {
        return blindCast(ENTRY_CREATOR);
    }

    @SuppressWarnings({"unchecked"})
    FunctionalPairs(Iterable<? extends Entry<? extends K, ? extends V>> delegate) {
        this.delegate = blindCast(delegate); // we never ADD anything to the Iterable, so this widening cast is safe
    }

    @Override
    public Iterable<Entry<K, V>> delegate() {
        return delegate;
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
    public Maybe.Pair<K, V> find(Predicate<? super Entry<K, V>> predicate) {
        return (Maybe.Pair<K, V>) super.find(predicate);
    }

    @Override
    public Maybe.Pair<K, V> reduce(Function2<? super Entry<K, V>, ? super Entry<K, V>, ? extends Entry<K, V>> reducer) {
        return (Maybe.Pair<K, V>) super.reduce(reducer);
    }

    public FunPairs<K, V> toSortedCopy(Ordering<? super Entry<K, V>> ordering) {
        return new FunctionalPairs<K,V>(ordering.<Entry<K,V>>sortedCopy(delegate));
    }

    @Override
    public Maybe.Pair<K, V> min(Ordering<? super Entry<K, V>> ordering) {
        return (Maybe.Pair<K, V>) super.min(ordering);
    }

    @Override
    public Maybe.Pair<K, V> max(Ordering<? super Entry<K, V>> ordering) {
        return (Maybe.Pair<K, V>) super.max(ordering);
    }

    @Override
    public Maybe.Pair<K, V> minBy(Function<? super Entry<K, V>, ? extends Comparable> valueComputer) {
        return (Maybe.Pair<K, V>) super.minBy(valueComputer);
    }

    @Override
    public <N> Maybe.Pair<K, V> minBy(Ordering<? super N> ordering, Function<? super Entry<K, V>, ? extends N> valueComputer) {
        return (Maybe.Pair<K, V>) super.minBy(ordering, valueComputer);
    }

    @Override
    public Maybe.Pair<K, V> maxBy(Function<? super Entry<K, V>, ? extends Comparable> valueComputer) {
        return (Maybe.Pair<K, V>) super.maxBy(valueComputer);
    }

    @Override
    public <N> Maybe.Pair<K, V> maxBy(Ordering<? super N> ordering, Function<? super Entry<K, V>, ? extends N> valueComputer) {
        return (Maybe.Pair<K, V>) super.maxBy(ordering, valueComputer);
    }

    public Maybe.Pair<K, V> minByKeys(Ordering<? super K> keyOrdering) {
        return (Maybe.Pair<K, V>) min(delegate, orderingByKey(keyOrdering));
    }

    public Maybe.Pair<K, V> maxByKeys(Ordering<? super K> keyOrdering) {
        return (Maybe.Pair<K, V>) max(delegate, orderingByKey(keyOrdering));
    }

    public FunPairs<K,V> sortByKeys(Ordering<? super K> keyOrdering) {
        return toSortedCopy(orderingByKey(keyOrdering));
    }

    private Ordering<Entry<? extends K, ?>> orderingByKey(Ordering<? super K> keyOrdering) {
        return keyOrdering.onResultOf(Pair.<K>getFirstFromPair());
    }

    public Maybe.Pair<K, V> minByValues(Ordering<? super V> valueOrdering) {
        return (Maybe.Pair<K, V>) min(delegate, orderingByValue(valueOrdering));
    }

    public Maybe.Pair<K, V> maxByValues(Ordering<? super V> valueOrdering) {
        return (Maybe.Pair<K, V>) max(delegate, orderingByValue(valueOrdering));
    }

    public <O> Maybe.Pair<K, V> minByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer) {
        return minBy(ordering, Pair.<K>getFirstFromPair().andThen(keyValueComputer));
    }

    public <O> Maybe.Pair<K, V> maxByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer) {
        return maxBy(ordering, Pair.<K>getFirstFromPair().andThen(keyValueComputer));
    }

    public <O> Maybe.Pair<K, V> minByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer) {
        return minBy(ordering, Pair.<V>getSecondFromPair().andThen(valueComputer));
    }

    public <O> Maybe.Pair<K, V> maxByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer) {
        return maxBy(ordering, Pair.<V>getSecondFromPair().andThen(valueComputer));
    }

    @Override
    public FunPairs<K, V> cons(Entry<K, V> firstElement) {
        return new FunctionalPairs<K, V>(PrependedIterable.<Entry<K,V>>of(firstElement, delegate));
    }

    public FunPairs<K, V> append(Entry<K, V> lastElement) {
        return new FunctionalPairs<K, V>(AppendedIterable.<Entry<K,V>>of(delegate, lastElement));
    }

    public FunPairs<K,V> sortByValues(Ordering<? super V> valueOrdering) {
        return toSortedCopy(orderingByValue(valueOrdering));
    }

    private Ordering<Entry<?, ? extends V>> orderingByValue(Ordering<? super V> valueOrdering) {
        return valueOrdering.onResultOf(Pair.<V>getSecondFromPair());
    }

    public <O> FunIterable<O> map2(F2<? super K, ? super V, ? extends O> transformer) {
        return map(transformer.<K,V>tupled());
    }

    public void foreach2(Consumer2<? super K, ? super V> visitor) {
        foreach(visitor);
    }

    public <K2, V2> FunPairs<K2, V2> mapPairs2(F2<? super K, ? super V, ? extends Entry<K2, V2>> transformer) {
        return mapPairs(transformer.<K,V>tupled());
    }

    public <K2, V2> FunPairs<K2, V2> flatMapPairs2(F2<? super K, ? super V, ? extends Iterable<? extends Entry<K2, V2>>> transformer) {
        return flatMapPairs(transformer.<K,V>tupled());
    }

    public FunPairs<V, K> swap() {
        return mapPairs(Pair.<K,V>swapper());
    }

    @Override
    public Maybe<V> findValueForKey(K key) {
        return find(Pair.<K>getFirstFromPair().resultEqualTo(key)).getValue();
    }

    @Override
    public String joinPairs(String pairSeparator, String keyValueSeparator) {
        return Joiner.on(pairSeparator).withKeyValueSeparator(keyValueSeparator).join(this);
    }

    public <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer) {
        return new FunctionalPairs<>(Iterables.transform(delegate, FunctionalPairs.<K, V, K2>keyMapper(keyTransformer)));
    }

    public <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer) {
        return new FunctionalPairs<>(Iterables.transform(delegate, FunctionalPairs.<K, V, V2>valueMapper(valueTransformer)));
    }

    public <K2> FunPairs<K2, V> flatMapKeys(Function<? super K, ? extends Iterable<? extends K2>> keyTransformer) {
        return new FunctionalPairs<>(Iterables.concat(Iterables.transform(delegate, FunctionalPairs.<K, V, K2>keyFlatMapper(keyTransformer))));
    }

    public <V2> FunPairs<K, V2> flatMapValues(Function<? super V, ? extends Iterable<? extends V2>> valueTransformer) {
        return new FunctionalPairs<>(Iterables.concat(Iterables.transform(delegate, FunctionalPairs.<K, V, V2>valueFlatMapper(valueTransformer))));
    }

    public FunPairs<K, V> filter(Predicate<? super Entry<K, V>> predicate) {
        return new FunctionalPairs<>(Iterables.filter(delegate, predicate));
    }

    public FunIterable<K> keys() {
        return extend(keysDelegate());
    }

    public FunIterable<V> values() {
        return extend(valuesDelegate());
    }

    public FunPairs<K, V> filterKeys(Predicate<? super K> predicate) {
        return filterKeys(delegate, predicate);
    }

    public FunPairs<K, V> uniqueKeys() {
        return filterKeys(Pred.newDeduplicator());
    }

    public static <K,V> FunPairs<K, V> filterKeys(Iterable<Entry<K, V>> entries, Predicate<? super K> predicate) {
        return new FunctionalPairs<>(Iterables.filter(entries, Pair.<K>getFirstFromPair().resultMatches(predicate)));
    }

    public FunPairs<K, V> filterValues(Predicate<? super V> predicate) {
        return filterValues(delegate, predicate);
    }

    public static <K,V> FunPairs<K, V> filterValues(Iterable<Entry<K, V>> entries, Predicate<? super V> predicate) {
        return new FunctionalPairs<>(Iterables.filter(entries, Pair.<V>getSecondFromPair().resultMatches(predicate)));
    }

    public FunPairs<K, V> uniqueValues() {
        return filterValues(Pred.newDeduplicator());
    }

    public FunPairs<K, V> plus(Iterable<? extends Entry<K, V>> more) {
        return new FunctionalPairs<>(Iterables.concat(delegate, more));
    }

    public FunPairs<K, V> limit(int maxElements) {
        return new FunctionalPairs<>(Iterables.limit(delegate, maxElements));
    }

    public FunPairs<K, V> skip(int skippedElements) {
        return new FunctionalPairs<>(Iterables.skip(delegate, skippedElements));
    }

    @Override
    public FunIterable<FunPairs<K, V>> partition(int size) {
        return Functional.map(Iterables.partition(delegate, size), FunctionalPairs.<K, V>pairExtender());
    }

    public ImmutableMap<K,V> toMap() {
        return immutableMapWithEntries(delegate);
    }

    public ImmutableListMultimap<K, V> toMultimap() {
        return multimapWithEntries(delegate);
    }

    public ImmutableBiMap<K, V> toBimap() {
        return bimapWithEntries(delegate);
    }

    public ComparingMap<K, V> toComparingMap(Comparator<? super V> ordering) {
        return ComparingMap.build(ordering, delegate);
    }

    public HashMap<K, V> toMutableMap() {
        HashMap<K,V> map = Maps.newHashMap();
        for (Entry<K, V> entry : delegate) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    protected Iterable<K> keysDelegate() {
        return Iterables.transform(delegate, Pair.<K>getFirstFromPair());
    }

    protected Iterable<V> valuesDelegate() {
        return Iterables.transform(delegate, Pair.<V>getSecondFromPair());
    }

    public static <K, V, V2> Pair<K, V2> mapValue(Entry<? extends K, ? extends V> pair, Function<? super V, ? extends V2> transformer) {
        return Pair.of(pair.getKey(), transformer.apply(pair.getValue()));
    }

    public static <K, V, K2> Pair<K2, V> mapKey(Entry<? extends K, ? extends V> pair, Function<? super K, ? extends K2> transformer) {
        return Pair.of(transformer.apply(pair.getKey()), pair.getValue());
    }

    public static <K, V, K2> Iterable<Pair<K2, V>> flatMapKey(Entry<? extends K, ? extends V> pair, Function<? super K, ? extends Iterable<? extends K2>> transformer) {
        return Iterables.transform(transformer.apply(pair.getKey()), Pair.<K2, V>creator().bindSecond(pair.getValue()));
    }

    public static <K, V, V2> Iterable<Pair<K, V2>> flatMapValue(Entry<? extends K, ? extends V> pair, Function<? super V, ? extends Iterable<? extends V2>> transformer) {
        return Iterables.transform(transformer.apply(pair.getValue()), Pair.<K, V2>creator().bindFirst(pair.getKey()));
    }

    public static <K, V, K2> F<Entry<? extends K, ? extends V>, Pair<K2, V>> keyMapper(final Function<? super K, ? extends K2> transformer) {
        return new F<Entry<? extends K, ? extends V>, Pair<K2, V>>() { public Pair<K2, V> apply(Entry<? extends K, ? extends V> input) {
            return mapKey(input, transformer);
        } };
    }

    public static <K, V, K2> F<Entry<? extends K, ? extends V>, Iterable<Pair<K2, V>>> keyFlatMapper(final Function<? super K, ? extends Iterable<? extends K2>> transformer) {
        return new F<Entry<? extends K, ? extends V>, Iterable<Pair<K2, V>>>() { public Iterable<Pair<K2, V>> apply(Entry<? extends K, ? extends V> input) {
            return flatMapKey(input, transformer);
        } };
    }

    public static <T, U, V> F<Map.Entry<? extends T, ? extends U>, Pair<T, V>> valueMapper(final Function<? super U, ? extends V> transformer) {
        return new F<Entry<? extends T, ? extends U>, Pair<T, V>>() { public Pair<T, V> apply(Entry<? extends T, ? extends U> input) {
            return mapValue(input, transformer);
        } };
    }

    public static <K, V, V2> F<Entry<? extends K, ? extends V>, Iterable<Pair<K, V2>>> valueFlatMapper(final Function<? super V, ? extends Iterable<? extends V2>> transformer) {
        return new F<Entry<? extends K, ? extends V>, Iterable<Pair<K, V2>>>() { public Iterable<Pair<K, V2>> apply(Entry<? extends K, ? extends V> input) {
            return flatMapValue(input, transformer);
        } };
    }

    static EmptyPairs EMPTY = new EmptyPairs();
    @SuppressWarnings({"unchecked"})
    static class EmptyPairs extends EmptyIterable<Entry<Object,Object>> implements FunPairs<Object,Object> {
        public FunIterable keys() {
            return Functional.EMPTY;
        }

        public FunIterable values() {
            return Functional.EMPTY;
        }

        public void foreach2(Consumer2 visitor) {
        }

        @Override
        public Maybe.Pair head() {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair last() {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair find(Predicate predicate) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair reduce(Function2 reducer) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair min(Ordering ordering) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair max(Ordering ordering) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair minBy(Function valueComputer) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair minBy(Ordering ordering, Function valueComputer) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair maxBy(Function valueComputer) {
            return Maybe.noPair();
        }

        @Override
        public Maybe.Pair maxBy(Ordering ordering, Function valueComputer) {
            return Maybe.noPair();
        }

        public FunPairs mapKeys(Function keyTransformer) {
            return this;
        }

        public FunPairs mapValues(Function valueTransformer) {
            return this;
        }

        public FunPairs flatMapKeys(Function keyTransformer) {
            return this;
        }

        public FunPairs flatMapValues(Function keyTransformer) {
            return this;
        }

        public FunIterable map2(F2 transformer) {
            return Functional.EMPTY;
        }

        public FunPairs filterKeys(Predicate predicate) {
            return this;
        }

        public FunPairs uniqueKeys() {
            return this;
        }

        public FunPairs filterValues(Predicate predicate) {
            return this;
        }

        public FunPairs uniqueValues() {
            return this;
        }

        public Maybe.Pair minByKeys(Ordering<? super Object> keyOrdering) {
            return Maybe.noPair();
        }

        public Maybe.Pair maxByKeys(Ordering<? super Object> keyOrdering) {
            return Maybe.noPair();
        }

        public Maybe.Pair minByValues(Ordering<? super Object> valueOrdering) {
            return Maybe.noPair();
        }

        public Maybe.Pair maxByValues(Ordering<? super Object> valueOrdering) {
            return Maybe.noPair();
        }

        public <K2> Maybe.Pair<Object, Object> minByKeys(Ordering<? super K2> ordering, Function<? super Object, K2> keyValueComputer) {
            return Maybe.noPair();
        }

        public <K2> Maybe.Pair<Object, Object> maxByKeys(Ordering<? super K2> ordering, Function<? super Object, K2> keyValueComputer) {
            return Maybe.noPair();
        }

        public <V2> Maybe.Pair<Object, Object> minByValues(Ordering<? super V2> ordering, Function<? super Object, V2> valueComputer) {
            return Maybe.noPair();
        }

        public <V2> Maybe.Pair<Object, Object> maxByValues(Ordering<? super V2> ordering, Function<? super Object, V2> valueComputer) {
            return Maybe.noPair();
        }

        @Override
        public FunPairs cons(Entry<Object, Object> firstElement) {
            return new FunctionalPairs(ImmutableList.of(firstElement));
        }

        @Override
        public FunPairs append(Entry<Object, Object> lastElement) {
            return cons(lastElement);
        }

        public FunPairs plus(Iterable more) {
            return FunctionalPairs.extendPairs(more);
        }

        public FunPairs sortByKeys(Ordering keyOrdering) {
            return this;
        }

        public FunPairs sortByValues(Ordering valueOrdering) {
            return this;
        }

        @Override
        public FunPairs filter(Predicate predicate) {
            return this;
        }

        @Override
        public FunPairs toSortedCopy(Ordering c) {
            return this;
        }

        public ImmutableMap toMap() {
            return ImmutableMap.of();
        }

        public ImmutableListMultimap toMultimap() {
            return ImmutableListMultimap.of();
        }

        public ImmutableBiMap toBimap() {
            return ImmutableBiMap.of();
        }

        public HashMap toMutableMap() {
            return Maps.newHashMap();
        }
        
        public FunPairs swap() {
            return this;
        }

        @Override
        public Maybe findValueForKey(Object key) {
            return Maybe.not();
        }

        @Override
        public String joinPairs(String pairSeparator, String keyValueSeparator) {
            return "";
        }

        public FunPairs flatMapPairs2(F2 transformer) {
            return this;
        }

        public ComparingMap toComparingMap(Comparator ordering) {
            return new ComparingMap(Maps.newHashMap(), ordering);
        }

        public FunPairs mapPairs2(F2 transformer) {
            return this;
        }
        
        @Override
        public FunPairs limit(int maxElements) {
            return this;
        }

        @Override
        public FunPairs skip(int skippedElements) {
            return this;
        }
    }

    private static class FunMapEntries<K, V> extends FunctionalPairs<K, V> {
        private final Map<K, V> delegate;

        FunMapEntries(Map<K, V> delegate) {
            super(delegate.entrySet());
            this.delegate = delegate;
        }

        @Override
        protected Iterable<K> keysDelegate() {
            return delegate.keySet();
        }

        @Override
        protected Iterable<V> valuesDelegate() {
            return delegate.values();
        }

        @Override
        public FunPairs<K, V> uniqueKeys() {
            return this;
        }

        @Override
        public Maybe<V> findValueForKey(K key) {
            return Maybe.of(delegate.get(key));
        }

        @Override
        public ImmutableMap<K, V> toMap() {
            return ImmutableMap.copyOf(delegate);
        }
    }

    static class ZippedPairs<T, U> extends FunctionalPairs<T, U> {
        private final Iterable<? extends T> first;
        private final Iterable<? extends U> second;

        public ZippedPairs(Iterable<? extends T> first, Iterable<? extends U> second) {
            super(ZippedIterable.of(first, second, Pair.<T, U>creator()));
            this.first = first;
            this.second = second;
        }

        // specialized overrides to avoid creating intermediate Pairs
        @Override
        public <O> FunIterable<O> map2(final F2<? super T, ? super U, ? extends O> transformer) {
            return zipWith(first, second, transformer);
        }

        @Override
        public <K2, V2> FunPairs<K2, V2> mapPairs2(F2<? super T, ? super U, ? extends Entry<K2, V2>> transformer) {
            return new FunctionalPairs<>(ZippedIterable.of(first, second, transformer));
        }

        @Override
        public <K2, V2> FunPairs<K2, V2> flatMapPairs2(F2<? super T, ? super U, ? extends Iterable<? extends Entry<K2, V2>>> transformer) {
            return new FunctionalPairs<>(Iterables.concat(ZippedIterable.of(first, second, transformer)));
        }

        @Override
        public void foreach2(Consumer2<? super T, ? super U> visitor) {
            Iterator<? extends T> it1 = first.iterator();
            Iterator<? extends U> it2 = second.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                visitor.handle(it1.next(), it2.next());
            }
        }
    }
}
