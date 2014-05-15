package joshng.util.exceptions;

/**
 * User: josh
 * Date: Oct 27, 2010
 * Time: 11:34:24 AM
 */
interface ITypedExceptionHandler<E extends Throwable> {
  public void handle(E e);
}
