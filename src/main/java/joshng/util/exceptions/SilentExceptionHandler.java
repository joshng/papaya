package joshng.util.exceptions;

/**
 * User: josh
 * Date: Nov 2, 2010
 * Time: 11:32:36 PM
 */
public class SilentExceptionHandler<E extends Throwable> extends ExceptionHandler<E> {

  public SilentExceptionHandler(Class<E> errorClass) {
    super(errorClass);
  }

  public void handle(E e) {
  }
}
