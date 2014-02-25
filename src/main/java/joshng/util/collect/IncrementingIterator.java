package joshng.util.collect;

import com.google.common.base.Predicate;

import java.util.Iterator;

/**
* User: josh
* Date: Sep 19, 2011
* Time: 9:49:05 PM
*/
public class IncrementingIterator implements Iterator<Integer> {
    private final int step;
    private final Predicate<Integer> inclusionPredicate;
    private int i;

    public IncrementingIterator(int start, int step, Predicate<Integer> inclusionPredicate) {
        i = start;
        this.step = step;
        this.inclusionPredicate = inclusionPredicate;
    }

    public boolean hasNext() {
        return inclusionPredicate.apply(i);
    }

    public Integer next() {
        int current = i;
        i += step;
        return current;
    }

    public void remove() {
        // what are you, a wiseguy??
        throw new UnsupportedOperationException();
    }
}
