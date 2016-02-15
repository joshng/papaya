package com.joshng.util.exceptions;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * User: josh
 * Date: Sep 11, 2010
 * Time: 10:51:43 AM
 */
public class ExceptionPolicy {
  private final List<IExceptionHandler> exceptionHandlers = Lists.newArrayList();

  public ExceptionPolicy add(IExceptionHandler handler) {
    exceptionHandlers.add(handler);
    return this;
  }

  public boolean apply(Throwable t) {
    return Exceptions.handleCause(t, exceptionHandlers);
  }

  public <T extends Throwable> void applyOrThrow(T t) throws T {
    Exceptions.handleCauseOrThrow(t, exceptionHandlers);
  }
}
