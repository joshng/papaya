package joshng.util.concurrent;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * User: josh
 * Date: 9/3/13
 * Time: 12:09 PM
 */
public abstract class AbstractFunFuture<T> extends AbstractFuture<T> implements FunFuture<T> {
  @Override
  public ListenableFuture<T> delegate() {
    return this;
  }

  @Override
  public void addListener(Runnable listener, Executor exec) {
    super.addListener(AsyncContext.getCurrentContext().wrapRunnable(listener), exec);
  }
}
