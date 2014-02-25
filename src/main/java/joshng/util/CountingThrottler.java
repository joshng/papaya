package joshng.util;

import com.google.common.base.Function;
import com.google.common.collect.ObjectArrays;
import com.yammer.metrics.core.Meter;
import joshng.util.blocks.Source;
import joshng.util.collect.Maybe;
import org.slf4j.Logger;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 12/28/12
 * Time: 4:21 PM
 */
/**
 * Utility class to encapsulate a common pattern that involves counting occurrences of an event,
 * with throttled logging to occasionally expose how many times the event has happened.
 *
 * Usage:
 *
 * <pre>{@code
 * // register a Meter to count all occurrences
 * Meter meter = Metrics.newMeter(MyClass.class, "someEvent", "whatever", TimeUnit.MINUTES);
 *
 * // configure a throttler to control our logging-spam: this will permit logging at most once every 5 seconds
 * CountingThrottler errorLogThrottler = new CountingThrottler(meter, new Throttler(5, TimeUnit.SECONDS);
 *
 * try {
 *     // ... do work ...
 * } catch (Exception e) {
 *     for (Long errorCount : errorLogThrottler.tryAcquire()) {
 *         // we'll do this at most once every 5 seconds
 *         LOG.warn("Oops, " + errorCount + " errors since startup", e);
 *     }
 * }
 * }</pre>
 */
public class CountingThrottler extends Source<Maybe<Long>> {
    private final Throttler throttler;
    private final Meter meter;

    public CountingThrottler(Meter meter, Throttler throttler) {
        this.meter = meter;
        this.throttler = throttler;
    }

    /**
     * @return If the underlying {@link Throttler} permits, returns the total count of calls
     * to tryAcquire() since startup. Otherwise, Maybe.not().
     */
    public Maybe<Long> tryAcquire() {
        meter.mark();
        if (throttler.tryAcquire()) {
            return definitely(getCount());
        } else {
            return Maybe.not();
        }
    }

    /**
     * If {@link #tryAcquire} permits (ie, if the underlying {@link Throttler} permits), invokes the provided
     * function with the result from {@link #tryAcquire} (ie, total count of all calls since startup).
     * Otherwise, does NOT invoke the function, and returns Maybe.not().
     */
    public <T> Maybe<T> invokeThrottled(Function<? super Long, T> periodicBlock) {
        return tryAcquire().map(periodicBlock);
    }

    public long getCount() {
        return meter.count();
    }

    public void logCountWithThrottling(Logger logger, LogLevel level, String format) {
        meter.mark();
        if (throttler.tryAcquire()) level.log(logger, format, getCount());
    }

    public void countAndLogWithThrottle(Logger logger, LogLevel level, String format, Object arg) {
        meter.mark();
        if (throttler.tryAcquire()) level.log(logger, format, arg, getCount());
    }

    public void countAndLogWithThrottle(Logger logger, LogLevel level, String format, Object... args) {
        meter.mark();
        if (throttler.tryAcquire() && level.isEnabled(logger)) level.log(logger, format, ObjectArrays.concat(args, getCount()));
    }

    public void countAndStacktraceWithThrottle(Logger logger, LogLevel level, Throwable t, String format) {
        meter.mark();
        if (throttler.tryAcquire()) level.stacktrace(logger, t, format, getCount());
    }

    public void countAndStacktraceWithThrottle(Logger logger, LogLevel level, Throwable t, String format, Object arg) {
        meter.mark();
        if (throttler.tryAcquire()) level.stacktrace(logger, t, format, arg, getCount());
    }

    public void countAndStacktraceWithThrottle(Logger logger, LogLevel level, Throwable t, String format, Object... args) {
        meter.mark();
        if (throttler.tryAcquire() && level.isEnabled(logger)) level.stacktrace(logger, t, format, ObjectArrays.concat(args, getCount()));
    }

    @Override
    public Maybe<Long> get() {
        return tryAcquire();
    }
}
