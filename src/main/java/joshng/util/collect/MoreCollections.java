package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.*;
import joshng.util.blocks.F;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Source;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: josh
 * Date: Sep 18, 2010
 * Time: 2:58:02 AM
 */
public class MoreCollections {
  private static final F ARRAY_AS_LIST_FUNCTION = new F<Object[], List<Object>>() {
    public List<Object> apply(Object[] input) {
      return Arrays.asList(input);
    }
  };

  private static final Source ARRAY_LIST_FACTORY = new Source<ArrayList>() {
    @Override
    public ArrayList get() {
      return new ArrayList();
    }
  };

  public static <T> Set<T> newConcurrentSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
  }

  public static <K extends Enum<K>, V> ListMultimap<K, V> newEnumListMultimap(Class<K> keyType) {
    return Multimaps.newListMultimap(Maps.<K, Collection<V>>newEnumMap(keyType), MoreCollections.<V>arrayListFactory());
  }

  public static <K extends Enum<K>, V> Multimap<K, V> newEnumMultimap(Class<K> keyType, Supplier<? extends Collection<V>> factory) {
    return Multimaps.newMultimap(Maps.<K, Collection<V>>newEnumMap(keyType), factory);
  }

  public static <E extends Enum<E>> Set<E> enumSetCopyWith(Set<E> values, E newValue) {
    EnumSet<E> result;
    if (values.isEmpty()) {
      result = EnumSet.of(newValue);
    } else {
      result = EnumSet.copyOf(values);
      result.add(newValue);
    }
    return result;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> F<T[], List<T>> arrayAsList() {
    return ARRAY_AS_LIST_FUNCTION;
  }

  @SuppressWarnings("unchecked")
  public static <T> Source<ArrayList<T>> arrayListFactory() {
    return ARRAY_LIST_FACTORY;
  }

  public static <T> Source<ArrayList<T>> arrayListWithCapacityFactory(final int initialCapacity) {
    return new Source<ArrayList<T>>() {
      @Override
      public ArrayList<T> get() {
        return Lists.newArrayListWithCapacity(initialCapacity);
      }
    };
  }

  public static int sum(Iterable<Integer> values) {
    int sum = 0;
    for (int value : values) {
      sum += value;
    }
    return sum;
  }

  public static <K, V> ImmutableMap<K, V> immutableMapTo(Iterable<K> keys, Function<? super K, ? extends V> valueComputer) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (K key : keys) {
      builder.put(key, valueComputer.apply(key));
    }
    return builder.build();
  }

  public static <K, V> ImmutableMap<K, V> immutableMapBy(Iterable<V> values, Function<? super V, ? extends K> keyComputer) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (V value : values) {
      builder.put(keyComputer.apply(value), value);
    }
    return builder.build();
  }

  public static <T, K, V> ImmutableMap<K, V> immutableMap(Iterable<T> values, Function<? super T, ? extends K> keyComputer, Function<? super T, ? extends V> valueComputer) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (T value : values) {
      builder.put(keyComputer.apply(value), valueComputer.apply(value));
    }
    return builder.build();
  }

  public static <K, V> ImmutableMap<K, V> immutableMapWithEntries(Iterable<? extends Map.Entry<K, V>> entries) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (Map.Entry<K, V> entry : entries) {
      builder.put(entry);
    }
    return builder.build();
  }

  public static <K, V> ImmutableListMultimap<K, V> multimapWithEntries(Iterable<? extends Map.Entry<K, V>> entries) {
    ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
    for (Map.Entry<K, V> entry : entries) {
      builder.put(entry);
    }
    return builder.build();
  }

  public static <K, V> ImmutableBiMap<K, V> bimapWithEntries(Iterable<? extends Map.Entry<K, V>> entries) {
    ImmutableBiMap.Builder<K, V> builder = ImmutableBiMap.builder();
    for (Map.Entry<K, V> entry : entries) {
      builder.put(entry);
    }
    return builder.build();
  }

  public static <K, V> Map<K, V> mutableMapBy(Iterable<? extends V> values, Map<K, V> intoMap, Function<? super V, ? extends K> keyComputer) {
    for (V value : values) intoMap.put(keyComputer.apply(value), value);
    return intoMap;
  }

  public static <K, V> Map<K, V> mutableMapBy(Iterable<? extends V> values, Function<? super V, ? extends K> keyComputer) {
    return mutableMapBy(values, Maps.<K, V>newHashMap(), keyComputer);
  }

  public static <T> Deque<T> newArrayDequeWithCapacity(int capacity) {
    return new ArrayDeque<T>(capacity);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T[] appendArray(T[] array, T toAppend) {
    return concat(array, toAppend);
  }

  @SafeVarargs
  public static <T> T[] concat(T[] first, T... second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  public static <T> List<T> safeSubList(List<T> source, int fromIndexInclusive, int toIndexExclusive) {
    int sourceSize = source.size();
    if (fromIndexInclusive == 0 && toIndexExclusive >= sourceSize) return source;
    int fromIndex = Math.min(sourceSize, fromIndexInclusive);
    int toIndex = Math.min(sourceSize, toIndexExclusive);
    return source.subList(fromIndex, toIndex);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getFromMap(Map map, String key) {
    return (T) checkNotNull(MoreCollections.getFromMapOrNull(map, key), "%s not found from map", key);
  }

  @Nullable
  @SuppressWarnings({"unchecked"})
  public static <T> T getFromMapOrNull(Map map, String key) {
    return (T) map.get(key);
  }

  public static <T, P extends Predicate<T>> Iterable<P> selectMatchingPredicates(final T target, Iterable<P> predicates) {
    return Iterables.filter(predicates, new Predicate<P>() {
      public boolean apply(P input) {
        return input.apply(target);
      }
    });
  }

  public static <I, O> ImmutableList<O> transformedCopy(Iterable<I> inputs, Function<? super I, ? extends O> transformer) {
    return ImmutableList.copyOf(Iterables.transform(inputs, transformer));
  }

  public static <T> int count(Iterable<T> iterable, Predicate<T> predicate) {
    int count = 0;
    for (T item : iterable) {
      if (predicate.apply(item)) {
        ++count;
      }
    }

    return count;
  }

  public static <X, Y> List<Pair<X, Y>> zip(Iterable<X> xs, Iterable<Y> ys) {
    Iterator<X> xIterator = xs.iterator();
    Iterator<Y> yIterator = ys.iterator();
    List<Pair<X, Y>> pairs = Lists.newArrayList();
    while (xIterator.hasNext() && yIterator.hasNext()) {
      pairs.add(Pair.of(xIterator.next(), yIterator.next()));
    }
    checkArgument(!xIterator.hasNext(), "First collection had too many elements");
    checkArgument(!yIterator.hasNext(), "Second collection had too many elements");
    return pairs;
  }

  public static <K extends Enum<K>, V> EnumMap<K, V> enumMapOf(Class<K> enumClass, Function<K, Maybe<V>> valueComputer) {
    return enumMapOf(enumClass, Arrays.asList(enumClass.getEnumConstants()), valueComputer);
  }

  public static <K extends Enum<K>, V> EnumMap<K, V> enumMapOf(Class<K> enumClass, Iterable<K> enumConstants, Function<K, Maybe<V>> valueComputer) {
    EnumMap<K, V> map = Maps.newEnumMap(enumClass);
    for (K k : enumConstants) {
      for (V v : valueComputer.apply(k)) {
        map.put(k, v);
      }
    }

    return map;
  }

  public static boolean notEmpty(@Nullable Collection collection) {
    return collection != null && !collection.isEmpty();
  }

  public static <T> boolean containsAny(Iterable<T> from, Iterable<T> collection) {
    for (T t : collection) {
      if (Iterables.contains(from, t)) {
        return true;
      }
    }

    return false;
  }

  @SafeVarargs
  public static <T> T[] splice(T[] src, T[] dst, int index, int howMany, T... values) {
    int valueLen = values.length;
    int srcLen = src.length;
    checkArgument(dst.length == srcLen - howMany + valueLen);
    if (index > 0) System.arraycopy(src, 0, dst, 0, index);
    if (valueLen > 0) System.arraycopy(values, 0, dst, index, valueLen);
    int restPos = index + howMany;
    int restLen = srcLen - restPos;
    if (restLen > 0) System.arraycopy(src, restPos, dst, index + valueLen, restLen);
    return dst;
  }

  private MoreCollections() {
  }

  public static <V> boolean removeWithCount(Iterable<V> iterable, Pred<V> pred, int count) {
    if (count <= 0) {
      return true;
    }
    Iterator<V> iter = iterable.iterator();
    int currentCount = 0;
    while (iter.hasNext()) {
      if (pred.apply(iter.next())) {
        iter.remove();
        if (++currentCount == count) {
          return true;
        }
      }
    }

    return false;
  }

  public static <V> boolean removeFirst(Iterable<V> iterable, Pred<V> pred) {
    return removeWithCount(iterable, pred, 1);
  }

  /**
   * @return true of this Iterable is Collection, AND it is empty.  Note that this method returns {@code false}
   * for non-Collection objects, even if they <b>ARE</b> empty! This is to avoid initializing an Iterator to
   * determine if the {@link Iterator#hasNext}.  If this is desirable, use {@link Iterables#isEmpty} instead.
   */
  public static boolean isCollectionThatIsEmpty(Iterable<?> iterable) {
    return iterable instanceof Collection && ((Collection) iterable).isEmpty();
  }
}
