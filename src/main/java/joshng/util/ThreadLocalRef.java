package joshng.util;

import joshng.util.collect.MutableReference;
import joshng.util.collect.Ref;
import joshng.util.context.StackContext;

import javax.annotation.Nullable;

/**
 * User: josh
 * Date: 2/4/12
 * Time: 9:36 AM
 */
public class ThreadLocalRef<T> extends ThreadLocal<T> implements MutableReference<T> {
    private final T initialValue;

    public ThreadLocalRef(T initialValue) {
        this.initialValue = initialValue;
    }

    public ThreadLocalRef() {
        this(null);
    }

    public StackContext contextWithValue(final T value) {
        return Ref.contextWithValue(value, this);
    }

    public boolean compareAndSet(@Nullable T expect, @Nullable T update) {
        return Ref.nonAtomicCompareAndSet(this, expect, update);
    }

    @Override public T getAndSet(T value) {
        return Ref.nonAtomicGetAndSet(this, value);
    }

    @Override
    protected T initialValue() {
        return initialValue;
    }
}
