package com.joshng.util.exceptions;

import org.slf4j.Logger;

/**
 * User: josh
 * Date: 11/11/14
 * Time: 2:02 PM
 */
public class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
  protected final Logger logger;

  public LoggingUncaughtExceptionHandler(Logger logger) {this.logger = logger;}

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    logger.error("Uncaught exception on thread {}", t, FatalErrorHandler.castOrDie(e));
  }
}
