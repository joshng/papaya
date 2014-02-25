package joshng.util.collect;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A simple <em>non-thread-safe</em> queue that allows traversal in FIFO order.
 * Differs from the standard FIFO implementations (LinkedList, Deque, BlockingQueue) by always exposing
 * new items added <em>during iteration</em> to the active iterator.
 *
 * IMPORTANT NOTE: this queue only supports a single iterator at a time; interacting with multiple
 * iterators simultaneously will result in an IllegalStateException.
 */
public class SimpleIterableQueue<T> extends AbstractQueue<T> {
    private final Node<T> sentinel = new Node<T>(null);

    private final int maxSize;
    private Node<T> last;
    private WeakReference<Iter> currentIterator;
    int size;

    public SimpleIterableQueue(int maxSize) {
        this.maxSize = maxSize;
        clear();
    }

    public static <T> SimpleIterableQueue<T> create() {
        return new SimpleIterableQueue<T>();
    }

    public SimpleIterableQueue() {
        this(Integer.MAX_VALUE);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        currentIterator = null;
        last = sentinel.next = sentinel;
        size = 0;
    }

    @Nullable
    public T poll() {
        if (isEmpty()) return null;

        return removeAfter(sentinel);
    }

    @Nullable
    public T peek() {
        return sentinel.next.value;
    }

    public boolean offer(@Nonnull T value) {
        checkNotNull(value, "Tried to insert null");
        if (size == maxSize) return false;

        Node<T> node = new Node<T>(value);
        node.next = sentinel;
        last.next = node;
        last = node;
        size++;
        return true;
    }

    public Iterator<T> iterator() {
        Iter iter = new Iter();
        currentIterator = new WeakReference<Iter>(iter);
        return iter;
    }

    private class Iter implements Iterator<T> {
        private Node<T> current = sentinel;
        private Node<T> prev = current;

        public boolean hasNext() {
            checkStaleIterator();
            return current.next != sentinel;
        }

        public T next() {
            checkStaleIterator();
            checkState(hasNext(), "No more elements");
            prev = current;
            current = current.next;
            return current.value;
        }

        public void remove() {
            checkStaleIterator();
            checkState(prev != current, "Already removed this element, or iterator was not started");
            removeAfter(prev);
            current = prev;
        }

        private void checkStaleIterator() {
            checkState(currentIterator != null && currentIterator.get() == this, "Stale iterator: another iterator");
        }
    }

    private T removeAfter(Node<T> prev) {
        Node<T> removed = prev.next;
        assert removed != sentinel;
        prev.next = removed.next;
        if (last == removed) last = prev;
        size--;
        return removed.value;
    }

    private static class Node<T> {
        private final T value;
        Node<T> next = null;

        private Node(T value) {
            this.value = value;
        }
    }
}
