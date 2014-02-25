package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by: josh 10/11/13 6:05 PM
 */

/**
 * A {@link FutureCompletionTracker} that aborts if any {@link #submit submitted} job fails.
 * The {@link FunFuture} returned by {@link #getCompletionFuture()} (and {@link #setNoMoreJobs()}) will
 * reflect the exception thrown by the first failed job, if any.
 */
public class FutureSuccessTracker extends FutureCompletionTracker {
    @Override
    protected void handleCompletedJob(ListenableFuture<?> job) {
        try {
            FunFutures.getUnchecked(job);
        } catch (Throwable e) {
            abort(e);
        }
    }
}
