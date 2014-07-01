package joshng.util.concurrent;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.Sink;
import joshng.util.collect.FunList;

import java.util.Queue;

/**
 * Created by: josh 11/15/13 1:39 PM
 */
public class IncrementalFutureList<T> extends AbstractIndependentCompletionTracker<T, FunList<T>> {
  private final Queue<ListenableFuture<? extends T>> futures = Queues.newLinkedBlockingQueue();

  public IncrementalFutureList() {
    super(MoreExecutors.sameThreadExecutor());
    getCompletionFuture().uponFailure(new Sink<Throwable>() {
      @Override
      public void accept(Throwable value) {
        for (ListenableFuture<? extends T> future : futures) {
          future.cancel(false);
        }
      }
    });
  }

  @Override
  public <F extends ListenableFuture<? extends T>> F track(F job) {
    futures.offer(job);
    return super.track(job);
  }

  @Override
  protected void handleCompletedJob(ListenableFuture<? extends T> job) throws Exception {
    job.get();
  }

  @Override
  protected FunList<T> computeResult() {
    return FunFuture.<T>getFromFuture().transform(futures).toList();
  }
}
