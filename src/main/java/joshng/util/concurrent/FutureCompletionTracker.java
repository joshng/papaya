package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

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
 *
 * Example usage:
 *
 * <pre>{@code
 * FutureCompletionTracker tracker = new FutureCompletionTracker();
 *
 * for (SomeInput input : inputs) {
 *     tracker.submit(startSomeJob(input));
 * }
 *
 * FunFuture<Long> completionFuture = tracker.setNoMoreJobs();
 *
 * long jobCount = completionTracker.getUnchecked(); // waits until all jobs have completed
 *
 *  // ... //
 *
 * FunFuture<Something> startSomeJob(SomeInput input) { ... }
 * }</pre>
 */
public class FutureCompletionTracker extends AbstractCompletionTracker<Object, Long> {
    public FutureCompletionTracker() {
        super(MoreExecutors.sameThreadExecutor());
    }

    @Override
    protected void handleCompletedJob(ListenableFuture<?> job) {
        // nothing to do
    }

    @Override
    protected void completePromise(Promise<Long> completionPromise) {
        completionPromise.setSuccess(getCompletedJobCount());
    }
}
