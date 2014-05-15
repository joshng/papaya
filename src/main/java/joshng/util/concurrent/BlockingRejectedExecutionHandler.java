package joshng.util.concurrent;

import java.util.concurrent.*;

/**
 * User: josh
 * Date: 6/27/13
 * Time: 8:50 AM
 */
public class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {
  private final long shutdownPollMicros;

  public BlockingRejectedExecutionHandler(long shutdownPollInterval, TimeUnit timeUnit) {
    this.shutdownPollMicros = timeUnit.toMicros(shutdownPollInterval);
  }

  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      BlockingQueue<Runnable> queue = executor.getQueue();
      do {
        if (executor.isShutdown()) throw reject(r, new RejectedExecutionException());
      } while (!queue.offer(r, shutdownPollMicros, TimeUnit.MICROSECONDS));
    } catch (InterruptedException e) {
      throw reject(r, new RejectedExecutionException(e));
    }
  }

  private RejectedExecutionException reject(Runnable task, RejectedExecutionException e) {
    CancellingRejectedExecutionHandler.cancelFuture(task);
    throw e;
  }
}
