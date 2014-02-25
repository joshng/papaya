package joshng.util.collect;

import com.google.common.collect.ObjectArrays;
import joshng.util.blocks.Consumer;

import java.util.Collection;

/**
 * User: josh
 * Date: 12/29/11
 * Time: 3:13 PM
 */
public abstract class FunctionalCollection<T> extends Functional<T> implements FunCollection<T> {
    @Override
    public abstract Collection<T> delegate();

    public FunCollection<T> foreach(Consumer<? super T> visitor) {
        super.foreach(visitor);
        return this;
    }

    public int size() {
        return delegate().size();
    }

    protected static UnsupportedOperationException rejectMutation() {
        return new UnsupportedOperationException("FunctionalCollections are immutable");
    }

    public boolean removeAll(Collection<?> collection) {
        throw rejectMutation();
    }

    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    public boolean contains(Object object) {
        return delegate().contains(object);
    }

    public Object[] toArray() {
        return delegate().toArray();
    }

    public <T> T[] toArray(T[] array) {
        //noinspection SuspiciousToArrayCall
        return delegate().toArray(array);
    }

    public boolean add(T element) {
        throw rejectMutation();
    }

    public boolean remove(Object object) {
        throw rejectMutation();
    }

    public boolean containsAll(Collection<?> collection) {
        return delegate().containsAll(collection);
    }

    public boolean addAll(Collection<? extends T> collection) {
        throw rejectMutation();
    }

    public boolean retainAll(Collection<?> collection) {
        throw rejectMutation();
    }

    public void clear() {
        throw rejectMutation();
    }

    abstract static class EmptyCollection extends EmptyIterable implements Collection {
        public abstract Collection delegate();

        public Object[] toArray() {
            return new Object[] {};
        }

        public Object[] toArray(Object[] a) {
            return ObjectArrays.newArray(a, 0);
        }

        public boolean add(Object o) {
            throw rejectMutation();
        }

        public boolean remove(Object o) {
            throw rejectMutation();
        }

        public boolean containsAll(Collection c) {
            return c.isEmpty();
        }

        public boolean addAll(Collection c) {
            throw rejectMutation();
        }

        public boolean removeAll(Collection c) {
            throw rejectMutation();
        }

        public boolean retainAll(Collection c) {
            throw rejectMutation();
        }

        public void clear() {
            throw rejectMutation();
        }

        public Object remove(int index) {
            throw rejectMutation();
        }

        public boolean contains(Object o) {
            return false;
        }
    }
}
