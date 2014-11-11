package joshng.util.exceptions;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;

/**
 * Created by: josh 10/10/13 12:27 PM
 */
public class CountAndLogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
  private final Logger logger;
  private final Meter uncaughtExceptionCounter;

  public CountAndLogUncaughtExceptionHandler(Logger logger, Meter uncaughtExceptionCounter) {
    this.logger = logger;
    this.uncaughtExceptionCounter = uncaughtExceptionCounter;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    uncaughtExceptionCounter.mark();
    logger.error("Uncaught exception on thread {}", t, FatalErrorHandler.castOrDie(e));
  }
}
