package com.joshng.util.exceptions;

/**
 * User: josh
 * Date: 10/22/14
 * Time: 7:00 PM
 */
public class UncheckedInterruptedException extends RuntimeException {
  private UncheckedInterruptedException(InterruptedException cause) {
    super(cause);
  }

  public static UncheckedInterruptedException propagate(InterruptedException cause) {
    Thread.currentThread().interrupt();
    throw new UncheckedInterruptedException(cause);
  }
}
