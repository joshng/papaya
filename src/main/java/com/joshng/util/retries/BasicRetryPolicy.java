package com.joshng.util.retries;

import com.google.common.util.concurrent.AsyncFunction;
import com.joshng.util.blocks.Pred;
import com.joshng.util.blocks.Source;
import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.exceptions.ExceptionPolicy;
import com.joshng.util.exceptions.IExceptionHandler;
import com.joshng.util.blocks.F;
import com.joshng.util.concurrent.AsyncF;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.joshng.util.concurrent.AsyncF.asyncF;

/**
 * User: josh
 * Date: Oct 14, 2010
 * Time: 4:53:16 PM
 */
public class BasicRetryPolicy implements RetryPolicy {
  private long firstDelay = 2;
  private long minDelay = 0;
  private long maxDelay = 1000;
  private long maxTryCount = Integer.MAX_VALUE;
  private long duration = Long.MAX_VALUE;
  private ExceptionPolicy exceptionPolicy = new ExceptionPolicy();
  private double backoffFactor = 2.0;
  private Predicate<? super Long> additionalAbortPolicy = Pred.alwaysTrue();
  private Consumer<Exception> abortHandler;
  private final F retryFunction = new F<Callable<?>, Object>() {
    @Override
    public Object apply(Callable<?> input) {
      return newSession().retry(input);
    }
  };

  public BasicRetryPolicy firstDelayMillis(long delayMillis) {
    firstDelay = delayMillis;
    return this;
  }

  public BasicRetryPolicy firstDelay(int delay, TimeUnit timeUnit) {
    return firstDelayMillis(timeUnit.toMillis(delay));
  }

  public BasicRetryPolicy minDelayMillis(long delayMillis) {
    minDelay = delayMillis;
    return this;
  }

  public BasicRetryPolicy minDelay(int delay, TimeUnit timeUnit) {
    return minDelayMillis(timeUnit.toMillis(delay));
  }

  public BasicRetryPolicy maxDelayMillis(long delayMillis) {
    maxDelay = delayMillis;
    return this;
  }

  public BasicRetryPolicy maxDelay(int delay, TimeUnit timeUnit) {
    return maxDelayMillis(timeUnit.toMillis(delay));
  }

  public BasicRetryPolicy failAfterTryCount(int count) {
    maxTryCount = count;
    return this;
  }

  public BasicRetryPolicy failAfterMillis(long delayMillis) {
    duration = delayMillis;
    return this;
  }

  public BasicRetryPolicy failAfter(int delay, TimeUnit timeUnit) {
    return failAfterMillis(timeUnit.toMillis(delay));
  }

  public BasicRetryPolicy backoff(double factor) {
    backoffFactor = factor;
    return this;
  }

  public BasicRetryPolicy handling(IExceptionHandler exceptionHandler) {
    exceptionPolicy.add(exceptionHandler);
    return this;
  }

  public BasicRetryPolicy handling(ExceptionPolicy exceptionPolicy) {
    this.exceptionPolicy = exceptionPolicy;
    return this;
  }

  public BasicRetryPolicy withAdditionalAbortPolicy(Predicate<? super Long> additionalAbortPolicy) {
    this.additionalAbortPolicy = additionalAbortPolicy;
    return this;
  }

  public BasicRetryPolicy withAbortHandler(Consumer<Exception> abortHandler) {
    this.abortHandler = abortHandler;
    return this;
  }


  @SuppressWarnings("unchecked")
  public <T> F<Callable<? extends T>, T> retry() {
    return retryFunction;
  }

  public Runnable wrapRunnable(final Runnable runnable) {
    return new Runnable() {
      public void run() {
        newSession().retry(runnable);
      }
    };
  }

  public <I, O> AsyncF<I, O> wrapAsync(final ScheduledExecutorService scheduler, final AsyncFunction<I, O> async) {
    final AsyncF<I, O> asyncF = asyncF(async);
    return input -> newSession().retryAsync(scheduler, asyncF.bind(input));
  }

  public <O> Source<FunFuture<O>> wrapAsyncCallable(final ScheduledExecutorService scheduler, final Callable<? extends FunFuture<O>> async) {
    return new Source<FunFuture<O>>() {
      @Override
      public FunFuture<O> get() {
        return newSession().retryAsync(scheduler, async);
      }
    };
  }

  public <I, O> F<I, O> wrapFunction(Function<I, O> function) {
    return F.<I, O>extendFunction(function).binder().andThen(this.<O>retry());
  }

  public <T> Source<T> wrapCallable(final Callable<T> callable) {
    return this.<T>retry().bind(callable);
  }

  public RetrySession newSession() {
    return new BasicRetrySession(this);
  }

  public ExceptionPolicy getExceptionPolicy() {
    return exceptionPolicy;
  }

  public long getFirstDelay() {
    return firstDelay;
  }

  public long getMinDelay() {
    return minDelay;
  }

  public long getMaxDelay() {
    return maxDelay;
  }

  public long getMaxTryCount() {
    return maxTryCount;
  }

  public long getDuration() {
    return duration;
  }

  public double getBackoffFactor() {
    return backoffFactor;
  }

  public Consumer<Exception> getAbortHandler() {
    return abortHandler;
  }

  public Predicate<? super Long> getAdditionalAbortPolicy() {
    return additionalAbortPolicy;
  }
}

