package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import joshng.util.blocks.F;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Source;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

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

  public static <T> Collector<T, ?, ArrayList<T>> arrayListCollector() {
    return Collectors.toCollection(arrayListFactory());
  }

  public static <T> T pickRandomItem(List<T> items, IntUnaryOperator random) {
    return items.get(random.applyAsInt(items.size()));
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

  public static <T> int count(Iterable<T> iterable, Predicate<T> predicate) {
    int count = 0;
    for (T item : iterable) {
      if (predicate.apply(item)) {
        ++count;
      }
    }

    return count;
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

  /**
   * Copies src into dst, but replaces elements in the range src[index..(index+howMany)] with the provided values
   * @param src source values to copy
   * @param dst destination for result
   * @param index starting index for replacement
   * @param howMany number of items to replace with provided value(s)
   * @param values values to overwrite [index..(index+howMany)]
   * @return the dst array
   */
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
