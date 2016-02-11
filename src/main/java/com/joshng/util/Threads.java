package com.joshng.util;

import com.joshng.util.exceptions.UncheckedInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * User: josh
 * Date: Apr 28, 2011
 * Time: 6:20:12 PM
 */
public class Threads {
  private static final Logger LOG = LoggerFactory.getLogger(Threads.class);

  public static void sleep(long time, TimeUnit timeUnit) {
    try {
      timeUnit.sleep(time);
    } catch (InterruptedException e) {
      throw UncheckedInterruptedException.propagate(e);
    }
  }

  public static boolean sleepUntil(Instant instant, Clock clock, Duration pollInterval, BooleanSupplier shouldContinue) {
    long maxSleep = pollInterval.toNanos();
    long sleepTime;
    while ((sleepTime = clock.instant().until(instant, ChronoUnit.NANOS)) > 0 && shouldContinue.getAsBoolean()) {
      sleep(Math.min(sleepTime, maxSleep), TimeUnit.NANOSECONDS);
    }
    return shouldContinue.getAsBoolean();
  }

  public static List<Thread> getAllThreads() {
    ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
    while (null != rootGroup.getParent()) {
      rootGroup = rootGroup.getParent();
    }
    Thread[] threads = new Thread[rootGroup.activeCount() * 2];
    int count = rootGroup.enumerate(threads, true);
    return Arrays.asList(Arrays.copyOf(threads, count));
  }

  public static void dumpAllThreads() {
    List<Thread> threads = getAllThreads();
    LOG.info("threads: {}", threads.size());
    for (Thread thread : threads) {
      ThreadGroup tg = thread.getThreadGroup();
      if (tg == null) continue; //thread has terminated
      LOG.info(String.format("  %4d %8s %s", thread.getId(), tg.getName(), thread.getName()));
    }
  }
}

