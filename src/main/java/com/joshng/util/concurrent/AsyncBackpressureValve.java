package com.joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.joshng.util.collect.Nothing;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
* User: josh
* Date: 10/2/14
* Time: 11:34 AM
*/
public class AsyncBackpressureValve {
    private final int lowWaterMark;
    private final int highWaterMark;
    private final AtomicInteger bufferCounter = new AtomicInteger(0);
    private volatile FunFuture<Nothing> nextProductionFuture = FunFuture.NOTHING;
    private volatile Promise<Nothing> nextProductionPromise;

    public AsyncBackpressureValve(int lowWaterMark, int highWaterMark) {
      checkArgument(lowWaterMark <= highWaterMark && lowWaterMark >= 0, "lowWaterMark must be in [0, highWaterMark]");
      this.lowWaterMark = lowWaterMark;
      this.highWaterMark = highWaterMark;
    }

    public <T> Callable<FunFuture<T>> wrapJobStarter(Callable<? extends ListenableFuture<T>> jobStarter) {
      return () -> callWithBackpressure(jobStarter);
    }

    public <T> FunFuture<T> callWithBackpressure(Callable<? extends ListenableFuture<T>> jobStarter) {
      if (bufferCounter.incrementAndGet() >= highWaterMark && !isPaused()) {
        synchronized (bufferCounter) {
          if (bufferCounter.get() >= highWaterMark && !isPaused()) {
            nextProductionFuture = nextProductionPromise = Promise.newPromise();
          }
        }
      }

      return nextProductionFuture.thenAsync(jobStarter).uponCompletion(this::onJobCompleted);
    }

    private void onJobCompleted() {
      Promise<Nothing> promiseToComplete = null;
      if (bufferCounter.decrementAndGet() <= lowWaterMark && isPaused()) {
        synchronized (bufferCounter) {
          if (bufferCounter.get() <= lowWaterMark && isPaused()) {
            promiseToComplete = nextProductionPromise;
            nextProductionPromise = null;
          }
        }
      }

      if (promiseToComplete != null) {
        promiseToComplete.setSuccess(Nothing.NOTHING);
      }
    }

    private boolean isPaused() {
      return nextProductionPromise != null;
    }
  }
