package joshng.util;

import org.slf4j.Logger;

/**
 * User: josh
 * Date: 5/17/13
 * Time: 12:26 AM
 */
public enum LogLevel {
    Trace { public boolean isEnabled(Logger logger) { return logger.isTraceEnabled(); }
        public void stacktrace(Logger logger, Throwable e, String message) { logger.trace(message, e); }
        public void log(Logger logger, String message) { logger.trace(message); } },
    Debug { public boolean isEnabled(Logger logger) { return logger.isDebugEnabled(); }
        public void stacktrace(Logger logger, Throwable e, String message) { logger.debug(message, e); }
        public void log(Logger logger, String message) { logger.debug(message); } },
    Info { public boolean isEnabled(Logger logger) { return logger.isInfoEnabled(); }
        public void stacktrace(Logger logger, Throwable e, String message) { logger.info(message, e); }
        public void log(Logger logger, String message) { logger.info(message); } },
    Warn { public boolean isEnabled(Logger logger) { return logger.isWarnEnabled(); }
        public void stacktrace(Logger logger, Throwable e, String message) { logger.warn(message, e); }
        public void log(Logger logger, String message) { logger.warn(message); } },
    Error { public boolean isEnabled(Logger logger) { return logger.isErrorEnabled(); }
        public void stacktrace(Logger logger, Throwable e, String message) { logger.error(message, e); }
        public void log(Logger logger, String message) { logger.error(message); } };

    public abstract boolean isEnabled(Logger logger);
    public abstract void log(Logger logger, String message);
    public abstract void stacktrace(Logger logger, Throwable e, String message);

    public void log(Logger logger, String format, Object... args) {
        if (isEnabled(logger)) log(logger, StringUtils.format(format, args));
    }

    public void stacktrace(Logger logger, Throwable error, String format, Object... args) {
        if (isEnabled(logger)) stacktrace(logger, error, StringUtils.format(format, args));
    }
}
