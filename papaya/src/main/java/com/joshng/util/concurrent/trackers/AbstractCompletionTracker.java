package com.joshng.util.concurrent.trackers;


import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.concurrent.Promise;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: josh
 * Date: 6/30/14
 * Time: 11:52 AM
 */
public abstract class AbstractCompletionTracker<I, O> {
  private final Executor jobCompletionExecutor;
  private final Promise<O> completionPromise = Promise.newPromise();
  private final AtomicLong startedCount = new AtomicLong();
  private final AtomicLong completedCount = new AtomicLong();
  private final AtomicBoolean markedComplete = new AtomicBoolean();

  public AbstractCompletionTracker(Executor jobCompletionExecutor) {
    this.jobCompletionExecutor = jobCompletionExecutor;
  }

  protected abstract void handleCompletedJob(CompletionStage<? extends I> job) throws Exception;

  protected abstract O computeResult() throws Exception;

  public FunFuture<O> getCompletionFuture() {
    return completionPromise;
  }

  public boolean isAcceptingNewJobs() {
    return !isDone();
  }

  public boolean isDone() {
    return completionPromise.isDone();
  }

  public AbstractCompletionTracker<I, O> trackAll(Iterable<? extends CompletionStage<? extends I>> futures) {
    for (CompletionStage<? extends I> future : futures) {
      track(future);
    }
    return this;
  }

  public <F extends CompletionStage<? extends I>> F track(final F job) {
    startedCount.incrementAndGet();
    if (!isAcceptingNewJobs()) job.toCompletableFuture().cancel(completionPromise.wasCancelledWithInterruption());
    job.whenCompleteAsync((r, x) -> {
      try {
        handleCompletedJob(job);
      } catch (Exception e) {
        abort(FunFuture.unwrapExecutionException(e));
      } finally {
        completedCount.incrementAndGet();
        checkDone();
      }
    }, jobCompletionExecutor);
    return job;
  }

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

  protected void checkDone() {
    if (allJobsDone() && markedComplete.compareAndSet(false, true)) {
      completionPromise.complete(this::computeResult);
    }
  }

  protected boolean allJobsDone() {
    return getIncompleteJobCount() == 0;
  }
}
