package com.joshng.util.concurrent;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.Uninterruptibles;
import com.joshng.util.blocks.F;
import com.joshng.util.blocks.Pred;
import com.joshng.util.blocks.SideEffect;
import com.joshng.util.blocks.Sink;
import com.joshng.util.blocks.Source;
import com.joshng.util.blocks.ThrowingBiFunction;
import com.joshng.util.blocks.ThrowingConsumer;
import com.joshng.util.blocks.ThrowingFunction;
import com.joshng.util.blocks.ThrowingRunnable;
import com.joshng.util.collect.Either;
import com.joshng.util.collect.FunIterable;
import com.joshng.util.collect.Maybe;
import com.joshng.util.collect.Nothing;
import com.joshng.util.collect.Pair;
import com.joshng.util.concurrent.trackers.FutureSuccessTracker;
import com.joshng.util.exceptions.FatalErrorHandler;
import com.joshng.util.exceptions.UncheckedInterruptedException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: 12/5/11
 * Time: 11:22 PM
 */
public interface FunFuture<T> extends ListenableFuture<T>, Cancellable {
  final FunFuture NULL_FUTURE = FunFuture.<Object>immediateFuture(null);

  F GET_UNCHECKED = (F<Future<Object>, Object>) FunFuture::getUnchecked;
  FunFuture<Boolean> FALSE = FunFuture.immediateFuture(false);
  FunFuture<Boolean> TRUE = FunFuture.immediateFuture(true);
  FunFuture<Nothing> NOTHING = Nothing.FUTURE;
  AsyncF<Object, Nothing> REPLACE_WITH_NOTHING = (Object discarded) -> NOTHING;

  public static <T> FunFuture<T> immediateFuture(T value) {
    return new ImmediateSuccess<>(value);
  }

  public static <T> FunFuture<T> immediateFailedFuture(Throwable e) {
    return newFuture(Futures.<T>immediateFailedFuture(unwrapExecutionException(e)));
  }

  public static <T> FunFuture<T> immediateCancelledFuture() {
    return newFuture(Futures.<T>immediateCancelledFuture());
  }

  public static FunFuture<Boolean> immediateBoolean(boolean value) {
    return value ? TRUE : FALSE;
  }

  public static <T> FunFuture<List<T>> allAsList(Iterable<? extends ListenableFuture<? extends T>> input) {
    return newFuture(Futures.allAsList(unwrapFutureIterable(input)));
  }

  public static FunFuture<Nothing> trackSuccess(ListenableFuture<?>... futures) {
    return trackSuccess(Arrays.asList(futures));
  }

  public static FunFuture<Nothing> trackSuccess(Iterable<? extends ListenableFuture<?>> futureList) {
    return new FutureSuccessTracker().trackAll(Iterables.transform(futureList, FunFuture::toCompletable)).setNoMoreJobs();
  }

  public static <T> FunFuture<List<T>> successfulAsList(Iterable<? extends ListenableFuture<? extends T>> input) {
    return newFuture(Futures.successfulAsList(unwrapFutureIterable(input)));
  }

  public static <T> FunFuture<T> any(final Iterable<? extends ListenableFuture<? extends T>> inputs) {
    final Promise<T> promise = Promise.newPromise();
    promise.attachCancellableCompletion(mayInterruptIfRunning -> {
      for (ListenableFuture<? extends T> input : inputs) {
        input.cancel(mayInterruptIfRunning);
      }
      return false;
    });

    for (ListenableFuture<? extends T> input : inputs) {
      promise.chainResult(input);
    }
    return promise;
  }

  static <T> CompletableFuture<T> toCompletable(ListenableFuture<T> listenable) {
    CompletablePromise<T> promise = new CompletablePromise<>();
    listenable.addListener(() -> promise.tryComplete(listenable::get), MoreExecutors.directExecutor());
    return promise;
  }

  static <T> FunFuture<T> extend(CompletionStage<T> completionStage) {
    Promise<T> promise = new Promise<>();
    CompletableFuture<T> future = completionStage.toCompletableFuture();
    promise.attachFutureCompletion(future);
    future.whenComplete((v, e) -> {
      if (e == null) {
        promise.succeed(v);
      } else {
        promise.fail(e);
      }
    });
    return promise;
  }

