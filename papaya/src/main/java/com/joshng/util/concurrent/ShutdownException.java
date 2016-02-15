package com.joshng.util.concurrent;

/**
 * User: josh
 * Date: Sep 8, 2010
 * Time: 11:49:53 PM
 */
public class ShutdownException extends RuntimeException {
  public ShutdownException() {
  }

  public ShutdownException(String message) {
    super(message);
  }

  public static void throwIf(boolean isShutdown) {
    throwIf(isShutdown, "Was shut down");
  }

  public static void throwIf(boolean isShutdown, String message) {
    if (isShutdown) throw new ShutdownException(message);
  }
}
