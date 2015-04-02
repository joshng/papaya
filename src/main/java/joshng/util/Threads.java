package joshng.util;

import joshng.util.exceptions.UncheckedInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

