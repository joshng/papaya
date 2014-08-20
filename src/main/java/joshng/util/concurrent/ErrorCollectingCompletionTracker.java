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
  public ErrorCollectingCompletionTracker() {
    super(Nothing.NOTHING, false, MoreExecutors.sameThreadExecutor(), (F2<Object, Nothing, Nothing>)(Object r1, Nothing r2) -> Nothing.NOTHING);
  }
}
