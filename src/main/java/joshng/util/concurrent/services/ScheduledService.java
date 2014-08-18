package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 8/15/14
 * Time: 11:24 PM
 */
public abstract class ScheduledService extends BaseService {
  private final AbstractScheduledService delegate = new InnerScheduledService();
  @Override protected Service delegate() { return delegate; }

  /**
   * Run one iteration of the scheduled task. If any invocation of this method throws an exception,
   * the service will transition to the {@link Service.State#FAILED} state and this method will no
   * longer be called.
   */
  protected abstract void runOneIteration() throws Exception;

  /**
   * Start the service.
   *
   * <p>By default this method does nothing.
   */
  protected void startUp() throws Exception {}

  /**
   * Stop the service. This is guaranteed not to run concurrently with {@link #runOneIteration}.
   *
   * <p>By default this method does nothing.
   */
  protected void shutDown() throws Exception {}

  /**
   * Returns the {@link Schedule} object used to configure this service.  This method will only be
   * called once.
   */
  protected abstract Schedule schedule(Scheduler scheduler);

  public enum Scheduler {
    INSTANCE;

    public Schedule scheduleFixedRate(long initialDelay, long period, TimeUnit timeUnit) {
      return new Schedule(AbstractScheduledService.Scheduler.newFixedRateSchedule(initialDelay, period, timeUnit));
    }

    public Schedule scheduleFixedDelay(long initialDelay, long delay, TimeUnit timeUnit) {
      return new Schedule(AbstractScheduledService.Scheduler.newFixedDelaySchedule(initialDelay, delay, timeUnit));
    }
  }

  // just a cosmetic wrapper
  protected static final class Schedule {
    private final AbstractScheduledService.Scheduler scheduler;

    private Schedule(AbstractScheduledService.Scheduler scheduler) {this.scheduler = scheduler;}
  }

  private class InnerScheduledService extends AbstractScheduledService {
    @Override protected void runOneIteration() throws Exception {
      ScheduledService.this.runOneIteration();
    }

    @Override protected Scheduler scheduler() {
      return schedule(ScheduledService.Scheduler.INSTANCE).scheduler;
    }

    @Override protected void startUp() throws Exception {
      ScheduledService.this.startUp();
    }

    @Override protected void shutDown() throws Exception {
      ScheduledService.this.shutDown();
    }
  }
}
