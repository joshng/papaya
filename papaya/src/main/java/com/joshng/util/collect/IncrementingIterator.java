package com.joshng.util.collect;

import java.util.Iterator;

/**
 * User: josh
 * Date: Sep 19, 2011
 * Time: 9:49:05 PM
 */
public class IncrementingIterator implements Iterator<Integer> {
  private final int step;
  private int i;

  public IncrementingIterator(int start, int step) {
    i = start;
    this.step = step;
  }

  public boolean hasNext() {
    return true;
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
