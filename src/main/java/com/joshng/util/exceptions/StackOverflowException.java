package com.joshng.util.exceptions;

/**
 * User: josh
 * Date: 11/15/12
 * Time: 1:42 PM
 */

/**
 * JVM stack overflows result in StackOverflowError. However, unlike most Errors, this merely indicates a logic bug;
 * the VM is likely still healthy, with only the Erroneous thread being affected.
 * <p>
 * <p>To conform to the convention that Errors indicate "systemic" problems, we wrap StackOverflowErrors in
 * StackOverflowExceptions, to protect the calling stack from the Error-type.
 */
public class StackOverflowException extends RuntimeException {
  public StackOverflowException(StackOverflowError cause) {
    super(cause);
  }
}
