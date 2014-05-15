package joshng.util.exceptions;

import com.google.common.base.Throwables;

/**
 * User: josh
 * Date: Oct 29, 2010
 * Time: 1:50:37 PM
 */
public class ExceptionHandlingRunnable implements Runnable {
  private final Runnable job;
  private final ExceptionPolicy exceptionPolicy;

  public ExceptionHandlingRunnable(Runnable job, ExceptionPolicy exceptionPolicy) {
    this.job = job;
    this.exceptionPolicy = exceptionPolicy;
  }

  public ExceptionPolicy getExceptionPolicy() {
    return exceptionPolicy;
  }

  public void run() {
    try {
      job.run();
    } catch (Throwable t) {
      try {
        exceptionPolicy.applyOrThrow(t);
      } catch (Throwable throwable) {
        throw Throwables.propagate(throwable);
      }
    }
  }
}
