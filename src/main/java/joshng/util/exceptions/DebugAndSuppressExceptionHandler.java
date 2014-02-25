package joshng.util.exceptions;

import org.slf4j.Logger;

/**
 * User: josh
 * Date: Oct 22, 2010
 * Time: 10:29:09 AM
 */
public class DebugAndSuppressExceptionHandler<E extends Throwable> extends ExceptionHandler<E> {
    private final String message;
    private final Logger logger;

    public DebugAndSuppressExceptionHandler(Class<E> errorClass, String message, Logger logger) {
        super(errorClass);
        this.message = message;
        this.logger = logger;
    }

    public void handle(E e) {
        logger.debug(message, e);
    }
}
