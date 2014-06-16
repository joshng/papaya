package joshng.util.collect;

/**
 * User: josh
 * Date: 6/15/14
 * Time: 2:09 PM
 */
public enum Count implements IncrementingIterable {
  FromZero(0),
  FromOne(1);

  private final int start;

  Count(int start) {
    this.start = start;
  }

  public int getStart() {
    return start;
  }

  @Override
  public int getStep() {
    return 1;
  }
}
