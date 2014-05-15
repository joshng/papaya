package joshng.util.retries;

import joshng.util.concurrent.FunFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * User: josh
 * Date: Oct 15, 2010
 * Time: 6:11:01 PM
 */
public interface RetrySession {
  <T> T retry(Callable<T> closure) throws RetryAbortedException;

  void retry(Runnable runnable) throws RetryAbortedException;

  <T> FunFuture<T> retryAsync(ScheduledExecutorService scheduler, Callable<? extends FunFuture<T>> jobStarter);

  RetrySession NO_RETRY = new AbstractRetrySession() {
    protected boolean sleepBeforeRetry(Exception e) {
      return false;
    }

    public boolean canRetry(Exception e) {
      return false;
    }

    public boolean canRetryAfterDelay(long currentDelay) {
      return false;
    }

    public void willRetry() {
      throw new UnsupportedOperationException("No retries permitted");
    }

    public long computeCurrentDelay() {
      return 0;
    }
  };
}
