package joshng.util.concurrent;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

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
}
