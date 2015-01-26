package joshng.util.collect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import joshng.util.blocks.F2;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Pred2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static joshng.util.blocks.Pred.pred;
import static joshng.util.blocks.Pred2.extendBiPredicate;
import static joshng.util.collect.MoreCollections.immutableMapWithEntries;
import static joshng.util.collect.MoreCollections.multimapWithEntries;
/**
 * User: josh
 * Date: 3/15/12
 * Time: 9:13 AM
 */

/**
 * An enhancement of the {@link FunIterable} interface for operating on sequences of {@link Pair} or {@link Map.Entry}
 *
 * @see #toMap
 */
public interface FunPairs<K, V> extends FunIterable<Map.Entry<K, V>> {
  /**
   * @return a FunIterable which, when iterated, yields the {@link Map.Entry#getKey keys} from the entries in this sequence
   */
  default FunIterable<K> keys() {
    return Functional.extend(keysDelegate());
  }

  default Iterable<K> keysDelegate() {
    return Iterables.transform(delegate(), Pair.<K>getFirstFromPair());
  }

  /**
   * @return a FunIterable which, when iterated, yields the {@link Map.Entry#getValue values} from the entries in this sequence
   */
  default FunIterable<V> values() {
    return Functional.extend(valuesDelegate());
  }

  default Iterable<V> valuesDelegate() {
    return Iterables.transform(delegate(), Pair.<V>getSecondFromPair());
  }

