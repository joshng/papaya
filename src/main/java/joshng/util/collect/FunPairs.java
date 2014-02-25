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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * User: josh
 * Date: 3/15/12
 * Time: 9:13 AM
 */

/**
 * An enhancement of the {@link FunIterable} interface for operating on sequences of {@link Pair} or {@link Map.Entry}
 * @see #toMap
 */
public interface FunPairs<K,V> extends FunIterable<Map.Entry<K,V>> {
    /**
     * @return a FunIterable which, when iterated, yields the {@link Map.Entry#getKey keys} from the entries in this sequence
     */
    FunIterable<K> keys();

    /**
     * @return a FunIterable which, when iterated, yields the {@link Map.Entry#getValue values} from the entries in this sequence
     */
    FunIterable<V> values();

    /**
     * Produces a new FunPairs that transforms the keys of this sequence using the provided {@code keyTransformer}.<br/><br/>
     *
     * Conceptually similar to the following (but more efficient/correct, as code shown would execute two
     * simultaneous traversals over the underlying sequence to compute both {@link #keys()} and {@link #values()}):
     * <pre>{@code
     * entries.keys().map(keyTransformer).zip(entries.values());
     * }</pre>
     * @param keyTransformer a {@link Function} to apply to the keys in this sequence
     * @param <K2> the key-type for the new FunPairs sequence
     */
    <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer);

    /**
     * Produces a new FunPairs that transforms the keys of this sequence using the provided {@code keyTransformer}.<br/><br/>
     *
     * Conceptually similar to the following (but more efficient/correct, as the code shown would execute two
     * simultaneous traversals over the underlying sequence to compute both {@link #keys()} and {@link #values()}):
     * <pre>{@code
     * entries.keys().zip(entries.values().map(valueTransformer));
     * }</pre>
     * @param valueTransformer a {@link Function} to apply to the values in this sequence
     * @param <V2> the value-type for the new FunPairs sequence
     */
    <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer);

    /**
     * Produces a new FunPairs that applies the given {@code keyTransformer} to each key in this sequence, and
     * pairs each resulting output as a key with the value originally associated with the input key.<br/><br/>
     *
     * Example:
     * <pre>{@code
     * FunPairs<String, Integer> pairs = funPairs(ImmutableMap.of("a", 1, "b", 2));
     *
     * pairs.flatMapKeys(new F<String, List<String>>() { public List<String> apply(String key) {
     *     return Arrays.asList(key + "X", key + "Y");  // two new keys for each input key
     * }});
     *
     * Returns a FunPairs<String, Integer> that yields:
     *          ("aX" -> 1),
     *          ("aY" -> 1),
     *          ("bX" -> 2),
     *          ("bY" -> 2)
     * }</pre>
     *
     * Note that any values (in this case, Integers) in this sequence that are associated with a key that is mapped to
     * an <em>empty</em> iterable will be OMITTED from the resulting pairs.  Thus, using a keyTransformer that returns
     * a {@link Maybe} can be quite useful as a mechanism for simultaneously transforming and filtering the sequence.
     *
     * @param keyTransformer a function to produce an Iterable of new keys from each input-key
     * @param <K2> the key-type of the resulting pairs
     */
    <K2> FunPairs<K2, V> flatMapKeys(Function<? super K, ? extends Iterable<? extends K2>> keyTransformer);

    /**
     * Produces a new FunPairs that applies the given {@code valueTransformer} to each value in this sequence, and
     * pairs each resulting output as a value with the key originally associated with the input value.<br/><br/>
     *
     * Example:
     * <pre>{@code
     * FunPairs<String, Integer> pairs = funPairs(ImmutableMap.of("a", 1, "b", 2));
     *
     * pairs.flatMapValues(new F<Integer, List<Integer>>() { public List<Integer> apply(Integer value) {
     *     return Arrays.asList(value * 10, value * 20);  // two new values for each input value
     * }});
     *
     * Returns a FunPairs<String, Integer> that yields:
     *          ("a" -> 10),
     *          ("a" -> 20),
     *          ("b" -> 20),
     *          ("b" -> 40)
     * }</pre>
     *
     * Note that any keys (in this case, Strings) in this sequence that are associated with a value that is mapped to
     * an <em>empty</em> iterable will be OMITTED from the resulting pairs.  Thus, using a valueTransformer that returns
     * a {@link Maybe} can be quite useful as a mechanism for simultaneously transforming and filtering the sequence.
     *
     * @param valueTransformer a function to produce an Iterable of new values from each input-value
     * @param <V2> the value-type of the resulting pairs
     */
    <V2> FunPairs<K, V2> flatMapValues(Function<? super V, ? extends Iterable<? extends V2>> valueTransformer);

    /**
     * Produces as new FunIterable that, when iterated, yields the results of applying the given
     * {@link F2} {@code transformer} to each {@link Map.Entry Entry} in this sequence.
     */
    <O> FunIterable<O> map2(F2<? super K, ? super V, ? extends O> transformer);

    void foreach2(Consumer2<? super K, ? super V> visitor);

    /**
     * {@inheritDoc}
     * @return a new FunPairs that, when iterated, applies the {@code predicate} to each element in this
     * sequence and yields only those entries for which the result is {@code true}.
     * */
    FunPairs<K, V> filter(Predicate<? super Map.Entry<K, V>> predicate);

    /**
     * Eliminates entries whose {@link Map.Entry#getKey keys} do not match the given {@link Predicate}.
     */
    FunPairs<K, V> filterKeys(Predicate<? super K> predicate);

    /**
     * @return a new FunPairs which omits all but the <em>first</em> occurrence of each unique
     * {@link Map.Entry#getKey key} in this sequence
     */
    FunPairs<K, V> uniqueKeys();

    /**
     * Eliminates entries whose {@link Map.Entry#getValue values} do not match the given {@link Predicate}.
     */
    FunPairs<K, V> filterValues(Predicate<? super V> predicate);

    /**
     * @return a new FunPairs which omits all but the <em>first</em> occurrence of each unique
     * {@link java.util.Map.Entry#getValue value} in this sequence
     */
    FunPairs<K, V> uniqueValues();

    FunPairs<K, V> toSortedCopy(Ordering<? super Map.Entry<K, V>> ordering);

    FunPairs<K, V> sortByKeys(Ordering<? super K> keyOrdering);
    FunPairs<K, V> sortByValues(Ordering<? super V> valueOrdering);

    Maybe.Pair<K, V> minByKeys(Ordering<? super K> keyOrdering);
    Maybe.Pair<K, V> maxByKeys(Ordering<? super K> keyOrdering);
    Maybe.Pair<K, V> minByValues(Ordering<? super V> valueOrdering);
    Maybe.Pair<K, V> maxByValues(Ordering<? super V> valueOrdering);

    <O> Maybe.Pair<K, V> minByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer);
    <O> Maybe.Pair<K, V> maxByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer);
    <O> Maybe.Pair<K, V> minByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer);
    <O> Maybe.Pair<K, V> maxByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer);

    FunPairs<K, V> cons(Map.Entry<K, V> firstElement);
    FunPairs<K, V> append(Map.Entry<K, V> lastElement);
    FunPairs<K, V> plus(Iterable<? extends Map.Entry<K, V>> moreElements);
    FunIterable<FunPairs<K,V>> partition(int size);
    @Override
    Maybe.Pair<K, V> head();
    @Override
    Maybe.Pair<K, V> last();

    Maybe.Pair<K, V> find(Predicate<? super Map.Entry<K, V>> predicate);

    @Override
    Maybe.Pair<K, V> reduce(Function2<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, ? extends Map.Entry<K, V>> reducer);
    @Override
    Maybe.Pair<K, V> min(Ordering<? super Map.Entry<K, V>> ordering);
    @Override
    Maybe.Pair<K, V> max(Ordering<? super Map.Entry<K, V>> ordering);
    @Override
    Maybe.Pair<K, V> minBy(Function<? super Map.Entry<K, V>, ? extends Comparable> valueComputer);
    @Override
    <N> Maybe.Pair<K, V> minBy(Ordering<? super N> ordering, Function<? super Map.Entry<K, V>, ? extends N> valueComputer);
    @Override
    Maybe.Pair<K, V> maxBy(Function<? super Map.Entry<K, V>, ? extends Comparable> valueComputer);
    @Override
    <N> Maybe.Pair<K, V> maxBy(Ordering<? super N> ordering, Function<? super Map.Entry<K, V>, ? extends N> valueComputer);
    /**
     * Builds an {@link ImmutableMap} from the entries in this sequence.<br/><br/>
     *
     * Note that the sequence <b>must not</b> contain entries with duplicated keys. If your sequence
     * could have multiple entries with the same key, consider using {@link #toMultimap()} or deduplicating
     * with {@link #uniqueKeys()}
     * @return a new ImmutableMap
     * @throw IllegalArgumentException if duplicate keys were found
     * @see #uniqueKeys
     * @see #toMultimap
     */
    ImmutableMap<K,V> toMap();
    ImmutableListMultimap<K, V> toMultimap();
    ImmutableBiMap<K, V> toBimap();
    ComparingMap<K, V> toComparingMap(Comparator<? super V> ordering);
    HashMap<K,V> toMutableMap();
    FunPairs<K, V> limit(int maxElements);
    FunPairs<K, V> skip(int skippedElements);
    <K2, V2> FunPairs<K2, V2> mapPairs2(F2<? super K, ? super V, ? extends Map.Entry<K2, V2>> transformer);
    <K2, V2> FunPairs<K2, V2> flatMapPairs2(F2<? super K, ? super V, ? extends Iterable<? extends Map.Entry<K2, V2>>> transformer);

    FunPairs<V,K> swap();
    Maybe<V> findValueForKey(K key);

    String joinPairs(String pairSeparator, String keyValueSeparator);
}
