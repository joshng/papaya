package joshng.util.concurrent;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import joshng.util.blocks.*;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Maybe;
import joshng.util.collect.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 12/5/11
 * Time: 11:22 PM
 */
public class FunFutures {
    private static final Logger LOG = LoggerFactory.getLogger(FunFutures.class);
    private static final FunFuture NULL_FUTURE = FunFutures.<Object>immediateFuture(null);

    private static final AsyncF SEQUENCER = new AsyncF<Iterable<ListenableFuture<Object>>, List<Object>>() {
        @Override
        public FunFuture<List<Object>> applyAsync(Iterable<ListenableFuture<Object>> input) {
            return allAsList(input);
        }
    };
    private static F GET_UNCHECKED = new F<Future<Object>, Object>() {
        public Object apply(Future<Object> from) {
            return getUnchecked(from);
        }
    };

    public static <T> FunFuture<T> immediateFuture(T value) {
        return extendFuture(Futures.immediateFuture(value));
    }

    public static <T> FunFuture<T> immediateFailedFuture(Throwable e) {
        return extendFuture(Futures.<T>immediateFailedFuture(unwrapExecutionException(e)));
    }

    public static <T> FunFuture<T> immediateCancelledFuture() {
        return extendFuture(Futures.<T>immediateCancelledFuture());
    }

    public static <T> FunFuture<List<T>> allAsList(Iterable<? extends ListenableFuture<? extends T>> input) {
        return extendFuture(Futures.allAsList(unwrapFutureIterable(input)));
    }

    public static <T> FunFuture<List<T>> successfulAsList(Iterable<? extends ListenableFuture<? extends T>> input) {
        return extendFuture(Futures.successfulAsList(unwrapFutureIterable(input)));
    }

