package joshng.util.concurrent;

import com.google.common.base.Supplier;
import joshng.util.collect.Ref;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Utility to lazily load an object reference.
 * It uses Double-Checked Locking under the covers
 * and thus can safely be accessed from multiple threads
 * concurrently.
 *
 * @author Robbie Vanbrabant
 * (heavily modified by:)
 * @author josh gruenberg
 */
@ThreadSafe
public abstract class LazyReference<T> extends Ref<T> {

    public LazyReference() {
        super(null);
    }

    private static class SuppliedLazyReference<T> extends LazyReference<T> {
        private final Supplier<T> instanceSupplier;

        private SuppliedLazyReference(Supplier<T> instanceSupplier) {
            this.instanceSupplier = instanceSupplier;
        }

        protected T supplyValue() {
            return instanceSupplier.get();
        }
    }

    protected abstract T supplyValue();

    public T get() {
        // Double-Checked Locking as seen in
        // Effective Java, 2nd edition, page 283.
        T result = value;
        if (needsLoad(result)) {
            synchronized (this) {
                result = value;
                if (needsLoad(result)) {
                    set(result = supplyValue());
                }
            }
        }
        return result;
    }

    public synchronized void set(T value) {
        super.set(value);
    }

    public boolean isSet() {
        return value != null;
    }

    protected boolean needsLoad(T result) {
        return result == null;
    }


    /**
     * Create a lazy reference with a Supplier of the
     * eventual instance.
     * @param instanceSupplier a {@link com.google.common.base.Supplier}
     *                         that gives out the eventual instance,
     *                         usually an expensive operation
     * @return an uninitialized reference to T
     */
    public static <T> LazyReference<T> from(Supplier<T> instanceSupplier) {
        return new SuppliedLazyReference<T>(instanceSupplier);
    }

    public synchronized void remove() {
        value = null;
    }

    @Override
    public synchronized boolean compareAndSet(T expect, T update) {
        return Ref.nonAtomicCompareAndSet(this, expect, update);
    }

    @Override
    public synchronized T getAndSet(T value) {
        return Ref.nonAtomicGetAndSet(this, value);
    }
}
