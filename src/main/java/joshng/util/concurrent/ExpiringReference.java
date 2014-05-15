package joshng.util.concurrent;

import com.google.common.base.Supplier;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 10/30/12
 * Time: 4:33 PM
 */
public abstract class ExpiringReference<T> extends LazyReference<T> {
  private final long expiryDurationNanos;
  private volatile long expiryDeadline = 0;

  public static <T> ExpiringReference<T> fromSupplier(long expiryDuration, TimeUnit expiryDurationUnit, Supplier<T> valueSupplier) {
    return new SuppliedExpiringReference<>(valueSupplier, expiryDuration, expiryDurationUnit);
  }

  protected ExpiringReference(long expiryDuration, TimeUnit expiryDurationUnit) {
    this.expiryDurationNanos = expiryDurationUnit.toNanos(expiryDuration);
  }

  protected abstract T load(@Nullable T cachedValue);

  @Override
  protected final T supplyValue() {
    return load(this.value);
  }

  private void touch(long now) {
    expiryDeadline = now + expiryDurationNanos;
  }

  @Override
  protected boolean needsLoad(T result) {
    return super.needsLoad(result) || System.nanoTime() - expiryDeadline >= 0;
  }

  @Override
  public void set(T value) {
    long now = System.nanoTime();
    synchronized (this) {
      super.set(value);
      touch(now);
    }
  }

  private static class SuppliedExpiringReference<T> extends ExpiringReference<T> {
    private final Supplier<T> supplier;

    protected SuppliedExpiringReference(Supplier<T> supplier, long expiryDuration, TimeUnit expiryDurationUnit) {
      super(expiryDuration, expiryDurationUnit);
      this.supplier = supplier;
    }

    @Override
    protected T load(@Nullable T cachedValue) {
      return supplier.get();
    }
  }
}
