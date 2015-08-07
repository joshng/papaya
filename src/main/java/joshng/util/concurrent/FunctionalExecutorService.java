package joshng.util.concurrent;

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Nothing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static joshng.util.concurrent.FunFuture.extendFuture;

/**
 * User: josh
 * Date: 12/7/12
 * Time: 10:51 AM
 */
public class FunctionalExecutorService extends ForwardingListeningExecutorService {
  private final ListeningExecutorService delegate;
  private final AsyncF submitter = new AsyncF<Callable<Object>, Object>() {
    @Override
    public FunFuture<Object> applyAsync(Callable<Object> input) {
      return extendFuture(getDelegate().submit(input));
    }
  };

  public static FunctionalExecutorService functionalDecorator(ExecutorService executorService) {
    return executorService instanceof FunctionalExecutorService
            ? (FunctionalExecutorService) executorService
            : new FunctionalExecutorService(MoreExecutors.listeningDecorator(executorService));
  }

  FunctionalExecutorService(ListeningExecutorService delegate) {
    this.delegate = delegate;
  }

  @SuppressWarnings("unchecked")
  public <T> AsyncF<Callable<T>, T> submitter() {
    return submitter;
  }

  public <T> Callable<FunFuture<T>> wrapCallable(Callable<T> callable) {
    return () -> submit(callable);
  }

  public <T> Source<FunFuture<T>> wrapSource(Supplier<T> supplier) {
    return this.<T>submitter().bind(supplier::get);
  }

  public <I, O> AsyncF<I, O> wrapFunction(ThrowingFunction<I, O> function) {
    return input -> submit(() -> function.apply(input));
  }

  public <I, O> AsyncF<I, O> wrapAsync(final AsyncF<I, O> function) {
    return new AsyncF<I, O>() {
      @Override
      public FunFuture<O> applyAsync(I input) {
        return FunFuture.dereference(submit(function.bind(input)));
      }
    };
  }

  public <T> Callable<FunFuture<T>> wrapAsyncCallable(Callable<? extends ListenableFuture<T>> callable) {
    return () -> submitAsync(callable);
  }

  public <T> AsyncF<T, Nothing> wrapSink(Consumer<T> sink) {
    return wrapFunction(Sink.extendConsumer(sink));
  }

  public <T> FunFuture<T> submitAsync(Callable<? extends ListenableFuture<T>> asyncCallable) {
    return FunFuture.<T>dereference(submit(asyncCallable));
  }

  @Override
  public <T> FunFuture<T> submit(Callable<T> task) {
    return FunFuture.newFuture(super.submit(task));
  }

  @Override
  public FunFuture<?> submit(Runnable task) {
    return FunFuture.newFuture(super.submit(task));
  }

  @Override
  public <T> FunFuture<T> submit(Runnable task, T result) {
    return FunFuture.newFuture(super.submit(task, result));
  }

  @Override
  protected ListeningExecutorService delegate() {
    return getDelegate();
  }

  protected ListeningExecutorService getDelegate() {
    return delegate;
  }
}
