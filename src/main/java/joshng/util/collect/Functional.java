package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import joshng.util.Reflect;
import joshng.util.ThreadLocalRef;
import joshng.util.ThreadLocals;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.concurrent.LazyReference;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static com.google.common.base.Preconditions.checkNotNull;
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
 *
 * @see FunList
 * @see FunSet
 * @see FunPairs
 */
public interface Functional<T> extends FunIterable<T> {
  /**
   * Wraps an existing Iterable with a FunIterable.  Note that this method does not make a copy or invoke the provided
   * Iterable in any way (besides checking {@link Collection#isEmpty} if it is a {@link Collection}):
   * changes to the underlying Iterable may be visible via the returned FunIterable, EXCEPT if it is initially an empty
   * collection.
   *
   * @param delegate the iterable to extend
   * @return a FunIterable wrapping the underlying iterable.
   */
  public static <T> FunIterable<T> extend(final Iterable<T> delegate) {
    if (delegate instanceof FunIterable) return (FunIterable<T>) delegate;
    if (MoreCollections.isCollectionThatIsEmpty(delegate)) return Functional.<T>empty();
    return new FunctionalIterable<>(delegate);
  }

  public static <T> FunList<T> extend(final Maybe<T> delegate) {
    return delegate.isDefined() ? funListOf(delegate.getOrThrow()) : Functional.<T>emptyList();
  }

  /**
   * Wraps an array in a FunIterable. Equivalent to calling {@link #extend}(Arrays.asList(items))
   */
  public static <T> FunIterable<T> extend(T[] items) {
    if (items.length == 0) return Functional.<T>empty();
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

  static final F EXTENDER = new F<Iterable<Object>, FunIterable<Object>>() {
    public FunIterable<Object> apply(Iterable<Object> input) {
      return extend(input);
    }
  };

  static final ThreadLocalRef<ExecutorService> activeParallelThreadPool = ThreadLocals.newThreadLocalRef();

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
   * <p>
   * Typically, functional traversals should instead begin with {@link #extend(Iterable)},
   * invoking {@link #toList()} or {@link #toSet()} (or simply iterating with a <em>{@code for}</em> loop) only after all
   * desired {@link #map}s and {@link #filter}s are expressed.
   *
   * @return a new FunList containing the items from the provided Iterable. Subsequent changes to the Iterable
   * will <b>not</b> be reflected in the FunList.
   */
  public static <T> FunList<T> funList(Iterable<T> delegate) {
    if (delegate instanceof FunList) return (FunList<T>) delegate;
    return FunctionalList.extendList(ImmutableList.copyOf(delegate));
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
   * <p>
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
   *
   * @see FunPairs
   * @see #funPairs(Map)
   */
  public static <K, V> FunPairs<K, V> funPairs(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return FunctionalPairs.extendPairs(entries);
  }

  /**
   * Wraps the {@link Map#entrySet} from the provided Map with a {@link FunPairs}. Changes to the underlying Map
   * will be reflected in the returned FunPairs.
   *
   * @see FunPairs
   */
  public static <K, V> FunPairs<K, V> funPairs(Map<K, V> map) {
    return FunctionalPairs.extendMap(map);
  }


  @SuppressWarnings({"unchecked"})
  public static <T> FunList<T> emptyList() {
    return FunctionalList.EMPTY;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> FunSet<T> emptySet() {
    return FunctionalSet.EMPTY;
  }

  public static <K, V> FunPairs<K, V> emptyPairs() {
    return blindCast(FunctionalPairs.EMPTY);
  }


  public static <T> FunIterable<T> interleave(final Iterable<? extends Iterable<? extends T>> streams) {
    return (AbstractFunIterable<T>) () -> interleavedIterator(Iterables.transform(streams, it -> it.iterator()));
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

  static EmptyIterable EMPTY = new EmptyIterable();

  @SuppressWarnings({"unchecked"})
  static class EmptyIterable<T> implements FunIterable<T> {
    private static final LazyReference LAZY_LIST_SUPPLIER = new LazyReference() {
      public Object get() {
        return emptyList();
      }

      protected Object supplyValue() {
        throw new UnsupportedOperationException(".supplyValue has not been implemented");
      }
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

    public FunIterable zipWith(Iterable other, F2 visitor) {
      return this;
    }

    public FunPairs zip(Iterable values) {
      return NO_PAIRS;
    }

    public FunIterable mapWithIndex(F2 visitor) {
      return this;
    }

    public FunPairs zipWithIndex() {
      return NO_PAIRS;
    }

    public Maybe reduce(F2 reducer) {
      return Maybe.not();
    }

    public FunPairs asKeysToFlattened(Function valuesComputer) {
      return NO_PAIRS;
    }

    public FunPairs asValuesFromFlattened(Function keysComputer) {
      return NO_PAIRS;
    }

    public Object foldLeft(Object input, F2 folder) {
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

    public FunSet toSet() {
      return FunctionalSet.EMPTY;
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

    public int sum(ToIntFunction valueAccessor) {
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
    public FunIterable prepend(Object firstElement) {
      return funListOf(firstElement);
    }

    @Override
    public FunIterable append(Object firstElement) {
      return prepend(firstElement);
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
    @Override
    public boolean equals(@Nullable Object object) {
      return object == this || (object instanceof Iterable && (Iterables.isEmpty((Iterable) object)));
    }

    @Override
    public int hashCode() {
      return delegate().hashCode();
    }

    @Override
    public void run() {
    }
  }
}

class FunctionalIterable<T> implements Functional<T> {
  private final Iterable<T> delegate;

  public FunctionalIterable(Iterable<T> delegate) {
    this.delegate = checkNotNull(delegate, "iterable");
  }

  @Override
  public Iterable<T> delegate() {
    return delegate;
  }

  @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
  @Override
  public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }
}
