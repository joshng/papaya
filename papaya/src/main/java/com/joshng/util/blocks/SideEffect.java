package com.joshng.util.blocks;

import com.joshng.util.collect.Nothing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: josh
 * Date: 5/23/13
 * Time: 10:59 AM
 */
/** A pimped Runnable/side-effect */
public interface SideEffect extends Source<Nothing>, Runnable, ThrowingRunnable {
  static final Logger LOG = LoggerFactory.getLogger(SideEffect.class);

  public static SideEffect sideEffect(SideEffect sideEffect) {
    return sideEffect;
  }

  public static SideEffect extendRunnable(final Runnable runnable) {
    if (runnable instanceof SideEffect) return (SideEffect) runnable;
    return runnable::run;
  }

  default void runIgnoringExceptions() {
    runIgnoringExceptions(this);
  }

  public static void runIgnoringExceptions(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      handleUncaughtException(e);
    }
  }

  static void handleUncaughtException(Throwable e) {
    Thread currentThread = Thread.currentThread();
    Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
    if (exceptionHandler != null) {
      exceptionHandler.uncaughtException(currentThread, e);
    } else {
      LOG.warn("Uncaught exception in thread {}:", currentThread, e);
    }
  }

  default <T> Tapper<T> asTapper() {
    return value -> run();
  }

  @Override
  default Nothing get() {
    run();
    return Nothing.NOTHING;
  }

  @Override default Nothing call() {
    return get();
  }
}
