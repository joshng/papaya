package joshng.util.concurrent;

import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.F2;
import joshng.util.collect.Nothing;

/**
 * User: josh
 * Date: 8/11/14
 * Time: 3:41 PM
 */
public class ErrorCollectingCompletionTracker extends ParallelFold<Object, Nothing> {
  private static final F2<Object, Object, Nothing> FOLD_NOTHING = F2.constant(Nothing.NOTHING);

  public ErrorCollectingCompletionTracker() {
    super(Nothing.NOTHING, false, MoreExecutors.sameThreadExecutor(), FOLD_NOTHING);
  }
}
