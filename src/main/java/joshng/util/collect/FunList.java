package joshng.util.collect;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: 10/3/11
 * Time: 4:05 PM
 */
public interface FunList<T> extends List<T>, FunCollection<T> {
    FunList<T> tail();
    FunList<T> foreach(Consumer<? super T> visitor);
    ImmutableList<T> delegate();
    FunList<T> reverse();
    <S> FunList<S> cast();
    FunIterable<FunList<T>> partition(int size);

    @Override default Object[] toArray() { return FunCollection.super.toArray(); }
    @Override default <T1> T1[] toArray(T1[] a) { return FunCollection.super.toArray(a); }
    @Override default boolean contains(Object o){ return FunCollection.super.contains(o); }
    @Override default boolean containsAll(Collection<?> collection) { return FunCollection.super.containsAll(collection); }
    @Override default boolean isEmpty() { return FunCollection.super.isEmpty(); }
    @Override default int size() { return FunCollection.super.size(); }
    @Override default Iterator<T> iterator() { return FunCollection.super.iterator(); }
    @Override default boolean add(Object o) { throw FunCollection.rejectMutation(); }
    @Override default boolean remove(Object o) { throw FunCollection.rejectMutation(); }
    @Override default void clear() { throw FunCollection.rejectMutation(); }
    @Override default boolean retainAll(Collection<?> collection) { throw FunCollection.rejectMutation(); }
    @Override default boolean removeAll(Collection<?> collection) { throw FunCollection.rejectMutation(); }
    @Override default boolean addAll(Collection<? extends T> collection) { throw FunCollection.rejectMutation(); }
}
