package joshng.util.exceptions;


import javax.annotation.Nullable;

/**
 * User: josh
 * Date: Oct 27, 2010
 * Time: 11:26:10 AM
 */
public abstract class AbstractExceptionHandler<E extends Throwable> implements IExceptionHandler, ITypedExceptionHandler<E> {
  public boolean didHandle(Throwable throwable) {
    E cause = extractHandledExceptionOrNull(throwable);
    boolean found = cause != null && isHandlable(cause);
    if (found) handle(cause);
    return found;
  }

  protected boolean isHandlable(E cause) {
    return true;
  }

  @Nullable
  protected abstract E extractHandledExceptionOrNull(Throwable throwable);
}

