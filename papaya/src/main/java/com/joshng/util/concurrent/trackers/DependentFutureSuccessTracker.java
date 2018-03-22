package com.joshng.util.concurrent.trackers;

import java.util.concurrent.CompletionStage;

/**
 * User: josh
 * Date: 6/30/14
 * Time: 12:23 PM
 */
public class DependentFutureSuccessTracker extends DependentFutureCompletionTracker {
  @Override
  protected void handleCompletedJob(CompletionStage<?> job) throws Exception {
    job.toCompletableFuture().get();
  }
}
