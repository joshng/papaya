package joshng.util.collect;

import java.util.Iterator;

/**
 * User: josh
 * Date: Sep 19, 2011
 * Time: 9:48:16 PM
 */
public interface IncrementingIterable extends AbstractFunIterable<Integer> {
  default Iterator<Integer> iterator() {
    return new IncrementingIterator(getStart(), getStep());
  }

  int getStart();

  int getStep();
}
