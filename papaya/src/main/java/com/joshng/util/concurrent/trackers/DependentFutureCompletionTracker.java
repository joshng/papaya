package com.joshng.util.concurrent.trackers;

import com.google.common.util.concurrent.MoreExecutors;
import com.joshng.util.collect.Nothing;

import java.util.concurrent.CompletionStage;

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
    super(MoreExecutors.directExecutor());
  }

  @Override
  protected void handleCompletedJob(CompletionStage<?> job) throws Exception {
  }

  @Override
  protected Nothing computeResult() {
    return Nothing.NOTHING;
  }
}
