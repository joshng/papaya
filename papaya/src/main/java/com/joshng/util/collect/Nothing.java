package com.joshng.util.collect;

import com.joshng.util.blocks.Source;
import com.joshng.util.concurrent.FunFuture;

/**
 * User: josh
 * Date: 2/13/13
 * Time: 6:46 PM
 * <p>
 * A "Unit" type; like {@link Void}, but useful for representing the lack of information without resorting to 'null'
 */
public final class Nothing {
  public static final Nothing NOTHING = new Nothing();
  public static final FunFuture<Nothing> FUTURE = FunFuture.immediateFuture(NOTHING);
  public static final Source<Nothing> SOURCE = Source.ofInstance(NOTHING);

  private Nothing() {
  }
}