  /**
   * Produces a new FunPairs that transforms the keys of this sequence using the provided {@code keyTransformer}.<br/><br/>
   * <p>
   * Conceptually similar to the following (but more efficient/correct, as code shown would execute two
   * simultaneous traversals over the underlying sequence to compute both {@link #keys()} and {@link #values()}):
   * <pre>{@code
   * entries.keys().map(keyTransformer).zip(entries.values());
   * }</pre>
   *
   * @param keyTransformer a {@link Function} to apply to the keys in this sequence
   * @param <K2>           the key-type for the new FunPairs sequence
   */
  default <K2> FunPairs<K2, V> mapKeys(Function<? super K, ? extends K2> keyTransformer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), FunctionalPairs.<K, V, K2>keyMapper(keyTransformer)));
  }


  /**
   * Produces a new FunPairs that transforms the keys of this sequence using the provided {@code keyTransformer}.<br/><br/>
   * <p>
   * Conceptually similar to the following (but more efficient/correct, as the code shown would execute two
   * simultaneous traversals over the underlying sequence to compute both {@link #keys()} and {@link #values()}):
   * <pre>{@code
   * entries.keys().zip(entries.values().map(valueTransformer));
   * }</pre>
   *
   * @param valueTransformer a {@link Function} to apply to the values in this sequence
   * @param <V2>             the value-type for the new FunPairs sequence
   */
  default <V2> FunPairs<K, V2> mapValues(Function<? super V, ? extends V2> valueTransformer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), FunctionalPairs.<K, V, V2>valueMapper(valueTransformer)));
  }


  /**
   * Produces a new FunPairs that applies the given {@code keyTransformer} to each key in this sequence, and
   * pairs each resulting output as a key with the value originally associated with the input key.<br/><br/>
   * <p>
   * Example:
   * <pre>{@code
   * FunPairs<String, Integer> pairs = funPairs(ImmutableMap.of("a", 1, "b", 2));
   * <p>
   * pairs.flatMapKeys(new F<String, List<String>>() { public List<String> apply(String key) {
   *     return Arrays.asList(key + "X", key + "Y");  // two new keys for each input key
   * }});
   * <p>
   * Returns a FunPairs<String, Integer> that yields:
   *          ("aX" -> 1),
   *          ("aY" -> 1),
   *          ("bX" -> 2),
   *          ("bY" -> 2)
   * }</pre>
   * <p>
   * Note that any values (in this case, Integers) in this sequence that are associated with a key that is mapped to
   * an <em>empty</em> iterable will be OMITTED from the resulting pairs.  Thus, using a keyTransformer that returns
   * a {@link Maybe} can be quite useful as a mechanism for simultaneously transforming and filtering the sequence.
   *
   * @param keyTransformer a function to produce an Iterable of new keys from each input-key
   * @param <K2>           the key-type of the resulting pairs
   */
  default <K2> FunPairs<K2, V> flatMapKeys(Function<? super K, ? extends Iterable<? extends K2>> keyTransformer) {
    return new FunctionalPairs<>(Iterables.concat(Iterables.transform(delegate(), FunctionalPairs.<K, V, K2>keyFlatMapper(keyTransformer))));
  }


  /**
   * Produces a new FunPairs that applies the given {@code valueTransformer} to each value in this sequence, and
   * pairs each resulting output as a value with the key originally associated with the input value.<br/><br/>
   * <p>
   * Example:
   * <pre>{@code
   * FunPairs<String, Integer> pairs = funPairs(ImmutableMap.of("a", 1, "b", 2));
   * <p>
   * pairs.flatMapValues(new F<Integer, List<Integer>>() { public List<Integer> apply(Integer value) {
   *     return Arrays.asList(value * 10, value * 20);  // two new values for each input value
   * }});
   * <p>
   * Returns a FunPairs<String, Integer> that yields:
   *          ("a" -> 10),
   *          ("a" -> 20),
   *          ("b" -> 20),
   *          ("b" -> 40)
   * }</pre>
   * <p>
   * Note that any keys (in this case, Strings) in this sequence that are associated with a value that is mapped to
   * an <em>empty</em> iterable will be OMITTED from the resulting pairs.  Thus, using a valueTransformer that returns
   * a {@link Maybe} can be quite useful as a mechanism for simultaneously transforming and filtering the sequence.
   *
   * @param valueTransformer a function to produce an Iterable of new values from each input-value
   * @param <V2>             the value-type of the resulting pairs
   */
  default <V2> FunPairs<K, V2> flatMapValues(Function<? super V, ? extends Iterable<? extends V2>> valueTransformer) {
    return new FunctionalPairs<>(Iterables.concat(Iterables.transform(delegate(), FunctionalPairs.<K, V, V2>valueFlatMapper(valueTransformer))));
  }


  /**
   * Produces as new FunIterable that, when iterated, yields the results of applying the given
   * {@link F2} {@code transformer} to each {@link Map.Entry Entry} in this sequence.
   */
  default <O> FunIterable<O> map2(F2<? super K, ? super V, O> transformer) {
    return map(transformer.tupled());
  }

  default void foreach2(BiConsumer<? super K, ? super V> visitor) {
    foreach(e -> visitor.accept(e.getKey(), e.getValue()));
  }

  default Maybe.Pair<K,V> findByKey(Predicate<? super K> predicate) {
    return find2(Pred2.ignoringSecond(predicate));
  }

  default Maybe.Pair<K,V> findByValue(Predicate<? super V> predicate) {
    return find2(Pred2.ignoringFirst(predicate));
  }

  default Maybe.Pair<K,V> find2(BiPredicate<? super K, ? super V> predicate) {
    return find(Pred2.extendBiPredicate(predicate));
  }

  default boolean all2(BiPredicate<? super K, ? super V> predicate) {
    return find2(predicate.negate()).isEmpty();
  }

  default <A> A accumulate2(BiAccumulator<? super K, ? super V, ? extends A> accumulator) {
    foreach2(accumulator);
    return accumulator.get();
  }

  /**
   * {@inheritDoc}
   *
   * @return a new FunPairs that, when iterated, applies the {@code predicate} to each element in this
   * sequence and yields only those entries for which the result is {@code true}.
   */
  default FunPairs<K, V> filter(Predicate<? super Entry<K, V>> predicate) {
    return new FunctionalPairs<>(Iterables.filter(delegate(), (com.google.common.base.Predicate<? super Entry<K, V>>) predicate::test));
  }


  /**
   * Eliminates entries whose {@link Map.Entry#getKey keys} do not match the given {@link Predicate}.
   */
  default FunPairs<K, V> filterKeys(Predicate<? super K> predicate) {
    return new FunctionalPairs<>(Iterables.filter(delegate(), Pair.<K>getFirstFromPair().resultMatches(predicate)));
  }

  /**
   * Eliminates entries whose {@link Map.Entry#getValue values} do not match the given {@link Predicate}.
   */
  default FunPairs<K, V> filterValues(Predicate<? super V> predicate) {
    return new FunctionalPairs<>(Iterables.filter(delegate(), Pair.<V>getSecondFromPair().resultMatches(predicate)));
  }

  default FunPairs<K, V> filter2(BiPredicate<? super K, ? super V> predicate) {
    return filter(extendBiPredicate(predicate));
  }

  /**
   * @return a new FunPairs which omits all but the <em>first</em> occurrence of each unique
   * {@link Map.Entry#getKey key} in this sequence
   */
  default FunPairs<K, V> uniqueKeys() {
    return filterKeys(Pred.newDeduplicator());
  }

  /**
   * @return a new FunPairs which omits all but the <em>first</em> occurrence of each unique
   * {@link java.util.Map.Entry#getValue value} in this sequence
   */
  default FunPairs<K, V> uniqueValues() {
    return filterValues(Pred.newDeduplicator());
  }

  default FunPairs<K, V> toSortedCopy(Ordering<? super Entry<K, V>> ordering) {
    return new FunctionalPairs<>(ordering.<Entry<K, V>>sortedCopy(delegate()));
  }


  default FunPairs<K, V> sortByKeys(Ordering<? super K> keyOrdering) {
    return toSortedCopy(orderingByKey(keyOrdering));
  }

  static <K, V> Ordering<Entry<K, V>> orderingByKey(Ordering<? super K> keyOrdering) {
    return keyOrdering.onResultOf(Entry::getKey);
  }

  default FunPairs<K, V> sortByValues(Ordering<? super V> valueOrdering) {
    return toSortedCopy(valueOrdering.onResultOf(Entry::getValue));
  }

  default Maybe.Pair<K, V> minByKeys(Ordering<? super K> keyOrdering) {
    return minBy(keyOrdering, Entry::getKey);
  }

  default Maybe.Pair<K, V> maxByKeys(Ordering<? super K> keyOrdering) {
    return maxBy(keyOrdering, Entry::getKey);
  }

  default Maybe.Pair<K, V> minByValues(Ordering<? super V> valueOrdering) {
    return minBy(valueOrdering, Entry::getValue);
  }

  default Maybe.Pair<K, V> maxByValues(Ordering<? super V> valueOrdering) {
    return maxBy(valueOrdering, Entry::getValue);
  }

  default <O> Maybe.Pair<K, V> minByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer) {
    return minBy(ordering, Pair.<K>getFirstFromPair().andThen(keyValueComputer));
  }

  default <O> Maybe.Pair<K, V> maxByKeys(Ordering<? super O> ordering, Function<? super K, O> keyValueComputer) {
    return maxBy(ordering, Pair.<K>getFirstFromPair().andThen(keyValueComputer));
  }

  default <O> Maybe.Pair<K, V> minByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer) {
    return minBy(ordering, Pair.<V>getSecondFromPair().andThen(valueComputer));
  }

  default <O> Maybe.Pair<K, V> maxByValues(Ordering<? super O> ordering, Function<? super V, O> valueComputer) {
    return maxBy(ordering, Pair.<V>getSecondFromPair().andThen(valueComputer));
  }


  @Override
  default FunPairs<K, V> prepend(Entry<K, V> firstElement) {
    return new FunctionalPairs<>(PrependedIterable.of(firstElement, delegate()));
  }

  @Override
  default FunPairs<K, V> append(Entry<K, V> lastElement) {
    return new FunctionalPairs<>(AppendedIterable.of(delegate(), lastElement));
  }

  default FunPairs<K, V> plus(Iterable<? extends Entry<K, V>> more) {
    return new FunctionalPairs<>(Iterables.concat(delegate(), more));
  }

  @Override
  default FunIterable<? extends FunPairs<K, V>> partition(int size) {
    return FunIterable.map(Iterables.partition(delegate(), size), FunctionalPairs::extendPairs);
  }


  @Override
  default Maybe.Pair<K, V> head() {
    return (Maybe.Pair<K, V>) FunIterable.super.head();
  }

  @Override
  default Maybe.Pair<K, V> foot() {
    return (Maybe.Pair<K, V>) FunIterable.super.foot();
  }


  @Override
  default Maybe.Pair<K, V> find(Predicate<? super Map.Entry<K, V>> predicate) {
    return (Maybe.Pair<K, V>) FunIterable.super.find(predicate);
  }


  @Override
  default Maybe.Pair<K, V> reduce(F2<? super Entry<K, V>, ? super Entry<K, V>, ? extends Entry<K, V>> reducer) {
    return (Maybe.Pair<K, V>) FunIterable.super.reduce(reducer);
  }

  @Override
  default Maybe.Pair<K, V> min(Ordering<? super Entry<K, V>> ordering) {
    return (Maybe.Pair<K, V>) FunIterable.super.min(ordering);
  }

  @Override
  default Maybe.Pair<K, V> max(Ordering<? super Entry<K, V>> ordering) {
    return (Maybe.Pair<K, V>) FunIterable.super.max(ordering);
  }

  @Override
  default Maybe.Pair<K, V> minBy(Function<? super Entry<K, V>, ? extends Comparable<?>> valueComputer) {
    return (Maybe.Pair<K, V>) FunIterable.super.minBy(valueComputer);
  }

  @Override
  default <N> Maybe.Pair<K, V> minBy(Ordering<? super N> ordering, Function<? super Entry<K, V>, ? extends N> valueComputer) {
    return (Maybe.Pair<K, V>) FunIterable.super.minBy(ordering, valueComputer);
  }

  @Override
  default Maybe.Pair<K, V> maxBy(Function<? super Entry<K, V>, ? extends Comparable<?>> valueComputer) {
    return (Maybe.Pair<K, V>) FunIterable.super.maxBy(valueComputer);
  }

  @Override
  default <N> Maybe.Pair<K, V> maxBy(Ordering<? super N> ordering, Function<? super Entry<K, V>, ? extends N> valueComputer) {
    return (Maybe.Pair<K, V>) FunIterable.super.maxBy(ordering, valueComputer);
  }

  /**
   * Builds an {@link ImmutableMap} from the entries in this sequence.<br/><br/>
   * <p>
   * Note that the sequence <b>must not</b> contain entries with duplicated keys. If your sequence
   * could have multiple entries with the same key, consider using {@link #toMultimap()} or deduplicating
   * with {@link #uniqueKeys()}
   *
   * @return a new ImmutableMap
   * @throw IllegalArgumentException if duplicate keys were found
   * @see #uniqueKeys
   * @see #toMultimap
   */
  default ImmutableMap<K, V> toMap() {
    return immutableMapWithEntries(delegate());
  }

  default ImmutableListMultimap<K, V> toMultimap() {
    return multimapWithEntries(delegate());
  }

  default ImmutableBiMap<K, V> toBimap() {
    return accumulate2(Accumulator.biMap());
  }

  default ComparingMap<K, V> toComparingMap(Comparator<? super V> ordering) {
    return ComparingMap.build(ordering, delegate());
  }

  default HashMap<K, V> toMutableMap() {
    HashMap<K, V> map = Maps.newHashMap();
    for (Entry<K, V> entry : delegate()) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

  default FunPairs<K, V> limit(int maxElements) {
    return new FunctionalPairs<>(Iterables.limit(delegate(), maxElements));
  }

  default FunPairs<K, V> skip(int skippedElements) {
    return new FunctionalPairs<>(Iterables.skip(delegate(), skippedElements));
  }

  default <K2, V2> FunPairs<K2, V2> mapPairs2(F2<? super K, ? super V, ? extends Entry<K2, V2>> transformer) {
    return mapPairs(transformer.tupled());
  }

  default <K2, V2> FunPairs<K2, V2> flatMapPairs2(F2<? super K, ? super V, ? extends Iterable<? extends Entry<K2, V2>>> transformer) {
    return flatMapPairs(transformer.tupled());
  }

  default <K2> FunPairs<K2, V> mapKeys2(F2<? super K, ? super V, K2> keyComputer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), pair -> new Pair<>(keyComputer.apply(pair.getKey(), pair.getValue()), pair.getValue())));
  }

  default <V2> FunPairs<K, V2> mapValues2(F2<? super K, ? super V, V2> valueComputer) {
    return new FunctionalPairs<>(Iterables.transform(delegate(), pair -> new Pair<>(pair.getKey(), valueComputer.apply(pair.getKey(), pair.getValue()))));
  }

  /**
   * @returns a FunPairs containing an eagerly precomputed list of the values produced by this sequence
   */
  default FunPairs<K, V> reifiedPairs() {
    List<Entry<K, V>> reifiedDelegate = ImmutableList.copyOf(delegate());
    if (reifiedDelegate == delegate()) return this;
    return new FunctionalPairs<>(reifiedDelegate);
  }

  default FunPairs<V, K> swap() {
    return mapPairs(Pair.<K, V>swapper());
  }

  default Maybe<V> findValueForKey(K key) {
    return find(Pair.<K>getFirstFromPair().resultEqualTo(key)).getValue();
  }

  default String joinPairs(String pairSeparator, String keyValueSeparator) {
    return Joiner.on(pairSeparator).withKeyValueSeparator(keyValueSeparator).join(this);
  }
}