    public static Cancellable extendCancellable(final Future future) {
        if (future instanceof Cancellable) return (Cancellable) future;
        return new Cancellable() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }
        };
    }

    public static <T> FunFuture<T> any(final Iterable<? extends ListenableFuture<? extends T>> inputs) {
        final Promise<T> promise = Promise.newPromise();
        promise.attachCancellableCompletion(new Cancellable() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                for (ListenableFuture<? extends T> input : inputs) {
                    input.cancel(mayInterruptIfRunning);
                }
                return false;
            }
        });

        for (ListenableFuture<? extends T> input : inputs) {
            promise.chainResult(input);
        }
        return promise;
    }

    @SuppressWarnings("unchecked")
    private static <T> Iterable<? extends ListenableFuture<? extends T>> unwrapFutureIterable(Iterable<? extends ListenableFuture<? extends T>> input) {
        // Guava copies to an immutable list; we may already have one wrapped in a FunList, so extract it here
        return input instanceof FunIterable ? ((FunIterable<ListenableFuture<T>>) input).toList().delegate() : input;
    }

    private static final F EXTENDER = new F<ListenableFuture, FunFuture>() {
        @SuppressWarnings("unchecked")
        @Override
        public FunFuture apply(ListenableFuture input) {
            return extendFuture(input);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> F<Future<? extends T>, T> getFromFuture() {
        return GET_UNCHECKED;
    }

    public static <T> T getUnchecked(Future<? extends T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e.getCause());
        }
    }

    public static <T> Maybe<T> getUnchecked(Future<? extends T> future, long timeout, TimeUnit timeUnit) {
        try {
            return definitely(future.get(timeout, timeUnit));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e.getCause());
        } catch (TimeoutException e) {
            return Maybe.not();
        }
    }

    public static <T, E extends Throwable> T getChecked(ListenableFuture<T> future, Class<E> exceptionClass) throws E {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Throwables.propagateIfPossible(cause, exceptionClass);
            throw new RuntimeException(cause);
        }
    }

    public static <T, E extends Throwable> Maybe<T> getChecked(ListenableFuture<T> future, long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
        try {
            return definitely(future.get(timeout, timeUnit));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Throwables.propagateIfPossible(cause, exceptionClass);
            throw new RuntimeException(cause);
        } catch (TimeoutException e) {
            return Maybe.not();
        }
    }

    public static <T> FunFuture<T> callSafely(Callable<? extends ListenableFuture<T>> callable) {
        try {
            return extendFuture(callable.call());
        } catch (Exception e) {
            return immediateFailedFuture(e);
        }
    }

    public static <T> FunFuture<T> immediateResult(Callable<T> block) {
        try {
            return FunFutures.immediateFuture(block.call());
        } catch (Throwable e) {
            return FunFutures.immediateFailedFuture(e);
        }
    }

    public static <T> Source<FunFuture<T>> immediateSafeCallable(final Callable<T> unsafe) {
        return new Source<FunFuture<T>>() {
            @Override
            public FunFuture<T> get() {
                try {
                    return immediateFuture(unsafe.call());
                } catch (Exception e) {
                    return immediateFailedFuture(e);
                }
            }
        };
    }

    public static void runSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            Thread currentThread = Thread.currentThread();
            Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
            if (exceptionHandler != null) {
                exceptionHandler.uncaughtException(currentThread, e);
            } else {
                LOG.warn("Uncaught exception in thread {}:", currentThread, e);
            }
        }
    }

    public static Sink<Future> cancelUnlessRunning() {
        return value -> {
            value.cancel(false);
        };
    }

    public static <T> FunIterable<T> getAll(Iterable<? extends Future<? extends T>> futures) {
        return FunIterable.map(futures, FunFutures.<T>getFromFuture());
    }

    @SuppressWarnings("unchecked")
    public static <T> AsyncF<Iterable<? extends ListenableFuture<T>>, List<T>> sequencer() {
        return SEQUENCER;
    }

    @SuppressWarnings("unchecked")
    public static <T> FunFuture<T> nullFuture() {
        return NULL_FUTURE;
    }

    public static <T> FunFuture<T> extendFuture(ListenableFuture<T> future) {
        if (future instanceof FunFuture) return (FunFuture<T>) future;
        return new FunctionalFuture<T>(future);
    }

    public static <T> FunRunnableFuture<T> funFutureTask(Callable<T> callable) {
        return funFutureTask(true, callable);
    }

    public static <T> FunRunnableFuture<T> funFutureTask(boolean allowCancelToInterrupt, Callable<T> callable) {
        return extendFutureTask(ListenableFutureTask.create(callable), allowCancelToInterrupt);
    }

    public static <T,P> FunRunnableFuture<T> prioritizedFutureTask(P priority, Comparator<? super P> comparator, boolean allowCancelToInterrupt, Callable<T> callable) {
        return new PrioritizedFutureTask<T,P>(ListenableFutureTask.create(callable), allowCancelToInterrupt, priority, comparator);
    }

    public static <T> FunRunnableFuture<T> extendFutureTask(ListenableFutureTask<T> futureTask, boolean allowCancelToInterrupt) {
        return new FunFutureTask<T>(futureTask, allowCancelToInterrupt);
    }

    @SuppressWarnings("unchecked")
    public static <T> F<ListenableFuture<T>, FunFuture<T>> extender() {
        return EXTENDER;
    }

    public static <I, O> FunFuture<O> map(ListenableFuture<I> future, Function<? super I, ? extends O> f) {
        return new FunctionalFuture<>(Futures.transform(future, (com.google.common.base.Function<? super I, ? extends O>)f::apply));
    }

    public static <I, O> FunFuture<O> map(ListenableFuture<I> future, Executor executor, Function<? super I, ? extends O> f) {
        return new FunctionalFuture<O>(Futures.transform(future, (com.google.common.base.Function<? super I, ? extends O>)f::apply, executor));
    }

    public static <I, O> AsyncF<ListenableFuture<? extends I>, O> mapper(final Function<? super I, ? extends O> mapper) {
        return input -> map(input, mapper);
    }

    public static <I, O> FunFuture<O> flatMap(ListenableFuture<I> future, AsyncFunction<? super I, ? extends O> f) {
        return new FunctionalFuture<O>(Futures.transform(future, f));
    }

    public static <I, O> AsyncF<ListenableFuture<? extends I>, O> flatMapper(final AsyncFunction<? super I, ? extends O> transformer) {
        return input -> flatMap(input, transformer);
    }

    public static <T, O> FunFuture<O> flatMap(ListenableFuture<T> future, Executor executor, AsyncFunction<? super T, ? extends O> f) {
        return new FunctionalFuture<O>(Futures.transform(future, f, executor));
    }

    private static final F MAYBE_WRAPPER = mapper(Maybe.of());
    private static final FunFuture EMPTY_FUTURE = immediateFuture(Maybe.not());

    @SuppressWarnings("unchecked")
    public static <T> F<ListenableFuture<? extends T>, FunFuture<Maybe<T>>> maybeWrapper() {
        return MAYBE_WRAPPER;
    }

    @SuppressWarnings("unchecked")
    public static <T> FunFuture<Maybe<T>> futureMaybeNot() {
        return EMPTY_FUTURE;
    }

    public static <T> FunFuture<Maybe<T>> asFutureOfMaybe(Maybe<? extends ListenableFuture<? extends T>> maybeOfFuture) {
        return maybeOfFuture.map(FunFutures.<T>maybeWrapper()).getOrElse(FunFutures.<T>futureMaybeNot());
    }

    public static <T> FunFuture<T> filter(ListenableFuture<T> future, final Predicate<? super T> filter) {
        return FunFutures.map(future, new Tapper<T>() {
            public void tap(T value) {
                if (!filter.test(value)) throw new FilteredFutureException();
            }
        });
    }

    public static <T> FunFuture<T> filter(ListenableFuture<T> future, final AsyncFunction<? super T, Boolean> filter) {
        return flatMap(future, new AsyncFunction<T, T>() {
            @Override
            public ListenableFuture<T> apply(final T value) throws Exception {
                return map(filter.apply(value), new F<Boolean, T>() {
                    @Override public T apply(Boolean input) {
                        if (!input) throw new FilteredFutureException();
                        return value;
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T, C> FunFuture<C> filter(ListenableFuture<T> future, Class<C> castClass) {
        return (FunFuture<C>) filter(future, Pred.instanceOf(castClass));
    }

    public static <T> FunFuture<T> recover(final ListenableFuture<T> future, final ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return recover(future, MoreExecutors.sameThreadExecutor(), exceptionHandler);
    }

    public static <T> FunFuture<T> recover(final ListenableFuture<T> future, final Executor executor, final ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return extendFuture(Futures.withFallback(future, new FutureFallback<T>() {
            @Override public ListenableFuture<T> create(Throwable t) throws Exception {
                return Futures.immediateFuture(exceptionHandler.apply(t));
            }
        }, executor));
    }

    public static <T> FunFuture<T> recoverWith(final ListenableFuture<T> future, final AsyncFunction<? super Throwable, ? extends T> exceptionHandler) {
        return extendFuture(Futures.withFallback(future, new FutureFallback<T>() {
            @SuppressWarnings("unchecked")
            @Override public ListenableFuture<T> create(Throwable t) throws Exception {
                return (ListenableFuture<T>) exceptionHandler.apply(t);
            }
        }));
    }


    public static <T> FunFuture<T> uponCompletion(final ListenableFuture<T> future, Executor executor, final Runnable sideEffect) {
        final Promise<T> promise = Promise.newPromise();
        promise.attachFutureCompletion(future);
        future.addListener(new SideEffect() {
            @Override public void run() {
                try {
                    runSafely(sideEffect);
                } finally {
                    promise.completeWith(future);
                }
            }
        }, executor);
        return promise;
    }

    public static <T> FunFuture<T> uponCompletion(final ListenableFuture<T> future, Executor executor, final FutureCallback<? super T> callback) {
        return uponCompletion(future, executor, new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onSuccess(Uninterruptibles.getUninterruptibly(future));
                } catch (ExecutionException e) {
                    callback.onFailure(e.getCause());
                }
            }
        });
    }

    public static <T> FunFuture<T> uponSuccess(final ListenableFuture<T> future, Executor executor, final Consumer<? super T> successObserver) {
        return uponCompletion(future, executor, new FutureCallback<T>() {
            @Override public void onSuccess(T result) {
                successObserver.accept(result);
            }

            @Override public void onFailure(Throwable t) {
            }
        });
    }

    public static <T> FunFuture<T> uponFailure(final ListenableFuture<T> future, Executor executor, final Consumer<? super Throwable> failureObserver) {
        return uponCompletion(future, executor, new FutureCallback<T>() {
            @Override public void onSuccess(T result) {
            }

            @Override public void onFailure(Throwable t) {
                failureObserver.accept(t);
            }
        });
    }

    public static <A, B> FunFuture<Pair<A, B>> zip(final ListenableFuture<A> a, final ListenableFuture<B> b) {
        return extendFuture(Futures.transform(Futures.allAsList(ImmutableList.of(a, b)), new F<List<Object>, Pair<A, B>>() {
            @Override public Pair<A, B> apply(List<Object> input) {
                return Pair.of(FunFutures.getUnchecked(a), FunFutures.getUnchecked(b));
            }
        }));
    }

    public static <K,V> Pair<FunFuture<K>, FunFuture<V>> unzip(ListenableFuture<? extends Map.Entry<? extends K, V>> futureOfPair) {
        return Pair.of(map(futureOfPair, Pair.<K>getFirstFromPair()), map(futureOfPair, Pair.<V>getSecondFromPair()));
    }

    public static <T> F<Future<? extends T>, Source<T>> asSource() {
        return FunFutures.<T>getFromFuture().binder();
    }

    public static <T> Source<T> asSource(Future<T> future) {
        return FunFutures.<T>getFromFuture().bind(future);
    }

    public static Throwable unwrapExecutionException(Throwable e) {
        Throwable cause;
        if (e instanceof ExecutionException || e instanceof UncheckedExecutionException) {
            cause = Objects.firstNonNull(e.getCause(), e);
        } else {
            cause = e;
        }
        return cause;
    }

    private static class FunctionalFuture<T> extends ForwardingListenableFuture.SimpleForwardingListenableFuture<T> implements FunFuture<T> {
        protected FunctionalFuture(ListenableFuture<T> delegate) {
            super(delegate);
        }

        @Override
        public T getUnchecked() {
            return FunFutures.getUnchecked(delegate());
        }

        @Override
        public Maybe<T> getWithin(long timeout, TimeUnit timeUnit) {
            return FunFutures.getUnchecked(delegate(), timeout, timeUnit);
        }

        @Override
        public <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E {
            return FunFutures.getChecked(delegate(), exceptionClass);
        }

        @Override
        public <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
            return FunFutures.getChecked(delegate(), timeout, timeUnit, exceptionClass);
        }

        @Override
        public <O> FunFuture<O> map(Function<? super T, ? extends O> f) {
            return FunFutures.map(delegate(), f);
        }

        @Override
        public <O> FunFuture<O> map(Executor executor, Function<? super T, ? extends O> f) {
            return FunFutures.map(delegate(), executor, f);
        }

        @Override
        public <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
            return FunFutures.flatMap(delegate(), f);
        }

        @Override
        public <O> FunFuture<O> flatMap(Executor executor, AsyncFunction<? super T, ? extends O> f) {
            return FunFutures.flatMap(delegate(), executor, f);
        }

        @Override
        public FunFuture<T> filter(final Predicate<? super T> filter) {
            return FunFutures.filter(delegate(), filter);
        }

        @Override
        public FunFuture<T> filter(AsyncFunction<? super T, Boolean> filter) {
            return FunFutures.filter(delegate(), filter);
        }

        @Override
        public <C> FunFuture<C> filter(Class<C> castClass) {
            return FunFutures.filter(delegate(), castClass);
        }

        @Override
        public FunFuture<T> recover(ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
            return FunFutures.recover(delegate(), exceptionHandler);
        }

        @Override
        public FunFuture<T> recover(Executor executor, final ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
            return FunFutures.recover(delegate(), executor, exceptionHandler);
        }

        @Override
        public FunFuture<T> recoverWith(AsyncFunction<? super Throwable, ? extends T> exceptionHandler) {
            return FunFutures.recoverWith(delegate(), exceptionHandler);
        }

        @Override
        public FunFuture<T> uponCompletion(Runnable runnable) {
            return uponCompletion(MoreExecutors.sameThreadExecutor(), runnable);
        }

        @Override
        public FunFuture<T> uponCompletion(final Executor executor, final Runnable sideEffect) {
            return FunFutures.uponCompletion(this, executor, sideEffect);
        }

        @Override
        public FunFuture<T> uponCompletion(FutureCallback<? super T> callback) {
            return uponCompletion(MoreExecutors.sameThreadExecutor(), callback);
        }

        @Override
        public FunFuture<T> uponCompletion(Executor executor, FutureCallback<? super T> callback) {
            return FunFutures.uponCompletion(this, executor, callback);
        }

        @Override
        public FunFuture<T> uponSuccess(Consumer<? super T> successObserver) {
            return uponSuccess(MoreExecutors.sameThreadExecutor(), successObserver);
        }

        @Override
        public FunFuture<T> uponSuccess(Executor executor, Consumer<? super T> successObserver) {
            return FunFutures.uponSuccess(this, executor, successObserver);
        }

        @Override
        public FunFuture<T> uponFailure(Consumer<? super Throwable> errorObserver) {
            return uponFailure(MoreExecutors.sameThreadExecutor(), errorObserver);
        }

        @Override
        public FunFuture<T> uponFailure(Executor executor, Consumer<? super Throwable> errorObserver) {
            return FunFutures.uponFailure(this, executor, errorObserver);
        }

        @Override
        public <U> FunFuture<Pair<T, U>> zip(ListenableFuture<U> that) {
            return FunFutures.zip(this, that);
        }
    }

    private static class FunFutureTask<T> extends FunctionalFuture<T> implements FunRunnableFuture<T> {
        private volatile boolean interruptOnCancel;

        protected FunFutureTask(ListenableFutureTask<T> delegate, boolean interruptOnCancel) {
            super(delegate);
            this.interruptOnCancel = interruptOnCancel;
        }

        public boolean isInterruptOnCancel() {
            return interruptOnCancel;
        }

        public void setInterruptOnCancel(boolean interruptOnCancel) {
            this.interruptOnCancel = interruptOnCancel;
        }

        @Override
        public void run() {
            ((ListenableFutureTask<T>)delegate()).run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(interruptOnCancel && mayInterruptIfRunning);
        }
    }

    private static class PrioritizedFutureTask<T, P> extends FunFutureTask<T> implements Comparable<PrioritizedFutureTask<T,P>> {
        private final Comparator<? super P> comparator;
        private final P priority;

        private PrioritizedFutureTask(ListenableFutureTask<T> delegate, boolean allowCancelToInterrupt, P priority, Comparator<? super P> comparator) {
            super(delegate, allowCancelToInterrupt);
            this.comparator = comparator;
            this.priority = priority;
        }

        @Override
        public int compareTo(PrioritizedFutureTask<T,P> that) {
            assert comparator.equals(that.comparator) : "Mismatched PrioritizedFutureTask comparators";
            return comparator.compare(priority, that.priority);
        }
    }

    public static class FilteredFutureException extends NoSuchElementException {
        public FilteredFutureException() {
            super("Future value did not match filter");
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
