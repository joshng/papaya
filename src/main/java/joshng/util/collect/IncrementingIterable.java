package joshng.util.collect;

import com.google.common.base.Predicate;
import joshng.util.blocks.Pred;

import java.util.Iterator;

/**
 * User: josh
 * Date: Sep 19, 2011
 * Time: 9:48:16 PM
 */
public class IncrementingIterable implements AbstractFunIterable<Integer> {
  private final int start;
  private final int step;
  private final Predicate<Integer> inclusionPredicate;

  private static final IncrementingIterable FROM_ZERO = new IncrementingIterable(0, 1, Pred.<Integer>alwaysTrue());

  public static IncrementingIterable countFromZero() {
    return FROM_ZERO;
  }

  public IncrementingIterable(int start, int step, Predicate<Integer> inclusionPredicate) {
    this.start = start;
    this.step = step;
    this.inclusionPredicate = inclusionPredicate;
  }

  public Iterator<Integer> iterator() {
    return new IncrementingIterator(start, step, inclusionPredicate);
  }
}
