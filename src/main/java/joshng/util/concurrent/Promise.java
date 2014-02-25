package joshng.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.Consumer;
import joshng.util.blocks.Source;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Maybe;
import joshng.util.collect.Pair;
import joshng.util.exceptions.MultiException;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * User: josh
 * Date: 12/20/12
 * Time: 8:30 AM
 */
public class Promise<T> extends AbstractFuture<T> implements FunFuture<T> {
    private static final AtomicReferenceFieldUpdater<Promise, Cancellable> CANCELLABLE_COMPLETION_UPDATER = AtomicReferenceFieldUpdater.newUpdater(Promise.class, Cancellable.class, "_cancellableCompletion");
    @SuppressWarnings("UnusedDeclaration") private volatile Cancellable _cancellableCompletion = null;

    public static <T> Promise<T> newPromise() {
        return new Promise<>();
    }

    /**
     * Immediately applies the value returned by calling the given resultSupplier as the successful outcome of this
     * {@link Future}, or, if the callable throws an Exception, applies that exception as the failure outcome.<p/>
     *
     * Note that if this Promise has already been completed via other means (eg, if it has been {@link #cancel cancelled}),
     * then the provided callable will <b>*not be called*</b>.
     *
     * @return the {@link FunFuture} view of this Promise
     */
    public FunFuture<T> complete(Callable<? extends T> resultSupplier) {
        if (!isDone()) {
            try {
                setSuccess(resultSupplier.call());
            } catch (Throwable e) {
                setFailure(e);
            }
        }
        return this;
    }

    /**
     * Arranges for the result of this Promise to be the same as the {@code futureResult}. Also passes {@futureResult}
     * to {@link #attachFutureCompletion} to arrange for it to be {@link Future#cancel cancelled} if this Promise is
     * {@link #cancel cancelled}.
     *
     * @param futureResult the future result to use for this Promise.
     * @return the {@link FunFuture} view of this Promise
     */
    public FunFuture<T> completeWith(ListenableFuture<? extends T> futureResult) {
        if (attachFutureCompletion(futureResult)) {
            chainResult(futureResult);
        }
        return this;
    }

