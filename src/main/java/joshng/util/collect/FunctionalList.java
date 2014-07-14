package joshng.util.collect;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.Reflect.blindCast;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 4:26:26 PM
 */
public class FunctionalList<E> extends FunctionalIterable<E> implements FunList<E> {
  FunctionalList(ImmutableList<E> delegate) {
    super(delegate);
  }

  @Override
  public ImmutableList<E> delegate() {
    return (ImmutableList<E>) super.delegate();
  }

  public static <T> FunList<T> extendList(ImmutableList<T> list) {
    if (list.isEmpty()) return Functional.emptyList();
    return new FunctionalList<T>(list);
  }

  public static <T> FunList<T> copyOf(T[] items) {
    if (items.length == 0) return Functional.emptyList();
    return new FunctionalList<T>(ImmutableList.copyOf(items));
  }

  public static <T> FunList<T> copyOf(Iterator<? extends T> items) {
    checkNotNull(items);
    if (!items.hasNext()) return Functional.emptyList();
    return new FunctionalList<T>(ImmutableList.copyOf(items));
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  @Override
  public FunIterable<FunList<E>> partition(final int size) {
    return new FunctionalIterable<FunList<E>>(new Iterable<FunList<E>>() {
      public Iterator<FunList<E>> iterator() {
        return new AbstractIterator<FunList<E>>() {
          private final int end = size();
          private int idx = 0;

          @Override
          protected FunList<E> computeNext() {
            if (idx == end) return endOfData();
            int start = idx;
            idx = Math.min(idx + size, end);
            return FunctionalList.extendList(delegate().subList(start, idx));
          }
        };
      }
    });
  }

  public FunList<E> subList(int fromIndex, int toIndex) {
    return extendList(delegate().subList(fromIndex, toIndex));
  }

  public Maybe<E> head() {
    return isEmpty() ? Maybe.<E>not() : Maybe.definitely(get(0));
  }

  public FunList<E> tail() {
    if (isEmpty()) return this;
    ImmutableList<E> delegate = delegate();
    return extendList(delegate.subList(1, delegate.size()));
  }

  @Override
  public Maybe<E> last() {
    return isEmpty() ? Maybe.<E>not() : Maybe.definitely(delegate().get(size() - 1));
  }

  public FunList<E> limit(int maxElements) {
    return subList(0, Math.min(maxElements, size()));
  }

  public FunList<E> skip(int skippedElements) {
    int size = size();
    return subList(Math.min(skippedElements, size), size);
  }

  public FunList<E> foreach(Consumer<? super E> visitor) {
    super.foreach(visitor);
    return this;
  }

  public FunList<E> toList() {
    return this;
  }

  public FunList<E> reverse() {
    return new FunctionalList<E>(delegate().reverse());
  }

  public void add(int index, E element) {
    throw FunCollection.rejectMutation();
  }

  public boolean addAll(int index, Collection<? extends E> elements) {
    throw FunCollection.rejectMutation();
  }

  public E get(int index) {
    return delegate().get(index);
  }

  public int indexOf(Object element) {
    return delegate().indexOf(element);
  }

  public int lastIndexOf(Object element) {
    return delegate().lastIndexOf(element);
  }

  public ListIterator<E> listIterator() {
    return delegate().listIterator();
  }

  public ListIterator<E> listIterator(int index) {
    return delegate().listIterator(index);
  }

  public E remove(int index) {
    throw FunCollection.rejectMutation();
  }

  public E set(int index, E element) {
    throw FunCollection.rejectMutation();
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <U> FunList<U> cast() {
    return blindCast(this);
  }

  public static class Builder<T> {
    private final ImmutableList.Builder<T> listBuilder = ImmutableList.builder();

    public Builder<T> add(T element) {
      listBuilder.add(element);
      return this;
    }

    public Builder<T> addAll(Iterable<? extends T> elements) {
      listBuilder.addAll(elements);
      return this;
    }

    @SafeVarargs
    public final Builder<T> add(T... elements) {
      listBuilder.add(elements);
      return this;
    }

    public Builder<T> addAll(Iterator<? extends T> elements) {
      listBuilder.addAll(elements);
      return this;
    }

    public FunList<T> build() {
      return extendList(listBuilder.build());
    }
  }

  static final EmptyList EMPTY = new EmptyList();

  @SuppressWarnings({"unchecked"})
  private static class EmptyList extends FunCollection.EmptyCollection implements FunList {

    public ImmutableList delegate() {
      return ImmutableList.of();
    }

    public FunList reverse() {
      return this;
    }

    public EmptyList tail() {
      return this;
    }

    @Override
    public EmptyList foreach(Consumer visitor) {
      return this;
    }

    @Override
    public FunList cast() {
      return this;
    }

    public boolean addAll(int index, Collection c) {
      throw FunCollection.rejectMutation();
    }

    public Object get(int index) {
      throw indexOutOfBounds(index);
    }

    public Object set(int index, Object element) {
      throw FunCollection.rejectMutation();
    }

    public void add(int index, Object element) {
      throw FunCollection.rejectMutation();
    }

    public int indexOf(java.lang.Object o) {
      return -1;
    }

    public int lastIndexOf(java.lang.Object o) {
      return -1;
    }

    public ListIterator listIterator() {
      return Collections.emptyListIterator();
    }

    public ListIterator listIterator(int index) {
      return listIterator();
    }

    public List subList(int fromIndex, int toIndex) {
      if (fromIndex != 0) throw indexOutOfBounds(fromIndex);
      if (toIndex != 0) throw indexOutOfBounds(toIndex);
      return this;
    }

    private IndexOutOfBoundsException indexOutOfBounds(int index) {
      return new IndexOutOfBoundsException(String.valueOf(index));
    }

    @Override
    public boolean equals(@Nullable Object object) {
      return object == this || (object instanceof List && ((List) object).isEmpty());
    }

    @Override
    public int hashCode() {
      return delegate().hashCode();
    }
  }
}
