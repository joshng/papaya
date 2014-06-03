package joshng.util.concurrent;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import joshng.util.blocks.*;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Maybe;
import joshng.util.collect.Nothing;
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
public interface FunFuture<T> extends ListenableFuture<T>, Cancellable {
  static final Logger LOG = LoggerFactory.getLogger(FunFuture.class);
  static final FunFuture NULL_FUTURE = FunFuture.<Object>immediateFuture(null);

  static final AsyncF SEQUENCER = (AsyncF<Iterable<ListenableFuture<Object>>, List<Object>>) FunFuture::allAsList;
  static F GET_UNCHECKED = (F<Future<Object>, Object>) FunFuture::getUnchecked;
  FunFuture<Boolean> FALSE = FunFuture.immediateFuture(false);
  FunFuture<Boolean> TRUE = FunFuture.immediateFuture(true);

  ListenableFuture<T> delegate();

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
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
      }
    };
  }

  public static <T> FunFuture<T> any(final Iterable<? extends ListenableFuture<? extends T>> inputs) {
    final Promise<T> promise = Promise.newPromise();
    promise.attachCancellableCompletion(new Cancellable() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
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
  static <T> Iterable<? extends ListenableFuture<? extends T>> unwrapFutureIterable(Iterable<? extends ListenableFuture<? extends T>> input) {
    // Guava copies to an immutable list; we may already have one wrapped in a FunList, so extract it here
    return input instanceof FunIterable ? ((FunIterable<ListenableFuture<T>>) input).toList().delegate() : input;
  }

  static final F EXTENDER = (F<ListenableFuture, FunFuture>) FunFuture::extendFuture;

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
      Throwable cause = e.getCause();
      if (cause instanceof UncheckedExecutionException) throw (UncheckedExecutionException) cause;
      throw new UncheckedExecutionException(cause);
    }
  }

  default T getUnchecked() {
    return FunFuture.getUnchecked(delegate());
  }

  default Maybe<T> getWithin(long timeout, TimeUnit timeUnit) {
    try {
      return definitely(get(timeout, timeUnit));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new UncheckedExecutionException(e.getCause());
    } catch (TimeoutException e) {
      return Maybe.not();
    }
  }

  default <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E {
    try {
      return get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, exceptionClass);
      throw new RuntimeException(cause);
    }
  }

  default <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
    try {
      return definitely(get(timeout, timeUnit));
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
      return FunFuture.immediateFuture(block.call());
    } catch (Throwable e) {
      return FunFuture.immediateFailedFuture(e);
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
      handleUncaughtException(e);
    }
  }

  public static void handleUncaughtException(Throwable e) {
    Thread currentThread = Thread.currentThread();
    Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
    if (exceptionHandler != null) {
      exceptionHandler.uncaughtException(currentThread, e);
    } else {
      LOG.warn("Uncaught exception in thread {}:", currentThread, e);
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
  public static <T> AsyncF<Iterable<? extends ListenableFuture<T>>, List<T>> sequencer() {
    return SEQUENCER;
  }

  @SuppressWarnings("unchecked")
  public static <T> FunFuture<T> nullFuture() {
    return NULL_FUTURE;
  }

  public static <T> FunFuture<T> extendFuture(ListenableFuture<T> future) {
    if (future instanceof FunFuture) return (FunFuture<T>) future;
    return new ForwardingFunFuture<>(future);
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

  @SuppressWarnings("unchecked")
  public static <T> F<ListenableFuture<T>, FunFuture<T>> extender() {
    return EXTENDER;
  }

  default <O> ForwardingFunFuture<O> wrapFuture(ListenableFuture<O> future) {
    return new ForwardingFunFuture<>(future);
  }

  default <O> FunFuture<O> map(Function<? super T, ? extends O> function) {
    return map(MoreExecutors.sameThreadExecutor(), function);
  }

  default <O> FunFuture<O> map(Executor executor, Function<? super T, ? extends O> function) {
    F<? super T, ? extends O> f = F.extendF(function);
    return wrapFuture(Futures.transform(delegate(), f, executor));
  }


  public static <I, O> AsyncF<ListenableFuture<? extends I>, O> mapper(final Function<? super I, ? extends O> mapper) {
    return input -> extendFuture(input).map(mapper);
  }

  default <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
    return flatMap(MoreExecutors.sameThreadExecutor(), f);
  }

  default <O> FunFuture<O> flatMap(Executor executor, AsyncFunction<? super T, ? extends O> f) {
    return wrapFuture(Futures.transform(delegate(), f, executor));
  }

  default <O> FunFutureMaybe<O> mapMaybe(Function<? super T, Maybe<O>> f) {
    return mapMaybe(MoreExecutors.sameThreadExecutor(), f);
  }

  default <O> FunFutureMaybe<O> flatMapMaybe(AsyncFunction<? super T, Maybe<O>> f) {
    return FunFutureMaybe.wrapFutureMaybe(Futures.transform(delegate(), f));
  }

  default <O> FunFutureMaybe<O> mapMaybe(Executor executor, Function<? super T, Maybe<O>> f) {
    return FunFutureMaybe.wrapFutureMaybe(Futures.transform(delegate(), F.extendF(f), executor));
  }

  static <I, O> F<ListenableFuture<? extends I>, FunFutureMaybe<O>> maybeMapper(Function<? super I, Maybe<O>> maybeFunction) {
    return future -> extendFuture(future).mapMaybe(maybeFunction);
  }

  default <O> FunFutureMaybe<O> flatMapMaybe(Executor executor, AsyncFunction<? super T, Maybe<O>> f) {
    return new ForwardingFunFutureMaybe<>(Futures.transform(delegate(), f, executor));
  }

  default FunFuture<Nothing> foreach(Sink<? super T> sideEffect) {
    return map(sideEffect);
  }

  default FunFuture<T> filter(final Predicate<? super T> filter) {
    return map(new Tapper<T>() {
      public void tap(T value) {
        if (!filter.test(value)) throw new FilteredFutureException();
      }
    });
  }

  default FunFuture<T> filter(final AsyncFunction<? super T, Boolean> filter) {
    return flatMap(value -> extendFuture(filter.apply(value)).map(input -> {
      if (!input) throw new FilteredFutureException();
      return value;
    }));
  }

  @SuppressWarnings("unchecked")
  default <C> FunFuture<C> filter(Class<C> castClass) {
    return (FunFuture<C>) filter(Pred.instanceOf(castClass));
  }

  default FunFuture<T> recover(final ThrowingFunction<? super Exception, ? extends T> exceptionHandler) {
    return recover(MoreExecutors.sameThreadExecutor(), exceptionHandler);
  }

  default FunFuture<T> recover(final Executor executor, final ThrowingFunction<? super Exception, ? extends T> exceptionHandler) {
    return extendFuture(Futures.withFallback(delegate(), t -> Futures.immediateFuture(exceptionHandler.apply((Exception)unwrapExecutionException(t))), executor));
  }

  default FunFuture<T> recoverWith(final AsyncFunction<? super Throwable, ? extends T> exceptionHandler) {
    return extendFuture(Futures.withFallback(delegate(), new FutureFallback<T>() {
      @SuppressWarnings("unchecked")
      @Override
      public ListenableFuture<T> create(Throwable t) throws Exception {
        return (ListenableFuture<T>) exceptionHandler.apply(unwrapExecutionException(t));
      }
    }));
  }

  default FunFuture<T> uponCompletion(Runnable runnable) {
    return uponCompletion(MoreExecutors.sameThreadExecutor(), runnable);
  }

  default FunFuture<T> uponCompletion(final FutureCallback<? super T> callback) {
    return uponCompletion(MoreExecutors.sameThreadExecutor(), callback);
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

  default FunFuture<T> uponSuccess(Consumer<? super T> successObserver) {
    return uponSuccess(MoreExecutors.sameThreadExecutor(), successObserver);
  }

  default FunFuture<T> uponFailure(Consumer<? super Exception> errorObserver) {
    return uponFailure(MoreExecutors.sameThreadExecutor(), errorObserver);
  }

  default FunFuture<T> uponCompletion(Executor executor, final Runnable sideEffect) {
    final Promise<T> promise = Promise.newPromise();
    ListenableFuture<T> future = delegate();
    promise.attachFutureCompletion(future);
    future.addListener(() -> {
      try {
        runSafely(sideEffect);
      } finally {
        promise.completeWith(future);
      }
    }, executor);
    return promise;
  }

  default FunFuture<T> uponCompletion(Executor executor, final FutureCallback<? super T> callback) {
    return uponCompletion(executor, new Runnable() {
      @Override
      public void run() {
        try {
          callback.onSuccess(Uninterruptibles.getUninterruptibly(delegate()));
        } catch (ExecutionException e) {
          callback.onFailure(e.getCause());
        }
      }
    });
  }

  default FunFuture<T> uponSuccess(Executor executor, final Consumer<? super T> successObserver) {
    return uponCompletion(executor, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        successObserver.accept(result);
      }

      @Override
      public void onFailure(Throwable t) {
      }
    });
  }

  default FunFuture<T> uponFailure(Executor executor, final Consumer<? super Exception> failureObserver) {
    return uponCompletion(executor, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
      }

      @Override
      public void onFailure(Throwable t) {
        failureObserver.accept((Exception)t);
      }
    });
  }

  public static <A, B> FunFuture<Pair<A, B>> zip(final ListenableFuture<A> a, final ListenableFuture<B> b) {
    return extendFuture(Futures.transform(Futures.allAsList(ImmutableList.of(a, b)), new F<List<Object>, Pair<A, B>>() {
      @Override
      public Pair<A, B> apply(List<Object> input) {
        return Pair.of(FunFuture.getUnchecked(a), FunFuture.getUnchecked(b));
      }
    }));
  }

  default <B> FunFuture<Pair<T, B>> zip(ListenableFuture<B> other) {
    return FunFuture.zip(delegate(), other);
  }

  public static <K, V> Pair<FunFuture<K>, FunFuture<V>> unzip(ListenableFuture<? extends Map.Entry<? extends K, V>> futureOfPair) {
    FunFuture<? extends Map.Entry<? extends K, V>> f = extendFuture(futureOfPair);
    return Pair.of(f.map(Pair.<K>getFirstFromPair()), f.map(Pair.<V>getSecondFromPair()));
  }

  default Source<T> asSource() {
    return FunFuture.<T>getFromFuture().bind(delegate());
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

  static class ForwardingFunFuture<T> extends ForwardingListenableFuture<T> implements FunFuture<T> {
    private ListenableFuture<T> delegate;

    protected ForwardingFunFuture(ListenableFuture<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public ListenableFuture<T> delegate() {
      return delegate;
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