    protected void chainResult(ListenableFuture<? extends T> futureResult) {
        Futures.addCallback(futureResult, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                setSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                setFailure(t);
            }
        });
    }

    public FunFuture<T> completeWithResultOf(Callable<? extends FunFuture<? extends T>> futureResultSupplier) {
        if (!isDone()) {
            try {
                completeWith(futureResultSupplier.call());
            } catch (Throwable e) {
                setFailure(e);
            }
        }
        return this;
    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);
        if (cancelled) cancelRunningCompletion(mayInterruptIfRunning);
        return cancelled;
    }

    public boolean wasCancelledWithInterruption() {
        return wasInterrupted();
    }

    public final boolean setSuccess(T resultValue) {
        try {
            return handleSuccess(resultValue);
        } catch (Exception e) {
            return fail(e);
        }
    }

    public final boolean setFailure(Throwable failure) {
        try {
            return handleFailure(failure);
        } catch (Throwable throwable) {
            return fail(failure, throwable);
        }
    }

    /**
     * Invoked by the public {@link #setSuccess} to handle application of the given resultValue. Overridable to
     * intercept and impose custom handling upon a successful outcome. Implementations should generally lead to an
     * invocation of either {@link #succeed} or {@link #fail}.
     *
     * @return {@code true} if the provided resultValue was applied to the associated {@link Future} (and will thus be
     *         returned from calls to {@link #get}); usually the result of calling {@link #succeed}(resultValue).
     */
    protected boolean handleSuccess(T resultValue) {
        return succeed(resultValue);
    }

    /**
     * Invoked by the public {@link #setFailure} to handle application of the given error. Overridable to intercept and
     * impose custom handling upon a failed outcome. Implementations should generally lead to an invocation of either
     * {@link #succeed} or {@link #fail}.
     *
     * @return {@code true} if the provided failure was applied to the associated {@link Future} (and will thus be
     *         thrown from calls to {@link #get}); usually the result of calling {@link #fail}(failure).
     */
    protected boolean handleFailure(Throwable failure) throws Throwable {
        return fail(failure);
    }

    /**
     * Attempts to apply the provided resultValue as the result of this {@link Future}.
     *
     * @return {@code true} if the provided result was applied to the associated {@link Future} (and will thus be
     *         returned from calls to {@link #get});
     *         <p/>
     *         {@code false} if the Future was already completed by an earlier call to {@link #succeed}, {@link #fail},
     *         or {@link #cancel}
     */
    protected final boolean succeed(T resultValue) {
        return onCompleteInternal(super.set(resultValue));
    }


    /**
     * Attempts to apply the provided failure as the result of this {@link Future}.
     *
     * @return {@code true} if the provided failure was applied to the associated {@link Future} (and will thus be
     *         thrown from calls to {@link #get});
     *         <p/>
     *         {@code false} if the Future was already completed by an earlier call to {@link #succeed}, {@link #fail},
     *         or {@link #cancel}
     */
    protected final boolean fail(Throwable failure) {
        Throwable cause = FunFutures.unwrapExecutionException(failure);
        if (cause instanceof CancellationException) {
            return onCompleteInternal(super.cancel(false));
        } else {
            return onCompleteInternal(setException(cause));
        }
    }

    /**
     * Arranges for the given {@link Future} to be cancelled if this one is.
     *
     * @return {@code true} if the Future was attached for cancellation and is still alive; {@code false} if this
     *         Promise was <em>already</em> cancelled.
     */
    protected boolean attachFutureCompletion(Future<?> other) {
        return attachCancellableCompletion(FunFutures.extendCancellable(other));
    }

    /**
     * Arranges for the given {@link Cancellable} to be cancelled if this one is.
     *
     * @return {@code true} if the Cancellable was attached for cancellation and is still alive; {@code false} if this
     *         Promise was <em>already</em> cancelled.
     */
    protected boolean attachCancellableCompletion(Cancellable cancellableCompletion) {
        _cancellableCompletion = cancellableCompletion;
        boolean cancelled = isCancelled();
        if (cancelled) cancelRunningCompletion(wasInterrupted());
        return !cancelled;
    }

    protected void cancelRunningCompletion(boolean mayInterruptIfRunning) {
        Cancellable completion = CANCELLABLE_COMPLETION_UPDATER.getAndSet(this, null);
        if (completion != null) completion.cancel(mayInterruptIfRunning);
    }

    private boolean onCompleteInternal(boolean result) {
        _cancellableCompletion = null;
        return result;
    }

    protected final boolean fail(Throwable throwable1, Throwable throwable2) {
        return fail(MultiException.Empty.with(throwable1).with(throwable2).getCombinedThrowable().getOrThrow());
    }


    protected final boolean set(T result) {
        return succeed(result);
    }

    @Override
    public <O> FunFuture<O> map(Executor executor, Function<? super T, ? extends O> f) {
        return FunFutures.map(this, executor, f);
    }

    @Override
    public <O> FunFuture<O> flatMap(Executor executor, AsyncFunction<? super T, ? extends O> f) {
        return FunFutures.flatMap(this, executor, f);
    }

    @Override
    public T getUnchecked() {
        return FunFutures.getUnchecked(this);
    }

    @Override
    public Maybe<T> getWithin(long timeout, TimeUnit timeUnit) {
        return FunFutures.getUnchecked(this, timeout, timeUnit);
    }

    @Override
    public <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E {
        return FunFutures.getChecked(this, exceptionClass);
    }

    @Override
    public <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
        return FunFutures.getChecked(this, timeout, timeUnit, exceptionClass);
    }

    public Maybe<T> toMaybe() {
        return Maybe.from(asSource());
    }

    public Source<T> asSource() {
        return FunFutures.asSource(this);
    }

    @Override
    public <O> FunFuture<O> map(Function<? super T, ? extends O> f) {
        return FunFutures.map(this, f);
    }

    @Override
    public <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
        return FunFutures.flatMap(this, f);
    }

    @Override
    public FunFuture<T> filter(Predicate<? super T> filter) {
        return FunFutures.filter(this, filter);
    }

    @Override
    public FunFuture<T> filter(AsyncFunction<? super T, Boolean> filter) {
        return FunFutures.filter(this, filter);
    }

    @Override
    public FunFuture<T> recover(ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recover(this, exceptionHandler);
    }

    @Override
    public FunFuture<T> recover(Executor executor, ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recover(this, executor, exceptionHandler);
    }

    @Override
    public FunFuture<T> recoverWith(AsyncFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recoverWith(this, exceptionHandler);
    }

    @Override
    public <C> FunFuture<C> filter(Class<C> castClass) {
        return FunFutures.filter(this, castClass);
    }

    @Override
    public FunFuture<T> uponCompletion(Runnable runnable) {
        return uponCompletion(MoreExecutors.sameThreadExecutor(), runnable);
    }

    @Override
    public FunFuture<T> uponCompletion(Executor executor, Runnable runnable) {
        return FunFutures.uponCompletion(this, executor, runnable);
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
