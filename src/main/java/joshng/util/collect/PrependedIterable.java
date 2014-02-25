package joshng.util.collect;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;

/**
 * User: josh
 * Date: 3/18/12
 * Time: 10:40 PM
 */
public class PrependedIterable<T> implements Iterable<T> {
    private final T first;
    private final Iterable<? extends T> rest;

    public static <T> PrependedIterable<T> of(T first, Iterable<? extends T> rest) {
        return new PrependedIterable<T>(first, rest);
    }

    public PrependedIterable(T first, Iterable<? extends T> rest) {
        this.first = first;
        this.rest = rest;
    }

    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            Iterator<? extends T> restIterator = null;
            @Override
            protected T computeNext() {
                if (restIterator == null) {
                    restIterator = rest.iterator();
                    return first;
                }
                return restIterator.hasNext() ? restIterator.next() : endOfData();
            }
        };
    }
}
