package joshng.util.concurrent;

import com.google.common.base.Function;
import joshng.util.blocks.Source;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

/**
 * User: josh
 * Date: 11/14/12
 * Time: 12:57 PM
 */
public abstract class RefreshingReference<T> implements Source<T> {

  private final Clock clock;
  private final Object lock = new Object();
  @SuppressWarnings("unchecked")
  private volatile ExpiringContainer<T> currentContainer = ExpiringContainer.NULL;

  public static <T> RefreshingReference<T> from(long refreshPeriod, TimeUnit timeUnit, Function<? super Instant, ? extends T> newInstanceSupplier) {
    return from(refreshPeriod, timeUnit, Clock.systemUTC(), newInstanceSupplier);
  }

  public static <T> RefreshingReference<T> from(long refreshPeriod, TimeUnit timeUnit, Clock clock, Function<? super Instant, ? extends T> newInstanceSupplier) {
    return new SuppliedRefreshingReference<>(refreshPeriod, timeUnit, clock, newInstanceSupplier);
  }

  public RefreshingReference(Clock clock) {
    this.clock = clock;
  }

  protected RefreshingReference() {
    this(Clock.systemUTC());
  }

  public T get() {
    ExpiringContainer<T> container = currentContainer;
    if (container.isExpired(currentTime())) {
      T expiredInstance = null;
      T newInstance = null;
      synchronized (lock) {
        container = currentContainer;
        Instant now = currentTime();
        if (container.isExpired(now)) {
          Instant refreshTime = computeNextRefreshTime(now);
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

  public Instant getNextRefreshTime() {
    return currentContainer.expireTime;
  }

  protected abstract Instant computeNextRefreshTime(Instant now);

  protected abstract T getNewInstance(Instant expireTime);

  protected Instant currentTime() {
    return clock.instant();
  }

  private static class SuppliedRefreshingReference<T> extends RefreshingReference<T> {
    private final Function<? super Instant, ? extends T> newInstanceSupplier;
    private final long periodMillis;

    public SuppliedRefreshingReference(long refreshPeriod, TimeUnit timeUnit, Clock ticker, Function<? super Instant, ? extends T> newInstanceSupplier) {
      super(ticker);
      this.newInstanceSupplier = newInstanceSupplier;
      this.periodMillis = timeUnit.toMillis(refreshPeriod);
    }

    protected T getNewInstance(Instant expireTime) {
      return newInstanceSupplier.apply(expireTime);
    }

    protected Instant computeNextRefreshTime(Instant now) {
      return computeNextIntervalTime(now, periodMillis);
    }
  }

  public static Instant computeNextIntervalTime(Instant now, long periodMillis) {
    return Instant.ofEpochMilli(((now.toEpochMilli() / periodMillis) + 1) * periodMillis);
  }

  private static class ExpiringContainer<T> {
    private static ExpiringContainer NULL = new ExpiringContainer<Object>(null, Instant.EPOCH) {
    };
    private final T value;
    private final Instant expireTime;

    private ExpiringContainer(T value, Instant expireTime) {
      this.value = value;
      this.expireTime = expireTime;
    }

    boolean isExpired(Instant now) {
      return !now.isBefore(expireTime);
    }

    public T getValue() {
      return value;
    }
  }
}
