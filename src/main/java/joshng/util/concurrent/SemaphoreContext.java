package joshng.util.concurrent;

import joshng.util.blocks.SideEffect;
import joshng.util.context.TransientContext;
import org.joda.time.DateTime;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 9/25/13
 * Time: 1:48 PM
 */
public class SemaphoreContext implements TransientContext {
  private final Semaphore semaphore;
  private final Releaser singlePermitReleaser = new Releaser(1);
  private final long shutdownPollNanos;
  private volatile boolean shutdown;

  public SemaphoreContext(int permits, long shutdownPollPeriod, TimeUnit shutdownPollUnit) {
    this(permits, false, shutdownPollPeriod, shutdownPollUnit);
  }

  public SemaphoreContext(int permits, boolean fair, long shutdownPollPeriod, TimeUnit shutdownPollUnit) {
    semaphore = new Semaphore(permits, fair);
    this.shutdownPollNanos = shutdownPollUnit.toNanos(shutdownPollPeriod);
  }

  public void shutdown() {
    shutdown = true;
  }

  @Override
  public Releaser enter() {
    try {
      return acquireOnePermit();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public class Releaser implements SideEffect, State {
    private final int permits;

    private Releaser(int permits) {
      this.permits = permits;
    }

    @Override
    public void run() {
      exit();
    }

    @Override
    public void exit() {
      semaphore.release(permits);
    }
  }

  public Releaser acquireOnePermit() throws InterruptedException {
    return acquirePermits(1);
  }

  public Releaser acquirePermits(int permits) throws InterruptedException, ShutdownException {
    acquire(permits);
    return permits == 1 ? singlePermitReleaser : new Releaser(permits);
  }

  public void acquire(int permits) throws InterruptedException {
    int tryCount = 0;
    DateTime blockedSince = null;
    do {
      ShutdownException.throwIf(shutdown);
      if (++tryCount == 1) {
        if (semaphore.tryAcquire(permits)) return;
        blockedSince = new DateTime();
      } else {
        onBlocked(tryCount, blockedSince);
      }
    } while (!semaphore.tryAcquire(permits, shutdownPollNanos, TimeUnit.NANOSECONDS));

    onUnblocked(tryCount, blockedSince);
  }

  protected void onBlocked(int tryCount, DateTime blockedSince) {
  }

  protected void onUnblocked(int tryCount, DateTime blockedSince) {
  }
}

