package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import joshng.util.blocks.SideEffect;
import joshng.util.blocks.ThrowingRunnable;
import joshng.util.exceptions.MultiException;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: 12/20/12
 * Time: 8:30 AM
 */
public class Promise<T> extends AbstractFunFuture<T> {
  private static final AtomicReferenceFieldUpdater<Promise, Cancellable> CANCELLABLE_COMPLETION_UPDATER = AtomicReferenceFieldUpdater
          .newUpdater(Promise.class, Cancellable.class, "_cancellableCompletion");
  @SuppressWarnings("UnusedDeclaration")
  private volatile Cancellable _cancellableCompletion = null;

  public static <T> Promise<T> newPromise() {
    return new Promise<>();
  }

  public static <T> Promise<T> newPromise(Consumer<Promise<T>> initializer) {
    Promise<T> promise = new Promise<>();
    initializer.accept(promise);
    return promise;
  }

  /**
   * Immediately applies the value returned by calling the given resultSupplier as the successful outcome of this
   * {@link Future}, or, if the callable throws an Exception, applies that exception as the failure outcome.<p/>
   * <p>
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

  public FunFuture<T> completeOrRecoverWith(
          ListenableFuture<T> future,
          AsyncFunction<? super Exception, ? extends T> exceptionHandler
  ) {
    if (attachFutureCompletion(future)) {
      Futures.addCallback(future, new FutureCallback<T>() {
        @Override public void onSuccess(@Nullable T result) {
          setSuccess(result);
        }

        @Override public void onFailure(Throwable t) {
          Exception cause = FunFuture.unwrapExecutionException(t);
          completeWithResultOf(() -> exceptionHandler.apply(cause));
        }
      });
    }
    return this;
  }

  public <I> FunFuture<T> completeWithFlatMap(
          ListenableFuture<I> input,
          AsyncFunction<? super I, ? extends T> function
  ) {
    if (attachFutureCompletion(input)) {
      input.addListener(() -> completeWithResultOf(() -> function.apply(Uninterruptibles.getUninterruptibly(input))),
              MoreExecutors.sameThreadExecutor());
    }
    return this;
  }

  public FunFuture<T> completeWithSideEffect(ListenableFuture<? extends T> future, Runnable sideEffect) {
    attachFutureCompletion(future);
    future.addListener(() -> {
      try {
        SideEffect.runIgnoringExceptions(sideEffect);
      } finally {
        completeWith(future);
      }
    }, MoreExecutors.sameThreadExecutor());
    return this;
  }

  public void catchFailure(ThrowingRunnable block) {
    try {
      block.run();
    } catch (Throwable t) {
      setFailure(t);
    }
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

  public FunFuture<T> completeWithResultOf(Callable<? extends ListenableFuture<? extends T>> futureResultSupplier) {
    if (!isDone()) {
      try {
        completeWith(futureResultSupplier.call());
      } catch (Throwable e) {
        setFailure(e);
      }
    }
    return this;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
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
   * returned from calls to {@link #get}); usually the result of calling {@link #succeed}(resultValue).
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
   * thrown from calls to {@link #get}); usually the result of calling {@link #fail}(failure).
   */
  protected boolean handleFailure(Throwable failure) throws Throwable {
    return fail(failure);
  }

  /**
   * Attempts to apply the provided resultValue as the result of this {@link Future}.
   *
   * @return {@code true} if the provided result was applied to the associated {@link Future} (and will thus be
   * returned from calls to {@link #get});
   * <p>
   * {@code false} if the Future was already completed by an earlier call to {@link #succeed}, {@link #fail},
   * or {@link #cancel}
   */
  protected final boolean succeed(T resultValue) {
    return onCompleteInternal(super.set(resultValue));
  }


  /**
   * Attempts to apply the provided failure as the result of this {@link Future}.
   *
   * @return {@code true} if the provided failure was applied to the associated {@link Future} (and will thus be
   * thrown from calls to {@link #get});
   * <p>
   * {@code false} if the Future was already completed by an earlier call to {@link #succeed}, {@link #fail},
   * or {@link #cancel}
   */
  protected final boolean fail(Throwable failure) {
    Throwable cause = FunFuture.unwrapExecutionException(failure);
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
   * Promise was <em>already</em> cancelled.
   */
  protected boolean attachFutureCompletion(Future<?> other) {
    return attachCancellableCompletion(Cancellable.extendFuture(other));
  }

  /**
   * Arranges for the given {@link Cancellable} to be cancelled if this one is.
   *
   * @return {@code true} if the Cancellable was attached for cancellation and is still alive; {@code false} if this
   * Promise was <em>already</em> cancelled.
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
}
