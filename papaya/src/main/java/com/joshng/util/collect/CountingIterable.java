package com.joshng.util.collect;

/**
* User: josh
* Date: 6/15/14
* Time: 2:18 PM
*/
public class CountingIterable implements IncrementingIterable {
  private final int start;
  private final int step;

  public CountingIterable(int start, int step) {
    this.start = start;
    this.step = step;
  }

  @Override
  public int getStart() {
    return start;
  }

  @Override
  public int getStep() {
    return step;
  }
}