  @SuppressWarnings("unchecked")
  static <T> Iterable<? extends ListenableFuture<? extends T>> unwrapFutureIterable(Iterable<? extends ListenableFuture<? extends T>> input) {
    // Guava copies to an immutable list; we may already have one wrapped in a FunList, so extract it here
    return input instanceof FunIterable ? ((FunIterable<ListenableFuture<T>>) input).toList().delegate() : input;
  }

  @SuppressWarnings("unchecked")
  public static <T> F<Future<? extends T>, T> getFromFuture() {
    return GET_UNCHECKED;
  }

  public static <T> T getUnchecked(Future<? extends T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw UncheckedInterruptedException.propagate(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof UncheckedExecutionException) throw (UncheckedExecutionException) cause;
      throw new UncheckedExecutionException(cause);
    }
  }

  default CompletableFuture<T> toCompletableFuture() {
    return FunFuture.toCompletable(this);
  }

  default T getUnchecked() {
    return FunFuture.getUnchecked(this);
  }

  default <V> FunFuture<V> replace(V value) {
    return replaceWith(Futures.immediateFuture(value));
  }

  default <V> FunFuture<V> replaceWith(ListenableFuture<V> replacement) {
    return flatMap(v -> replacement);
  }

  default FunFuture<Nothing> discardValue() {
    return flatMap(REPLACE_WITH_NOTHING);
  }

  default Maybe<T> getWithin(long timeout, TimeUnit timeUnit) {
    try {
      return Maybe.definitely(get(timeout, timeUnit));
    } catch (InterruptedException e) {
      throw UncheckedInterruptedException.propagate(e);
    } catch (ExecutionException e) {
      throw new UncheckedExecutionException(e.getCause());
    } catch (TimeoutException e) {
      return Maybe.not();
    }
  }

  default FunFuture<T> cancelAfterTimeout(boolean mayInterruptIfRunning, long timeout, TimeUnit timeUnit, ScheduledExecutorService scheduler) {
    scheduler.schedule(() -> cancel(mayInterruptIfRunning), timeout, timeUnit);
    return this;
  }

  default <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E {
    try {
      return get();
    } catch (InterruptedException e) {
      throw UncheckedInterruptedException.propagate(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, exceptionClass);
      throw new RuntimeException(cause);
    }
  }

  default <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
    try {
      return Maybe.definitely(get(timeout, timeUnit));
    } catch (InterruptedException e) {
      throw UncheckedInterruptedException.propagate(e);
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
      return FunFuture.immediateFuture(block.call());
    } catch (Throwable e) {
      return FunFuture.immediateFailedFuture(e);
    }
  }

  public static FunFuture<Nothing> runSafely(ThrowingRunnable sideEffect) {
    try {
      sideEffect.run();
      return Nothing.FUTURE;
    } catch (Exception e) {
      return immediateFailedFuture(e);
    }
  }

  public static Sink<Future> cancelUnlessRunning() {
    return value -> {
      value.cancel(false);
    };
  }

  public static <T> FunIterable<T> getAll(Iterable<? extends Future<? extends T>> futures) {
    return FunIterable.map(futures, FunFuture.<T>getFromFuture());
  }

  @SuppressWarnings("unchecked")
  public static <T> FunFuture<T> nullFuture() {
    return NULL_FUTURE;
  }

  public static <T> FunFuture<T> extendFuture(ListenableFuture<T> future) {
    if (future instanceof FunFuture) return (FunFuture<T>) future;
    return newFuture(future);
  }

  static <T> ForwardingFunFuture<T> newFuture(ListenableFuture<T> future) {
    return new ForwardingFunFuture<>(future);
  }

  public static FunRunnableFuture<Nothing> funFutureTask(Runnable runnable) {
    return FunFuture.funFutureTask(() -> {
      runnable.run();
      return Nothing.NOTHING;
    });
  }

  public static <T> FunRunnableFuture<T> funFutureTask(Callable<T> callable) {
    return funFutureTask(true, callable);
  }

  public static <T> FunRunnableFuture<T> funFutureTask(boolean allowCancelToInterrupt, Callable<T> callable) {
    return extendFutureTask(ListenableFutureTask.create(callable), allowCancelToInterrupt);
  }

  public static <T, P> FunRunnableFuture<T> prioritizedFutureTask(P priority, Comparator<? super P> comparator, boolean allowCancelToInterrupt, Callable<T> callable) {
    return new PrioritizedFutureTask<T, P>(ListenableFutureTask.create(callable), allowCancelToInterrupt, priority, comparator);
  }

  public static <T> FunRunnableFuture<T> extendFutureTask(ListenableFutureTask<T> futureTask, boolean allowCancelToInterrupt) {
    return new FunFutureTask<T>(futureTask, allowCancelToInterrupt);
  }

  public static <T> FunFuture<T> dereference(ListenableFuture<? extends ListenableFuture<T>> futureOfFuture) {
    return Promise.<T>newPromise().completeWithFlatMap(futureOfFuture, AsyncF.asyncIdentity());
  }

  default <O> ForwardingFunFuture<O> wrapFuture(ListenableFuture<O> future) {
    return new ForwardingFunFuture<>(future);
  }

  default <O> FunFuture<O> map(ThrowingFunction<? super T, ? extends O> function) {
    AsyncF<? super T, ? extends O> asyncF = AsyncF.liftFunction(function);
    return flatMap(asyncF);
  }

  default <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
    return flatMapToPromise(f, Promise.newPromise());
  }

  default <O, P extends Promise<O>> P flatMapToPromise(AsyncFunction<? super T, ? extends O> f, P promise) {
    promise.completeWithFlatMap(this, f);
    return promise;
  }

  default <O> FunFutureMaybe<O> mapMaybe(ThrowingFunction<? super T, Maybe<O>> f) {
    return flatMapMaybe(AsyncF.liftFunction(f));
  }

  default <L, R> FunFutureEither<L, R> mapEither(ThrowingFunction<? super T, Either<L, R>> f) {
    return flatMapEither(AsyncF.liftFunction(f));
  }

  default <L, R> FunFutureEither<L, R> flatMapEither(AsyncFunction<? super T, ? extends Either<L, R>> f) {
    return flatMapToPromise(f, new FunFutureEither.EitherPromise<>());
  }

  default <O> FunFuture<O> thenAsync(Callable<? extends ListenableFuture<O>> nextTask) {
    return flatMap((T ignored) -> callSafely(nextTask));
  }

  default <O> FunFuture<O> thenReplace(Callable<O> replacementSource) {
    return flatMap((T ignored) -> immediateResult(replacementSource));
  }

  default FunFuture<Nothing> thenRun(Runnable runnable) {
    return thenReplace(Executors.callable(runnable, Nothing.NOTHING));
  }

  default FunFuture<T> tap(ThrowingConsumer<? super T> sideEffect) {
    return tapAsync((AsyncF<T, Nothing>) result -> {
              sideEffect.accept(result);
              return Nothing.FUTURE;
            });
  }

  default <O> FunFuture<T> tapAsync(AsyncF<? super T, O> sideEffect) {
    AsyncFunction<T, T> f = (T result) -> sideEffect.apply(result).replace(result);
    return flatMapToPromise(f, newCompletionPromise());
  }

  default <O> FunFuture<O> mapUnchecked(ThrowingFunction<? super T, ? extends O> throwingFunction) {
    return map(
            t -> {
              try {
                return throwingFunction.apply(t);
              } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new UncheckedExecutionException(e);
              }
            });
  }

  default <V> FunFuturePair<T,V> asKeyTo(ThrowingFunction<? super T, V> function) {
    return mapPair(k -> Pair.of(k, function.apply(k)));
  }

  default <K> FunFuturePair<K,T> asValueFrom(ThrowingFunction<? super T, K> function) {
    return mapPair(k -> Pair.of(function.apply(k), k));
  }

  default <K,V> FunFuturePair<K,V> mapPair(ThrowingFunction<? super T, ? extends Map.Entry<K,V>> function) {
    return flatMapPair((AsyncF<? super T, ? extends Map.Entry<K, V>>) AsyncF.liftFunction(function));
  }

  default <K,V> FunFuturePair<K,V> flatMapPair(AsyncFunction<? super T, ? extends Map.Entry<K,V>> function) {
    return flatMapToPromise(function, new FunFuturePair.PairPromise<>());
  }

  static <K, V> FunFuturePair.ForwardingFunFuturePair<K, V> newFuturePair(ListenableFuture<? extends Map.Entry<K, V>> future) {
    return new FunFuturePair.ForwardingFunFuturePair<>(future);
  }

  default <O> FunFutureMaybe<O> flatMapMaybe(AsyncFunction<? super T, Maybe<O>> f) {
    return flatMapToPromise(f, new FunFutureMaybe.MaybePromise<>());
  }

  static <I, O> F<ListenableFuture<? extends I>, FunFutureMaybe<O>> maybeMapper(ThrowingFunction<? super I, Maybe<O>> maybeFunction) {
    return future -> extendFuture(future).mapMaybe(maybeFunction);
  }

  default FunFuture<Nothing> foreach(ThrowingConsumer<? super T> sideEffect) {
    return flatMap(t -> {
      sideEffect.accept(t);
      return Nothing.FUTURE;
    });
  }

  default FunFutureMaybe<T> filter(final Predicate<? super T> filter) {
    return mapMaybe(value -> Maybe.onlyIf(filter.test(value), value));
  }

  default FunFutureMaybe<T> filter(final AsyncFunction<? super T, Boolean> filter) {
    return flatMapMaybe(
            value -> extendFuture(filter.apply(value)).map(
                    filterResult ->
                            Maybe.onlyIf(filterResult, value)));
  }

  @SuppressWarnings("unchecked")
  default <C> FunFuture<C> filter(Class<C> castClass) {
    return (FunFuture<C>) filter(Pred.instanceOf(castClass));
  }

  default <E extends Exception> FunFuture<T> recover(Class<E> exceptionType, ThrowingFunction<? super E, ? extends T> alternateResultSource) {
    return recover(Pred.instanceOf(exceptionType), (ThrowingFunction<Exception, ? extends T>) alternateResultSource);
  }

  default FunFuture<T> recover(Predicate<? super Exception> exceptionFilter, ThrowingFunction<? super Exception, ? extends T> alternateResultSource) {
    return recoverWith(
            throwable -> exceptionFilter.test(throwable)
                    ? Futures.immediateFuture(alternateResultSource.apply(throwable))
                    : Futures.immediateFailedFuture(throwable));
  }

  default FunFuture<T> recoverWith(final AsyncFunction<? super Exception, ? extends T> exceptionHandler) {
    return newCompletionPromise().completeOrRecoverWith(this, exceptionHandler);
  }

  default <E extends Exception> FunFuture<T> recoverWith(Class<E> exceptionType, AsyncFunction<? super E, ? extends T> alternateResultSource) {
    return recoverWith(
            Pred.instanceOf(exceptionType),
            (AsyncFunction<? super Exception, ? extends T>) alternateResultSource);
  }

  default FunFuture<T> recoverWith(Predicate<? super Exception> exceptionFilter, AsyncFunction<? super Exception, ? extends T> alternateResultSource) {
    return recoverWith(throwable -> exceptionFilter.test(throwable) ? (ListenableFuture<T>)alternateResultSource.apply(throwable) : Futures.immediateFailedFuture(throwable));
  }

  default void addSameThreadListener(Runnable runnable) {
    addListener(runnable, MoreExecutors.directExecutor());
  }

  default FunFuture<T> uponCompletion2(Consumer<? super T> successObserver, Consumer<? super Exception> errorObserver) {
    return uponCompletion(new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        successObserver.accept(result);
      }

      @Override
      public void onFailure(Throwable t) {
        errorObserver.accept((Exception) t);
      }
    });
  }

  default FunFuture<T> uponCompletion(Runnable sideEffect) {
    return newCompletionPromise().completeWithSideEffect(this, sideEffect);
  }

  default Promise<T> newCompletionPromise() {return Promise.<T>newPromise();}

  default FunFuture<T> uponCompletion(final FutureCallback<? super T> callback) {
    return uponCompletion(() -> {
              try {
                T value;
                try {
                  value = Uninterruptibles.getUninterruptibly(this);
                } catch (Throwable e) {
                  callback.onFailure(unwrapExecutionException(e));
                  return;
                }
                callback.onSuccess(value);
              } catch (Throwable e) {
                SideEffect.handleUncaughtException(e);
              }
            });
  }

  default FunFuture<T> uponSuccess(final Consumer<? super T> successObserver) {
    return uponCompletion(new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        successObserver.accept(result);
      }

      @Override
      public void onFailure(Throwable t) {
      }
    });
  }

  default FunFuture<T> uponFailure(Consumer<? super Throwable> failureObserver) {
    return uponCompletion(new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
      }

      @Override
      public void onFailure(Throwable t) {
        failureObserver.accept(t);
      }
    });
  }

  default <V> FunFuturePair<T, V> zip(ListenableFuture<V> other) {
    return FunFuture.allAsList(ImmutableList.<ListenableFuture<? extends Object>>of(this, other))
                    .mapPair(
                            (List<Object> input) ->
                                    Pair.of(get(), other.get()));
  }

  default <U, V> FunFuture<V> zipWith(ListenableFuture<U> other, ThrowingBiFunction<? super T, ? super U, ? extends ListenableFuture<V>> zipper) {
    return FunFuture.allAsList(ImmutableList.of(this, other))
                    .thenAsync(() -> zipper.apply(get(), other.get()));
  }

  default Source<T> asSource() {
    return FunFuture.<T>getFromFuture().bind(this);
  }

  public static Exception unwrapExecutionException(Throwable e) {
    Throwable cause;
    if (e instanceof ExecutionException || e instanceof UncheckedExecutionException) {
      cause = MoreObjects.firstNonNull(e.getCause(), e);
    } else {
      cause = e;
    }
    return FatalErrorHandler.castOrDie(cause);
  }

  public static class ForwardingFunFuture<T> extends ForwardingListenableFuture<T> implements FunFuture<T> {
    private ListenableFuture<T> delegate;

    public ForwardingFunFuture(ListenableFuture<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public ListenableFuture<T> delegate() {
      return delegate;
    }

    @Override
    public void addListener(Runnable listener, Executor exec) {
      super.addListener(AsyncContext.snapshot().wrapRunnable(listener), exec);
    }
  }

  public static class ImmediateSuccess<T> extends ForwardingFunFuture<T> {
    public ImmediateSuccess(T value) {
      super(Futures.immediateFuture(value));
    }

    @Override public <O> FunFuture<O> map(ThrowingFunction<? super T, ? extends O> function) {
      return FunFuture.callSafely(() -> new ImmediateSuccess<>(function.apply(get())));
    }

    @Override public <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
      return FunFuture.callSafely(() -> (ListenableFuture<O>) f.apply(get()));
    }
  }

  static class FunFutureTask<T> extends ForwardingFunFuture<T> implements FunRunnableFuture<T> {
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
      ((ListenableFutureTask<T>) delegate()).run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(interruptOnCancel && mayInterruptIfRunning);
    }
  }

  static class PrioritizedFutureTask<T, P> extends FunFutureTask<T> implements Comparable<PrioritizedFutureTask<T, P>> {
    private final Comparator<? super P> comparator;
    private final P priority;

    private PrioritizedFutureTask(ListenableFutureTask<T> delegate, boolean allowCancelToInterrupt, P priority, Comparator<? super P> comparator) {
      super(delegate, allowCancelToInterrupt);
      this.comparator = comparator;
      this.priority = priority;
    }

    @Override
    public int compareTo(PrioritizedFutureTask<T, P> that) {
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
