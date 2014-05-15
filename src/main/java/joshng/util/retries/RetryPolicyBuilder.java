package joshng.util.retries;

import joshng.util.builders.BaseBuilder;
import joshng.util.exceptions.ExceptionPolicy;
import joshng.util.exceptions.IExceptionHandler;

import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 5/30/13
 * Time: 4:05 PM
 */
public class RetryPolicyBuilder extends BaseBuilder<RetryPolicy, RetryPolicyBuilder> {
  private static final RetryPolicyBuilder builder = new RetryPolicyBuilder();
  public static final DurationProperty firstDelay = builder.durationWithDefault(2, TimeUnit.MILLISECONDS);
  public static final DurationProperty minDelay = builder.durationWithDefault(0, TimeUnit.MILLISECONDS);
  public static final DurationProperty maxDelay = builder.durationWithDefault(1, TimeUnit.SECONDS);
  public static final PositiveIntProperty maxTryCount = builder.new PositiveIntProperty().withDefault(Integer.MAX_VALUE);
  public static final DurationProperty maxDuration = builder.durationWithDefault(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
  public static final Property<Double> backoffFactor = builder.propertyWithDefault(2.0d);
  public static final ListProperty<IExceptionHandler> exceptionHandlers = builder.listProperty();

  public static RetryPolicyBuilder newBuilder() {
    return builder;
  }

  public BasicRetryPolicy build() {
    ExceptionPolicy policy = new ExceptionPolicy();
    for (IExceptionHandler handler : get(exceptionHandlers)) {
      policy.add(handler);
    }
    return new BasicRetryPolicy()
            .firstDelayMillis(get(firstDelay))
            .minDelayMillis(get(minDelay))
            .maxDelayMillis(get(maxDelay))
            .failAfterTryCount(get(maxTryCount))
            .failAfterMillis(get(maxDuration))
            .backoff(get(backoffFactor))
            .handling(policy);
  }
}
