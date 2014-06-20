package joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkState;

/**
 * User: josh
 * Date: 6/25/13
 * Time: 12:59 PM
 */
public abstract class AbstractCompletionTracker<I, O> {
  private final ListeningExecutorService jobCompletionExecutor;
  private final Promise<O> completionPromise = Promise.newPromise();
  private final AtomicLong startedCount = new AtomicLong();
  private final AtomicLong completedCount = new AtomicLong();
  private final AtomicBoolean noMore = new AtomicBoolean();
  private final AtomicBoolean markedComplete = new AtomicBoolean();

  public AbstractCompletionTracker(ListeningExecutorService jobCompletionExecutor) {
    this.jobCompletionExecutor = jobCompletionExecutor;
  }

  public FunFuture<O> getCompletionFuture() {
    return completionPromise;
  }

  public boolean isDone() {
    return completionPromise.isDone();
  }

  public AbstractCompletionTracker<I, O> trackAll(Iterable<? extends ListenableFuture<? extends I>> futures) {
    for (ListenableFuture<? extends I> future : futures) {
      track(future);
    }
    return this;
  }

  public <F extends ListenableFuture<? extends I>> F track(final F job) {
    boolean doSubmit;
    synchronized (noMore) {
      doSubmit = !(noMore.get() || completionPromise.isDone());
      if (doSubmit) startedCount.incrementAndGet();
    }
    if (doSubmit) {
      job.addListener(() -> {
        try {
          handleCompletedJob(job);
        } finally {
          completedCount.incrementAndGet();
          checkDone();
        }
      }, jobCompletionExecutor);
    } else {
      job.cancel(completionPromise.wasCancelledWithInterruption());
    }
    return job;
  }

  protected abstract void handleCompletedJob(ListenableFuture<? extends I> job);

  protected abstract void completePromise(Promise<O> completionPromise);

  /**
   * Completes the completion-future with the given exception, and causes all subsequently-submitted
   * jobs to be rejected/cancelled.
   *
   * @return true if this exception was applied to the completion of this tracker
   */
  protected boolean abort(Throwable e) {
    return completionPromise.setFailure(e);
  }

  public long getIncompleteJobCount() {
    return startedCount.get() - completedCount.get();
  }

  public long getCompletedJobCount() {
    return completedCount.get();
  }

  public FunFuture<O> setNoMoreJobs() {
    synchronized (noMore) {
      checkState(noMore.compareAndSet(false, true), "Called setNoMoreJobs more than once");
      checkDone();
    }
    return completionPromise;
  }

  private void checkDone() {
    if (noMore.get()
            && getIncompleteJobCount() == 0
            && markedComplete.compareAndSet(false, true)) {
      completePromise(completionPromise);
    }
  }
}
