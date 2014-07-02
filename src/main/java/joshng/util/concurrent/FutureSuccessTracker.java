package joshng.util.concurrent;

/**
 * Created by: josh 10/11/13 6:05 PM
 */

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import joshng.util.blocks.Tapper;
import joshng.util.collect.Nothing;

import java.util.function.Consumer;

/**
 * A {@link FutureCompletionTracker} that aborts if any {@link #track submitted} job fails.
 * The {@link FunFuture} returned by {@link #getCompletionFuture()} (and {@link #setNoMoreJobs()}) will
 * reflect the exception thrown by the first failed job, if any.
 */
public class FutureSuccessTracker extends FutureCompletionTracker {
  public FutureSuccessTracker() {
    super();
  }

  public static FunFuture<Nothing> collect(Consumer<FutureSuccessTracker> block) {
    return FunFuture.callSafely(() -> Tapper.extendConsumer(block).apply(new FutureSuccessTracker()).setNoMoreJobs());
  }

  @Override
  protected void handleCompletedJob(ListenableFuture<?> job) throws Exception {
    Uninterruptibles.getUninterruptibly(job);
  }
}
