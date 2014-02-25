package joshng.util.collect;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;

/**
 * User: josh
 * Date: 3/18/12
 * Time: 10:40 PM
 */
public class AppendedIterable<T> implements Iterable<T> {
    private final Iterable<? extends T> init;
    private final T last;

    public static <T> AppendedIterable<T> of(Iterable<? extends T> init, T last) {
        return new AppendedIterable<T>(init, last);
    }

    public AppendedIterable(Iterable<? extends T> init, T last) {
        this.init = init;
        this.last = last;
    }

    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            Iterator<? extends T> initIterator = init.iterator();

            @Override
            protected T computeNext() {
                T next;
                if (initIterator == null) {
                    next = endOfData();
                } else if (initIterator.hasNext()) {
                    next = initIterator.next();
                } else {
                    initIterator = null;
                    next = last;
                }
                return next;
            }
        };
    }
}
