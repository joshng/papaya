package joshng.util.retries;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import joshng.util.blocks.Consumer;
import joshng.util.exceptions.ExceptionPolicy;

/**
* User: josh
* Date: Oct 14, 2010
* Time: 5:33:16 PM
*/
public class BasicRetrySession extends AbstractRetrySession {
    private final long minDelay;
    private final long maxDelay;
    private final long maxTryCount;
    private final long retryStartTime;
    private final long retryEndTime;
    private final double backoffFactor;
    private final int delayIncrement = 0;
    private final ExceptionPolicy exceptionPolicy;
    private long nextDelay;
    private int tryCount = 0;
    private final Consumer<Exception> abortHandler;
    private final Predicate<? super Long> additionalAbortPolicy;

    public BasicRetrySession(BasicRetryPolicy policy) {
        this.minDelay = policy.getMinDelay();
        this.maxDelay = policy.getMaxDelay();
        this.maxTryCount = policy.getMaxTryCount();
        this.backoffFactor = policy.getBackoffFactor();
        this.exceptionPolicy = policy.getExceptionPolicy();
        this.abortHandler = policy.getAbortHandler();
        this.additionalAbortPolicy = policy.getAdditionalAbortPolicy();
        this.retryStartTime = System.currentTimeMillis();
        this.retryEndTime = retryStartTime + policy.getDuration();
        nextDelay = policy.getFirstDelay();
    }

    @Override
    protected RuntimeException onAborted(Exception exception) {
        if(abortHandler != null) abortHandler.handle(exception);
        return super.onAborted(exception);
    }

    public boolean canRetry(Exception e) {
        return exceptionPolicy.apply(e);
    }

    public boolean canRetryAfterDelay(long currentDelay) {
        return (!isMaxTryCountAttained() || shouldRetryOnMaxCountAttained())
                && canRetryWithDelay(currentDelay) && additionalAbortPolicy.apply(currentDelay);
    }

    protected boolean sleepBeforeRetry(Exception e) {
        return canRetry(e) && sleepBeforeRetry();
    }

    public boolean sleepBeforeRetry() {
        long delay = computeCurrentDelay();
        if (!canRetryAfterDelay(delay)) return false;

        beforeDelay(delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        willRetry();

        return true;
    }

    public void willRetry() {
        tryCount++;
        nextDelay = (long) Math.min(computeNextDelay(), maxDelay);
    }

    protected void beforeDelay(long delay) {
    }

    protected boolean shouldRetryOnDurationExceeded(long timeRemaining) {
        return false;
    }

    protected boolean shouldRetryOnMaxCountAttained() {
        return false;
    }

    protected double computeNextDelay() {
        return nextDelay * backoffFactor + delayIncrement;
    }

    protected boolean canRetryWithDelay(long currentDelay) {
        return !isDurationExceeded(currentDelay) || shouldRetryOnDurationExceeded(currentDelay);
    }

    protected boolean isDurationExceeded(long currentDelay) {
        return currentDelay < minDelay;
    }

    public long computeCurrentDelay() {
        return Math.min(retryEndTime - System.currentTimeMillis(), nextDelay);
    }

    private boolean isMaxTryCountAttained() {
        return tryCount + 1 >= maxTryCount;
    }
}
