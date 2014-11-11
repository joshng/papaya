package joshng.util.exceptions;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;

/**
 * Created by: josh 10/10/13 12:27 PM
 */
public class CountAndLogUncaughtExceptionHandler extends LoggingUncaughtExceptionHandler {
  private final Meter uncaughtExceptionCounter;

  public CountAndLogUncaughtExceptionHandler(Logger logger, Meter uncaughtExceptionCounter) {
    super(logger);
    this.uncaughtExceptionCounter = uncaughtExceptionCounter;
  }

  @Override public void uncaughtException(Thread t, Throwable e) {
    uncaughtExceptionCounter.mark();
    super.uncaughtException(t, e);
  }
}
