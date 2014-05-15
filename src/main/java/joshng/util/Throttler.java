package joshng.util;

import com.google.common.base.Ticker;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: josh
 * Date: 11/14/12
 * Time: 9:49 AM
 */
public class Throttler {
  private final AtomicLong nextAvailableTime = new AtomicLong();
  private final long intervalMillis;
  private final Ticker ticker;

  public Throttler(long interval, TimeUnit intervalUnit) {
    this(interval, intervalUnit, Ticker.systemTicker());
  }

  public Throttler(long interval, TimeUnit intervalUnit, Ticker ticker) {
    this.ticker = ticker;
    this.intervalMillis = intervalUnit.toMillis(interval);
  }

  public boolean tryAcquire() {
    final long whenAvailable = nextAvailableTime.get();
    final long now = getCurrentTime();
    return whenAvailable <= now && nextAvailableTime.compareAndSet(whenAvailable, now + intervalMillis);
  }

  public void logWithThrottling(Logger logger, LogLevel level, String message) {
    if (tryAcquire()) level.log(logger, message);
  }

  public void logWithThrottling(Logger logger, LogLevel level, String format, Object... args) {
    if (tryAcquire()) level.log(logger, format, args);
  }

  public void stacktraceWithThrottling(Logger logger, LogLevel level, Throwable t, String message) {
    if (tryAcquire()) level.stacktrace(logger, t, message);
  }

  public void stacktraceWithThrottling(Logger logger, LogLevel level, Throwable t, String format, Object... args) {
    if (tryAcquire()) level.stacktrace(logger, t, format, args);
  }

  private long getCurrentTime() {
    return ticker.read() / 1000000;
  }

  public void acquire() throws InterruptedException {
    long timeToWait;

    boolean acquired;
    do {
      long now = getCurrentTime();
      long whenAvailable = nextAvailableTime.get();
      timeToWait = whenAvailable - now;
      acquired = nextAvailableTime.compareAndSet(whenAvailable, (timeToWait <= 0 ? now : whenAvailable) + intervalMillis);
    } while (!acquired);

    if (timeToWait > 0) Thread.sleep(timeToWait);
  }
}
