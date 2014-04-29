package joshng.util.collect;

import com.google.common.collect.*;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.blocks.Sink2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static joshng.util.Reflect.blindCast;

/**
 * User: josh
 * Date: 3/15/12
 * Time: 9:33 AM
 */
public class FunctionalPairs<K,V> extends FunctionalIterable<Entry<K,V>> implements FunPairs<K, V> {
    private static final F2 ENTRY_CREATOR = Pair.creator();

    public static <K,V> FunPairs<K,V> extendPairs(Iterable<? extends Entry<? extends K, ? extends V>> delegate) {
        if (delegate instanceof FunPairs) return blindCast(delegate);
        if (MoreCollections.isCollectionThatIsEmpty(delegate)) return Functional.emptyPairs();
        return new FunctionalPairs<>(delegate);
    }

    public static <K, V> FunPairs<K, V> extend(Map<K, V> map) {
        if (map.isEmpty()) return Functional.emptyPairs();
        return new FunMapEntries<>(map);
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
        super(blindCast(delegate)); // we never ADD anything to the Iterable, so this widening cast is safe
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
    static class EmptyPairs extends Functional.EmptyIterable<Entry<Object,Object>> implements FunPairs<Object,Object> {
        public FunIterable keys() {
            return Functional.EMPTY;
        }

        public FunIterable values() {
            return Functional.EMPTY;
        }

        public void foreach2(BiConsumer visitor) {
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
        public Maybe.Pair reduce(F2 reducer) {
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

//        @Override
//        public FunPairs prepend(Entry firstElement) {
//            return new FunctionalPairs<Object, Object>(ImmutableList.of(firstElement));
//        }

//        @Override
//        public FunPairs append(Entry<Object, Object> lastElement) {
//            return prepend(lastElement);
//        }

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

    private static class FunMapEntries<K, V> implements FunPairs<K,V> {
        private final Map<K, V> map;

        FunMapEntries(Map<K, V> delegate) {
            this.map = delegate;
        }

        @Override
        public Iterable<Entry<K, V>> delegate() {
            return map.entrySet();
        }

        @Override
        public Iterable<K> keysDelegate() {
            return map.keySet();
        }

        @Override
        public Iterable<V> valuesDelegate() {
            return map.values();
        }

        @Override
        public FunPairs<K, V> uniqueKeys() {
            return this;
        }

        @Override
        public <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer) {
            return new TransformedMap<K, V, K2, V>(map, F.extendF(keyTransformer), F.<V>identity());
        }

        @Override
        public <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer) {
            return new TransformedMap<K, V, K, V2>(map, F.<K>identity(), F.extendF(valueTransformer));
        }

        @Override
        public Maybe<V> findValueForKey(K key) {
            return Maybe.of(map.get(key));
        }

        @Override
        public ImmutableMap<K, V> toMap() {
            return ImmutableMap.copyOf(map);
        }

        private static class TransformedMap<K, V, K1, V1> implements FunPairs<K1,V1> {
            private Map<K, V> map;
            private final F<? super K, ? extends K1> keyTransformer;
            private final F<? super V, ? extends V1> valueTransformer;

            private TransformedMap(Map<K, V> map, F<? super K, ? extends K1> keyTransformer, F<? super V, ? extends V1> valueTransformer) {
                this.map = map;
                this.keyTransformer = keyTransformer;
                this.valueTransformer = valueTransformer;
            }

            @Override
            public Iterable<Entry<K1, V1>> delegate() {
                return Iterables.transform(map.entrySet(), p -> Pair.of(keyTransformer.apply(p.getKey()), valueTransformer.apply(p.getValue())));
            }

            @Override
            public Iterable<K1> keysDelegate() {
                return Iterables.transform(map.keySet(), keyTransformer);
            }

            @Override
            public Iterable<V1> valuesDelegate() {
                return Iterables.transform(map.values(), valueTransformer);
            }

            @Override
            public <K2> FunPairs<K2, V1> mapKeys(Function<? super K1, ? extends K2> keyTransformer) {
                return new TransformedMap<>(map, this.keyTransformer.andThen(keyTransformer), valueTransformer);
            }

            @Override
            public <V2> FunPairs<K1, V2> mapValues(Function<? super V1, ? extends V2> valueTransformer) {
                return new TransformedMap<>(map, this.keyTransformer, this.valueTransformer.andThen(valueTransformer));
            }

            @Override
            public void foreach2(BiConsumer<? super K1, ? super V1> visitor) {
                for (Entry<K, V> entry : map.entrySet()) {
                    visitor.accept(keyTransformer.apply(entry.getKey()), valueTransformer.apply(entry.getValue()));
                }
            }

            @Override
            public <O> FunIterable<O> map2(F2<? super K1, ? super V1, ? extends O> transformer) {
                return new FunctionalIterable<>(() -> new AbstractIterator<O>() {
                    Iterator<Entry<K,V>> iterator = map.entrySet().iterator();
                    @Override
                    protected O computeNext() {
                        if (!iterator.hasNext()) return endOfData();
                        Entry<K, V> nextRaw = iterator.next();
                        return transformer.apply(keyTransformer.apply(nextRaw.getKey()), valueTransformer.apply(nextRaw.getValue()));
                    }
                });
            }

            @Override
            public ImmutableMap<K1, V1> toMap() {
                return accumulate2(Accumulator.immutableMap());
            }
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
            return FunIterable.zipWith(first, second, transformer);
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
        public void foreach2(BiConsumer<? super T, ? super U> visitor) {
            Iterator<? extends T> it1 = first.iterator();
            Iterator<? extends U> it2 = second.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                visitor.accept(it1.next(), it2.next());
            }
        }
    }
}
