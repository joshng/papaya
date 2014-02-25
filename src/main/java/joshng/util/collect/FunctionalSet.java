package joshng.util.collect;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import joshng.util.blocks.Consumer;

import javax.annotation.Nullable;
import java.util.Set;

import static joshng.util.Reflect.blindCast;

/**
 * User: josh
 * Date: Aug 27, 2011
 * Time: 4:26:26 PM
 */
public class FunctionalSet<T> extends FunctionalCollection<T> implements FunSet<T> {
    private final ImmutableSet<T> delegate;

    public FunctionalSet(ImmutableSet<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public ImmutableSet<T> delegate() {
        return delegate;
    }

    public static <T> FunSet<T> copyOf(Iterable<T> delegate) {
        if (delegate instanceof FunSet) return (FunSet<T>) delegate;
        return extend(ImmutableSet.copyOf(delegate));
    }

    public static <T> FunSet<T> extend(ImmutableSet<T> delegate) {
        if (delegate.isEmpty()) return emptySet();
        return new FunctionalSet<>(delegate);
    }

    public static <T> FunSet<T> of(T singleton) {
        return new FunctionalSet<>(ImmutableSet.of(singleton));
    }

    @SafeVarargs
    public static <T> FunSet<T> of(T item1, T... items) {
        return new FunctionalSet<>(ImmutableSet.copyOf(Lists.asList(item1, items)));
    }

    @Override
    public FunSet<T> foreach(Consumer<? super T> visitor) {
        super.foreach(visitor);
        return this;
    }

    @Override
    public FunList<T> toList() {
        return new FunctionalList<>(delegate.asList());
    }

    public FunctionalSet<T> toSet() {
        return this;
    }

    @Override
    public <U> FunSet<U> cast() {
        return blindCast(this);
    }

    static final EmptySet EMPTY = new EmptySet();
    @SuppressWarnings({"unchecked"})
    private static class EmptySet extends EmptyCollection implements FunSet {
        public ImmutableSet delegate() {
            return ImmutableSet.of();
        }

        @Override
        public EmptySet foreach(Consumer visitor) {
            return this;
        }

        @Override
        public FunSet cast() {
            return this;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        @Override public boolean equals(@Nullable Object object) {
            return object == this || (object instanceof Set && ((Set) object).isEmpty());
        }

        @Override public int hashCode() {
            return delegate().hashCode();
        }
    }
}
