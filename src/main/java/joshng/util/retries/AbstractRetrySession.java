package joshng.util.retries;

import com.google.common.base.Throwables;
import joshng.util.concurrent.FunFuture;
import joshng.util.concurrent.Promise;
import joshng.util.exceptions.FatalErrorHandler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: Oct 20, 2010
 * Time: 1:29:26 PM
 */
public abstract class AbstractRetrySession implements RetrySession {
  public <T> T retry(Callable<T> callable) {
    Exception exception;
    do {
      try {
        return callable.call();
      } catch (Exception e) {
        exception = e;
      }
    } while (sleepBeforeRetry(exception));

    throw onAborted(exception);
  }

  public final void retry(Runnable runnable) {
    retry(Executors.callable(runnable));
  }

  public final <T> FunFuture<T> retryAsync(final ScheduledExecutorService scheduler, final Callable<? extends FunFuture<T>> jobStarter) {
    return new AsyncRetryPromise<>(scheduler, jobStarter);
  }

  protected abstract long computeCurrentDelay();

  protected abstract boolean canRetry(Exception e);

  protected abstract boolean canRetryAfterDelay(long currentDelay);

  protected abstract void willRetry();

  protected abstract boolean sleepBeforeRetry(Exception e);

  protected RuntimeException onAborted(Exception exception) {
    Throwables.propagateIfPossible(exception);
    throw new RetryAbortedException(exception);
  }


  private class AsyncRetryPromise<T> extends Promise<T> implements Runnable {
    private final ScheduledExecutorService scheduler;
    private final Callable<? extends FunFuture<T>> jobStarter;

    public AsyncRetryPromise(ScheduledExecutorService scheduler, Callable<? extends FunFuture<T>> jobStarter) {
      this.scheduler = scheduler;
      this.jobStarter = jobStarter;
      try {
        schedule(0);
      } catch (Exception rejectedExecutionException) {
        setFailure(rejectedExecutionException); // may retry
      }
    }

    public void run() {
      completeWithResultOf(jobStarter);
    }

    @Override
    protected boolean handleFailure(Throwable failure) {
      Exception e = FatalErrorHandler.castOrDie(failure);
      if (canRetry(e)) {
        long delay = computeCurrentDelay();
        if (canRetryAfterDelay(delay)) {
          willRetry();
          schedule(delay);
          return false;
        }
      }
      return fail(failure);
    }

    private void schedule(long delay) {
      attachFutureCompletion(scheduler.schedule(this, delay, TimeUnit.MILLISECONDS));
    }
  }
}
