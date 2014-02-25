package joshng.util.exceptions;

/**
 * User: josh
 * Date: Oct 27, 2010
 * Time: 11:23:02 AM
 */
public interface IExceptionHandler {
    boolean didHandle(Throwable throwable);
}
