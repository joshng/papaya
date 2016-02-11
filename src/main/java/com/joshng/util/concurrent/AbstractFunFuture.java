package com.joshng.util.concurrent;

import com.google.common.util.concurrent.AbstractFuture;

import java.util.concurrent.Executor;

/**
 * User: josh
 * Date: 9/3/13
 * Time: 12:09 PM
 */
public abstract class AbstractFunFuture<T> extends AbstractFuture<T> implements FunFuture<T> {
  @Override
  public void addListener(Runnable listener, Executor exec) {
    super.addListener(AsyncContext.snapshot().wrapRunnable(listener), exec);
  }
}
