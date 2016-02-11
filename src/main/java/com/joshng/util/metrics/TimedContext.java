package com.joshng.util.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.joshng.util.context.TransientContext;

/**
 * User: josh
 * Date: 2/19/13
 * Time: 6:07 PM
 */

/**
 * A StackContext for yammer timers
 */
public class TimedContext implements TransientContext {
  private final Timer timer;

  public static TimedContext newTimedContext(MetricRegistry registry, String name) {
    return new TimedContext(registry.timer(name));
  }

  public TimedContext(Timer timer) {
    this.timer = timer;
  }

  @Override
  public State enter() {
    final Timer.Context timerContext = timer.time();
    return timerContext::stop;
  }
}
