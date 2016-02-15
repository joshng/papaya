package com.joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * User: josh
 * Date: 6/30/14
 * Time: 12:23 PM
 */
public class DependentFutureSuccessTracker extends DependentFutureCompletionTracker {
  @Override
  protected void handleCompletedJob(ListenableFuture<?> job) throws Exception {
    Uninterruptibles.getUninterruptibly(job);
  }
}
