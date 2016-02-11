package com.joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: josh
 * Date: 8/7/15
 * Time: 2:24 PM
 */
public class FutureChain<T> {
  private static final AtomicReferenceFieldUpdater<FutureChain, FunFuture> TAIL_UPDATER = AtomicReferenceFieldUpdater.newUpdater(FutureChain.class, FunFuture.class, "tail");
  private volatile FunFuture<T> tail;

  public FutureChain(FunFuture<T> initialValue) {
    this.tail = checkNotNull(initialValue, "initialValue");
  }

  public FutureChain(T initialValue) {
    this(FunFuture.immediateFuture(initialValue));
  }

  public FunFuture<T> getTail() {
    return tail;
  }

  public <R extends T> FunFuture<R> append(Callable<? extends ListenableFuture<R>> job) {
    return transform(prev -> job.call());
  }

  public <R extends T> FunFuture<R> transform(AsyncFunction<? super T, R> job) {
    return Promise.newPromise(promise -> {
              @SuppressWarnings("unchecked") FunFuture<T> prevTail = TAIL_UPDATER.getAndSet(this, promise);
              promise.completeWith(prevTail.flatMap(job));
            });
  }
}

