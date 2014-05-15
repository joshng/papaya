package joshng.util.exceptions;

import joshng.util.StringUtils;
import org.slf4j.Logger;

/**
 * User: josh
 * Date: Sep 14, 2011
 * Time: 5:09:51 PM
 */
public final class StackTraceLogger extends RuntimeException {
  public static void log(Logger logger, String format, Object... formatArgs) {
    String message = StringUtils.format(format, formatArgs);
    logger.warn(message, new StackTraceLogger(message));
  }

  private StackTraceLogger(String message) {
    super(message);
    fillInStackTrace();
  }
}
