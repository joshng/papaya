package joshng.util.exceptions;

import org.slf4j.Logger;

/**
 * User: josh
 * Date: Oct 22, 2010
 * Time: 10:29:09 AM
 */
public class WarnAndSuppressExceptionHandler<E extends Throwable> extends ExceptionHandler<E> {
    private final String message;
    private final Logger logger;

    public WarnAndSuppressExceptionHandler(Class<E> errorClass, String message, Logger logger) {
        super(errorClass);
        this.message = message;
        this.logger = logger;
    }

    public void handle(E e) {
        logger.warn(message, e);
    }
}
