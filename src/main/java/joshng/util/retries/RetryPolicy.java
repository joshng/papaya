package joshng.util.retries;

import joshng.util.exceptions.ExceptionPolicy;

/**
 * User: josh
 * Date: Oct 15, 2010
 * Time: 7:08:27 PM
 */
public interface RetryPolicy {
  RetryPolicy NEVER_RETRY = new RetryPolicy() {
    public RetrySession newSession() {
      return RetrySession.NO_RETRY;
    }

    public ExceptionPolicy getExceptionPolicy() {
      throw new UnsupportedOperationException("RetryPolicy.NEVER_RETRY has no exception policy");
    }
  };

  RetrySession newSession();

  ExceptionPolicy getExceptionPolicy();
}
