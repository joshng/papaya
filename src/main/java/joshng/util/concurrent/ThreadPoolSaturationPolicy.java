package joshng.util.concurrent;

import java.util.concurrent.*;

/**
 * Created by: josh 2/25/14 12:47 AM
 */
public enum ThreadPoolSaturationPolicy implements RejectedExecutionHandler {
  Block {
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
      BlockingQueue<Runnable> queue = executor.getQueue();
      try {
        do {
          checkShutdown(executor, task);
        } while (!queue.offer(task, 2, TimeUnit.SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw reject(task, e);
      }

      // check for shutdown again, to avoid a race that could orphan this task
      if (executor.isShutdown() && queue.remove(task)) throw reject(task, null);
    }
  },
  /**
   * <b>(MINOR) CAVEATS</b>:
   * <ol>
   * <li>The CallerRuns policy may cause the {@link java.util.concurrent.Executor#execute} method to behave
   * inconsistently when the Executor's queue is full (see below for details)</li>
   * <li>{@link java.util.concurrent.ExecutorService#awaitTermination} and {@link java.util.concurrent.ExecutorService#shutdownNow} will not be aware
   * of any tasks still being run by calling threads if the pool was previously saturated</li>
   * <li>{@link java.util.concurrent.ThreadPoolExecutor#getTaskCount} and {@link java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount getCompletedTaskCount} will
   * not include tasks that are run by calling threads</li>
   * </ol>
   * Details:
   * <p>
   * When the queue is <em>not</em> full and tasks are run normally by a thread from the pool,
   * any exceptions thrown by the tasks are absorbed in {@link java.util.concurrent.ThreadPoolExecutor#runWorker},
   * and the {@link java.util.concurrent.ThreadPoolExecutor#beforeExecute} and {@link java.util.concurrent.ThreadPoolExecutor#afterExecute} methods are
   * called surrounding each task's {@link Runnable#run} method.
   * <p>
   * However, if the pool's task-queue <em>is</em> full, {@link ThreadPoolSaturationPolicy#CallerRuns} will simply
   * run the task in the calling thread, bypassing the before/after methods and throwing any exceptions directly
   * to the caller.
   * <p>
   * To avoid the exception-handling oddity, do not use {@link java.util.concurrent.Executor#execute}; use {@link java.util.concurrent.ExecutorService#submit}
   * instead (which typically wraps every task in a {@link java.util.concurrent.FutureTask} that handles exceptions internally).
   * <p>
   * There is no simple workaround for the beforeExecute/afterExecute misbehavior, but unless you've overridden
   * one of these empty template-methods with some custom ThreadPoolExecutor subclass (unlikely), this doesn't matter.
   */
  CallerRuns {
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
      checkShutdown(executor, task);
      task.run();
    }
  },
  Abort {
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor e) {
      throw reject(task, null);
    }
  },
  Discard {
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor e) {
      cancel(task);
    }
  },
  DiscardOldest {
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
      checkShutdown(executor, task);
      cancel(executor.getQueue().poll());
      executor.execute(task);
    }
  };

  public static void cancel(Runnable task) {
    if (task instanceof Future) ((Future) task).cancel(false);
  }

  private static void checkShutdown(ThreadPoolExecutor executor, Runnable task) {
    if (executor.isShutdown()) throw reject(task, null);
  }

  private static RejectedExecutionException reject(Runnable task, Throwable cause) throws RejectedExecutionException {
    cancel(task);
    throw new RejectedExecutionException(cause);
  }
}
