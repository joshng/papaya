package joshng.util.concurrent;

import com.google.common.base.Function;
import joshng.util.SystemTime;
import joshng.util.WallClock;
import joshng.util.blocks.Source;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

/**
* User: josh
* Date: 11/14/12
* Time: 12:57 PM
*/
public abstract class RefreshingReference<T> extends Source<T> {

    private final WallClock clock;
    private final Object lock = new Object();
    @SuppressWarnings("unchecked")
    private volatile ExpiringContainer<T> currentContainer = ExpiringContainer.NULL;

    public static <T> RefreshingReference<T> from(long refreshPeriod, TimeUnit timeUnit, Function<? super ReadableInstant, ? extends T> newInstanceSupplier) {
        return from(refreshPeriod, timeUnit, WallClock.SYSTEM_CLOCK, newInstanceSupplier);
    }

    public static <T> RefreshingReference<T> from(long refreshPeriod, TimeUnit timeUnit, WallClock clock, Function<? super ReadableInstant, ? extends T> newInstanceSupplier) {
        return new SuppliedRefreshingReference<>(refreshPeriod, timeUnit, clock, newInstanceSupplier);
    }

    public RefreshingReference(WallClock clock) {
        this.clock = clock;
    }

    protected RefreshingReference() {
        this(WallClock.SYSTEM_CLOCK);
    }

    public T get() {
        ExpiringContainer<T> container = currentContainer;
        if (container.isExpired(currentTime())) {
            T expiredInstance = null;
            T newInstance = null;
            synchronized (lock) {
                container = currentContainer;
                ReadableInstant now = currentTime();
                if (container.isExpired(now)) {
                    ReadableInstant refreshTime = computeNextRefreshTime(now);
                    checkState(refreshTime.isAfter(now), "Computed refresh time was already expired", refreshTime, now);
                    expiredInstance = container.getValue();
                    newInstance = getNewInstance(refreshTime);
                    currentContainer = container = new ExpiringContainer<T>(newInstance, refreshTime);
                }
            }
            if (expiredInstance != null) {
                onInstanceExpired(expiredInstance, newInstance);
            }
        }
        return container.getValue();
    }

    protected void onInstanceExpired(T expiredInstance, T newInstance) {
    }

    public ReadableInstant getNextRefreshTime() {
        return currentContainer.expireTime;
    }

    protected abstract ReadableInstant computeNextRefreshTime(ReadableInstant now);

    protected abstract T getNewInstance(ReadableInstant expireTime);

    protected ReadableInstant currentTime() {
        return clock.now();
    }

    private static class SuppliedRefreshingReference<T> extends RefreshingReference<T> {
        private final Function<? super ReadableInstant, ? extends T> newInstanceSupplier;
        private final long periodMillis;

        public SuppliedRefreshingReference(long refreshPeriod, TimeUnit timeUnit, WallClock ticker, Function<? super ReadableInstant, ? extends T> newInstanceSupplier) {
            super(ticker);
            this.newInstanceSupplier = newInstanceSupplier;
            this.periodMillis = timeUnit.toMillis(refreshPeriod);
        }

        protected T getNewInstance(ReadableInstant expireTime) {
            return newInstanceSupplier.apply(expireTime);
        }

        protected ReadableInstant computeNextRefreshTime(ReadableInstant now) {
            return computeNextIntervalTime(now, periodMillis);
        }
    }

    public static ReadableInstant computeNextIntervalTime(ReadableInstant now, long periodMillis) {
        return new DateTime(((now.getMillis() / periodMillis) + 1) * periodMillis);
    }

    private static class ExpiringContainer<T> {
        private static ExpiringContainer NULL = new ExpiringContainer<Object>(null, SystemTime.ZERO) {};
        private final T value;
        private final ReadableInstant expireTime;

        private ExpiringContainer(T value, ReadableInstant expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired(ReadableInstant now) {
            return !now.isBefore(expireTime);
        }

        public T getValue() {
            return value;
        }
    }
}
