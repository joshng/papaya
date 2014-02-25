package joshng.util.retries;

public class RetryAbortedException extends RuntimeException {
    public RetryAbortedException(Throwable cause) {
        super(cause);
    }
}


