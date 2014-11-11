package joshng.util.exceptions;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;

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
