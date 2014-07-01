package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.collect.Nothing;

/**
 * User: josh
 * Date: 6/30/14
 * Time: 12:18 PM
 */

/**
 * A FutureCompletionTracker that is marked complete as soon as it has no incomplete jobs left.
 * Assuming the jobs may complete asynchronously, this requires that all tracked jobs be submitted prior to the
 * completion of previously-submitted jobs...  This is hard to explain :) -jg
 */
public class DependentFutureCompletionTracker extends AbstractCompletionTracker<Object, Nothing> {
  public DependentFutureCompletionTracker() {
    super(MoreExecutors.sameThreadExecutor());
  }

  @Override
  protected void handleCompletedJob(ListenableFuture<?> job) throws Exception {
  }

  @Override
  protected Nothing computeResult() {
    return Nothing.NOTHING;
  }
}
