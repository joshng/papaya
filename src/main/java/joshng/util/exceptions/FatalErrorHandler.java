package joshng.util.exceptions;

import com.google.common.base.Throwables;
import joshng.util.Localhost;
import joshng.util.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: josh
 * Date: Sep 30, 2010
 * Time: 7:15:59 PM
 */
public class FatalErrorHandler {
  private static final Logger LOG = LoggerFactory.getLogger(FatalErrorHandler.class);
  private static final ExecutorService TERMINATION_THREAD = Executors.newSingleThreadExecutor(
          new NamedThreadFactory("fatal-error-shutdown"));
  private static final AtomicBoolean TERMINATED = new AtomicBoolean();

  private static volatile Runnable s_shutdownTrigger = () -> System.exit(2);

  private static final Runnable TERMINATION_TASK = () -> {
            if (TERMINATED.compareAndSet(false, true)) {
              //noinspection finally
              try {
                s_shutdownTrigger.run();
              } finally {
                System.exit(2);
              }
            }
          };

  public static void setShutdownTrigger(Runnable shutdownTrigger) {
    s_shutdownTrigger = shutdownTrigger;
  }

  public static Error terminateProcess(Throwable throwable) {
    return terminateProcess("", throwable);
  }

  public static Error terminateProcess(String message, Throwable throwable) {
    try {
      String fullMessage = "FATAL ERROR, terminating! " + message + "\nHost: " + Localhost.getDescription();
      Error error = new Error(fullMessage, throwable);
      LOG.error(fullMessage, error);
      throw error;
    } finally {
      TERMINATION_THREAD.execute(TERMINATION_TASK);
    }
  }

  public static Error terminateProcess(String reason) {
    throw terminateProcess(reason, null);
  }

  public static RuntimeException propagate(Throwable throwable) {
    return propagate(throwable, RuntimeException.class);
  }

  public static <E extends Exception> RuntimeException propagate(Throwable throwable, Class<E> tolerableToThrow) throws E {
    Exception exception = castOrDie(throwable);
    Throwables.propagateIfPossible(exception, tolerableToThrow);
    throw new RuntimeException(throwable);
  }

  public static <E extends Throwable, T extends Exception> E castOrPropagate(Throwable throwable, Class<E> typeToReturn, Class<T> typeToThrow) throws T {
    if (typeToReturn.isInstance(throwable)) return typeToReturn.cast(throwable);
    throw propagate(throwable, typeToThrow);
  }

  public static <E extends Throwable> E castOrPropagate(Throwable throwable, Class<E> typeToReturn) {
    return castOrPropagate(throwable, typeToReturn, RuntimeException.class);
  }

  public static Exception castOrDie(Throwable throwable) {
    if (throwable instanceof Exception && !(throwable instanceof ClassNotFoundException)) return (Exception) throwable;
    if (throwable instanceof StackOverflowError) return new StackOverflowException((StackOverflowError) throwable);
    throw terminateProcess(throwable);
  }

  public static IExceptionHandler EXCEPTION_FILTER = new IExceptionHandler() {
    @Override
    public boolean didHandle(Throwable throwable) {
      castOrDie(throwable);
      return false;
    }
  };
}
