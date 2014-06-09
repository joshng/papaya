package joshng.util.concurrent;

import com.google.common.util.concurrent.*;
import joshng.util.blocks.F;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;
import joshng.util.collect.Nothing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
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

  public <T> Source<FunFuture<T>> wrapSource(Supplier<T> supplier) {
    return this.<T>submitter().bind(supplier::get);
  }

  public <I, O> AsyncF<I, O> wrapFunction(Function<I, O> function) {
    return AsyncF.extendAsyncFunction(F.extendFunction(function).binder().andThen(this.<O>submitter()));
  }

  public <I, O> AsyncF<I, O> wrapAsync(final AsyncF<? super I, ? extends O> function) {
    return new AsyncF<I, O>() {
      @Override
      public FunFuture<O> applyAsync(I input) {
        return submit(function.bind(input)).flatMap(AsyncF.<O>asyncIdentity());
      }
    };
  }

  public <T> AsyncF<T, Nothing> wrapSink(Consumer<T> sink) {
    return wrapFunction(Sink.extendConsumer(sink));
  }

  public <I, O> AsyncF<ListenableFuture<? extends I>, O> mapper(final Function<? super I, ? extends O> mapper) {
    com.google.common.base.Function<I, O> f = mapper::apply;
    return new AsyncF<ListenableFuture<? extends I>, O>() {
      @Override
      public FunFuture<O> applyAsync(ListenableFuture<? extends I> input) {
        return extendFuture(Futures.transform(input, f, getDelegate()));
      }
    };
  }

  public <I, O> AsyncF<ListenableFuture<? extends I>, O> flatMapper(final AsyncFunction<? super I, ? extends O> mapper) {
    return new AsyncF<ListenableFuture<? extends I>, O>() {
      @Override
      public FunFuture<O> applyAsync(ListenableFuture<? extends I> input) {
        return extendFuture(Futures.transform(input, mapper, getDelegate()));
      }
    };
  }

  public <T> FunFuture<T> submitAsync(Callable<? extends ListenableFuture<T>> asyncCallable) {
    return extendFuture(Futures.transform(submit(asyncCallable), (AsyncFunction<ListenableFuture<? extends T>, T>) AsyncF.<T>asyncIdentity()));
  }

  @Override
  public <T> FunFuture<T> submit(Callable<T> task) {
    return extendFuture(super.submit(task));
  }

  @Override
  public FunFuture<?> submit(Runnable task) {
    return extendFuture(super.submit(task));
  }

  @Override
  public <T> FunFuture<T> submit(Runnable task, T result) {
    return extendFuture(super.submit(task, result));
  }

  @Override
  protected ListeningExecutorService delegate() {
    return getDelegate();
  }

  protected ListeningExecutorService getDelegate() {
    return delegate;
  }
}
