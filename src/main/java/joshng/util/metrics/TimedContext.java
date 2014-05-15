package joshng.util.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import joshng.util.context.StackContext;

import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 2/19/13
 * Time: 6:07 PM
 */

/**
 * A StackContext for yammer timers
 */
public class TimedContext extends StackContext {
  private final Timer timer;

  public static TimedContext newTimedContext(Class<?> klass, String name, TimeUnit durationUnit, TimeUnit rateUnit) {
    return new TimedContext(Metrics.newTimer(klass, name, durationUnit, rateUnit));
  }

  public static TimedContext newTimedContext(Class<?> klass, String name, String scope, TimeUnit durationUnit, TimeUnit rateUnit) {
    return new TimedContext(Metrics.newTimer(klass, name, scope, durationUnit, rateUnit));
  }

  public static TimedContext newTimedContext(Class<?> klass, String name) {
    return new TimedContext(Metrics.newTimer(klass, name));
  }

  public TimedContext(Timer timer) {
    this.timer = timer;
  }


  @Override
  public State enter() {
    return new State() {
      final TimerContext timerContext = timer.time();

      @Override
      public void exit() {
        timerContext.stop();
      }
    };
  }
}
