package com.joshng.util.exceptions;

import com.google.common.base.Throwables;

import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: Oct 29, 2010
 * Time: 1:50:37 PM
 */
public class ExceptionHandlingCallable<T> implements Callable<T> {
  private final Callable<T> job;
  private final ExceptionPolicy exceptionPolicy;

  public ExceptionHandlingCallable(Callable<T> job, ExceptionPolicy exceptionPolicy) {
    this.job = job;
    this.exceptionPolicy = exceptionPolicy;
  }

  public T call() throws Exception {
    try {
      return job.call();
    } catch (Throwable t) {
      try {
        exceptionPolicy.applyOrThrow(t);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
      throw propagate(t);
    }
  }

  private Exception propagate(Throwable throwable) throws Exception {
    Throwables.propagateIfPossible(throwable, Exception.class);
    throw Throwables.propagate(throwable);
  }
}
