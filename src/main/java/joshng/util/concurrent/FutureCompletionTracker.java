package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.collect.Nothing;

/**
 * User: josh
 * Date: 6/25/13
 * Time: 1:05 PM
 */

/**
 * Provides a single {@link FunFuture} that represents the completion of a stream of {@link ListenableFuture ListenableFutures},
 * without regard to whether they succeed or fail. Particularly useful in shutdown scenarios, where you want to know
 * when all asynchronous jobs have completed, but aren't concerned with their outcomes. If you do care about results or failures,
 * consider using a {@link FutureSuccessTracker} or {@link ParallelFold} instead.<p/>
 * <p>
 * Example usage:
 * <p>
 * <pre>{@code
 * FutureCompletionTracker tracker = new FutureCompletionTracker();
 * <p>
 * for (SomeInput input : inputs) {
 *     tracker.track(startSomeJob(input));
 * }
 * <p>
 * FunFuture<Long> completionFuture = tracker.setNoMoreJobs();
 * <p>
 * long jobCount = completionFuture.getUnchecked(); // waits until all jobs have completed
 * <p>
 *  // ... //
 * <p>
 * FunFuture<Something> startSomeJob(SomeInput input) { ... }
 * }</pre>
 */
public class FutureCompletionTracker extends AbstractIndependentCompletionTracker<Object, Nothing> {
  public FutureCompletionTracker() {
    super(MoreExecutors.directExecutor());
  }

  @Override
  protected void handleCompletedJob(ListenableFuture<?> job) throws Exception {
  }

  @Override
  protected Nothing computeResult() throws Exception {
    return Nothing.NOTHING;
  }
}
