package joshng.util.blocks;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import joshng.util.collect.Either;
import joshng.util.collect.Maybe;
import joshng.util.exceptions.ExceptionPolicy;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:10:50 AM
 */
public abstract class Source<T> extends F<Object, T> implements Callable<T>, Supplier<T> {
    private static final F GETTER = new F<Supplier, Object>() {
        public Object apply(Supplier from) {
            return from.get();
        }
    };
    public static final F<Source<?>, Runnable> AS_RUNNABLE = new F<Source<?>, Runnable>() {
        public Runnable apply(Source<?> input) {
            return input.asRunnable();
        }
    };

    private static final Source NULL_SOURCE = Source.<Object>ofInstance(null);

    @SuppressWarnings("unchecked")
    public static <T> Source<T> nullSource() {
        return NULL_SOURCE;
    }

    public static <T> Source<T> ofInstance(final T value) {
        return new SourceOfInstance<T>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Source<T> extendSupplier(final Supplier<? extends T> supplier) {
        if (supplier instanceof Source) return (Source<T>) supplier;
        return new Source<T>() { public T get() {
            return supplier.get();
        } };
    }

    public static <T> Source<T> extendRunnable(final Runnable runnable, final T result) {
        return new Source<T>() {
            @Override
            public T get() {
                runnable.run();
                return result;
            }
        };
    }

    public static <I, O> F<Supplier<? extends I>, Source<O>> mapper(Function<I, O> f) {
        final F<I, O> extended = F.extendF(f);
        return new F<Supplier<? extends I>, Source<O>>() {
            @Override
            public Source<O> apply(Supplier<? extends I> input) {
                return extended.bindFrom(input);
            }
        };
    }

    public static <I, O> F<Supplier<? extends I>, Source<O>> flatMapper(final Function<I, ? extends Supplier<O>> f) {
        return new F<Supplier<? extends I>, Source<O>>() {
            @Override
            public Source<O> apply(Supplier<? extends I> input) {
                return extendSupplier(input).flatMap(f);
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public static <T> F<Supplier<? extends T>, T> getter() {
        return GETTER;
    }

    public <U> Source<U> andThen(final Function<? super T, U> transformer) {
//        return F.extendF(transformer).bindFrom(this);
        return new Source<U>() { public U get() {
            return transformer.apply(Source.this.get());
        } };
    }

    public <U> Source<U> map(final Function<? super T, U> transformer) {
        return andThen(transformer);
    }

    public <U> Source<U> flatMap(final Function<? super T, ? extends Supplier<U>> transformer) {
        return new Source<U>() {
            @Override
            public U get() {
                return transformer.apply(Source.this.get()).get();
            }
        };
    }

    public Source<T> memoize() {
        return extendSupplier(Suppliers.memoize(this));
    }

    public Source<T> memoizeWithExpiration(long duration, TimeUnit timeUnit) {
        return extendSupplier(Suppliers.memoizeWithExpiration(this, duration, timeUnit));
    }

    public Runnable asRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                get();
            }
        };
    }

    public Supplier<T> asSupplier() {
        return this;
    }

    /**
     * Returns a Source that wraps the result of calling get in a Maybe, returning Maybe.not()
     * if the underlying call throws a Throwable which is "handled" by the provided ExceptionPolicy.
     *
     * <p>NOTE: any Throwable that is NOT handled by the ExceptionPolicy will still be thrown out
     * (wrapped in a RuntimeException if necessary by {@link Throwables#propagate}).
     */
    public Source<Maybe<T>> handlingExceptions(final ExceptionPolicy exceptionPolicy) {
        return new Source<Maybe<T>>() {
            @Override
            public Maybe<T> get() {
                try {
                    return definitely(Source.this.get());
                } catch (Throwable e) {
                    if (exceptionPolicy.apply(e)) return Maybe.not();
                    throw Throwables.propagate(e);
                }
            }
        };
    }

    public Source<T> recover(final ThrowingFunction<? super Exception, ? extends T> recovery) {
        return new Source<T>() {
            @Override public T get() {
                try {
                    return Source.this.get();
                } catch (Exception t) {
                    try {
                        return recovery.apply(t);
                    } catch (Exception e) {
                        throw Throwables.propagate(e);
                    }
                }
            }
        };
    }

    public Source<Either<Exception, T>> exceptionToLeft() {
        return new Source<Either<Exception, T>>() {
            @Override
            public Either<Exception, T> get() {
                return applyExceptionToLeft(null, Source.this);
            }
        };
    }

    public T apply(Object input) {
        return get();
    }

    public T call() {
        return get();
    }

    private static class SourceOfInstance<T> extends Source<T> {
        private final T value;

        public SourceOfInstance(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public <U> Source<U> andThen(Function<? super T, U> transformer) {
            return extendF(transformer).bind(value);
        }

        @Override
        public Source<T> memoize() {
            return this;
        }

        @Override
        public Source<T> memoizeWithExpiration(long duration, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public Source<Maybe<T>> handlingExceptions(ExceptionPolicy exceptionPolicy) {
            return Source.ofInstance(Maybe.definitely(value));
        }

        @Override
        public Source<Either<Exception, T>> exceptionToLeft() {
            return Source.ofInstance(Either.<Exception, T>right(value));
        }

        @Override
        public <U> Source<U> flatMap(Function<? super T, ? extends Supplier<U>> transformer) {
            return Source.<U>getter().compose(transformer).bind(value);
        }
    }

}
