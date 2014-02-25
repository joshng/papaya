package joshng.util.collect;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkState;

/**
 * User: josh
 * Date: 3/20/12
 * Time: 9:50 PM
 */
public class IteratorIterable<T> implements Iterable<T> {
    private final Iterator<T> iterator;
    private final AtomicBoolean consumed = new AtomicBoolean();

    public IteratorIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public Iterator<T> iterator() {
        checkState(consumed.compareAndSet(false, true), "IteratorIterable was already consumed");
        return iterator;
    }

    public static <T> Iterable<T> of(Iterator<T> iterator) {
        return new IteratorIterable<T>(iterator);
    }
}
