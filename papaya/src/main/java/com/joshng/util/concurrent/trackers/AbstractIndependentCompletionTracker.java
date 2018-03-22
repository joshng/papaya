package com.joshng.util.concurrent.trackers;


import com.joshng.util.concurrent.FunFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkState;

/**
 * User: josh
 * Date: 6/25/13
 * Time: 12:59 PM
 */
public abstract class AbstractIndependentCompletionTracker<I, O> extends AbstractCompletionTracker<I,O> {
  private final AtomicBoolean noMore = new AtomicBoolean();

  public AbstractIndependentCompletionTracker(Executor jobCompletionExecutor) {
    super(jobCompletionExecutor);
  }

  @Override
  public boolean isAcceptingNewJobs() {
    return !noMore.get() && super.isAcceptingNewJobs();
  }

  public FunFuture<O> setNoMoreJobs() {
    checkState(noMore.compareAndSet(false, true), "Called setNoMoreJobs more than once");
    checkDone();
    return getCompletionFuture();
  }

  @Override
  public AbstractIndependentCompletionTracker<I, O> trackAll(Iterable<? extends CompletionStage<? extends I>> completionStages) {
    super.trackAll(completionStages);
    return this;
  }

  @Override
  protected boolean allJobsDone() {
    return !isAcceptingNewJobs() && super.allJobsDone();
  }
}
