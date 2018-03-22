package com.joshng.util.concurrent.trackers;

import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.MoreExecutors;
import com.joshng.util.blocks.Sink;
import com.joshng.util.collect.FunList;
import com.joshng.util.collect.Functional;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by: josh 11/15/13 1:39 PM
 */
public class IncrementalFutureList<T> extends AbstractIndependentCompletionTracker<T, FunList<T>> {
  private final Queue<CompletableFuture<? extends T>> futures = Queues.newLinkedBlockingQueue();

  public IncrementalFutureList() {
    super(MoreExecutors.directExecutor());
    getCompletionFuture().uponFailure(new Sink<Throwable>() {
      @Override
      public void accept(Throwable value) {
        for (CompletionStage<? extends T> future : futures) {
          future.toCompletableFuture().cancel(false);
        }
      }
    });
  }

  @Override
  public <F extends CompletionStage<? extends T>> F track(F job) {
    futures.offer(job.toCompletableFuture());
    return super.track(job);
  }

  @Override
  protected void handleCompletedJob(CompletionStage<? extends T> job) throws Exception {
    job.toCompletableFuture().join();
  }

  @Override
  protected FunList<T> computeResult() {
    return Functional.funList(Iterables.transform(futures, CompletableFuture::join));
  }
}
