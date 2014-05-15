package joshng.util.exceptions;

import com.google.common.base.Preconditions;
import com.yammer.metrics.core.Meter;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * User: josh
 * Date: Sep 11, 2010
 * Time: 10:23:23 AM
 */
public abstract class ExceptionHandler<E extends Throwable> extends AbstractExceptionHandler<E> {
  private final Class<E> errorClass;

  public static <E extends Throwable> WarnAndSuppressExceptionHandler<E> warnAndSuppress(Class<E> throwableClass, String message, Logger logger) {
    return new WarnAndSuppressExceptionHandler<E>(throwableClass, message, logger);
  }

  public static <E extends Throwable> DebugAndSuppressExceptionHandler<E> debugAndSuppress(Class<E> throwableClass, String message, Logger logger) {
    return new DebugAndSuppressExceptionHandler<E>(throwableClass, message, logger);
  }

  public static <E extends Throwable> SilentExceptionHandler<E> silent(Class<E> throwableClass) {
    return new SilentExceptionHandler<E>(throwableClass);
  }

  public static <E extends Throwable> WarnAndSuppressWithMeterExceptionHandler<E> warnAndSuppressWithMeter(Class<E> throwableClass, String message, Logger logger, Meter meter) {
    return new WarnAndSuppressWithMeterExceptionHandler<E>(throwableClass, message, logger, meter);
  }

  public ExceptionHandler(Class<E> errorClass) {
    this.errorClass = Preconditions.checkNotNull(errorClass);
  }

  @Override
  @Nullable
  protected E extractHandledExceptionOrNull(Throwable throwable) {
    return Exceptions.extractCauseOrNull(throwable, errorClass);
  }
}

