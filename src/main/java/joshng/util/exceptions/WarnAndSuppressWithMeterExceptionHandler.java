package joshng.util.exceptions;

import com.yammer.metrics.core.Meter;
import org.slf4j.Logger;

/**
 * User: daryoush
 * Date: 6/28/12
 * Time: 10:12 AM
 */
public class WarnAndSuppressWithMeterExceptionHandler<E extends Throwable> extends WarnAndSuppressExceptionHandler<E> {
  final Meter meter;

  public WarnAndSuppressWithMeterExceptionHandler(Class<E> throwableClass, String message, Logger logger, Meter meter) {

    super(throwableClass, message, logger);
    this.meter = meter;
  }

  public void handle(E e) {
    meter.mark();
    super.handle(e);
  }
}
