package com.joshng.util.concurrent;

import java.util.concurrent.Future;

/**
 * Created by: josh 10/22/13 2:04 PM
 */
public interface Cancellable {
  static Cancellable extendFuture(final Future future) {
    if (future instanceof Cancellable) return (Cancellable) future;
    return future::cancel;
  }

  boolean cancel(boolean mayInterruptIfRunning);
}
