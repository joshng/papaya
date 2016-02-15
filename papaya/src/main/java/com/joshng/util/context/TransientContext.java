package com.joshng.util.context;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.joshng.util.blocks.Sink;
import com.joshng.util.blocks.F;
import com.joshng.util.blocks.SideEffect;
import com.joshng.util.blocks.Source;
import com.joshng.util.concurrent.AsyncF;
import com.joshng.util.concurrent.FunFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * User: josh
 * Date: 2/3/12
 * Time: 12:14 AM
 */
public interface TransientContext {
  static CompositeTransientContext sequence(TransientContext first, TransientContext second, TransientContext... rest) {
    return new CompositeTransientContext(Lists.asList(first, second, rest));
  }

  public interface State {
    void exit();

    State NULL = NullState.INSTANCE;

    public enum NullState implements State {
      INSTANCE;

      public void exit() {
        // nothing
      }
    }
  }

  State enter();

  default TransientContext andThen(TransientContext innerContext) {
    return innerContext != NullContext.INSTANCE ? sequence(this, innerContext) : this;
  }

  default void runInContext(Runnable r) {
    try {
      callInContext(SideEffect.extendRunnable(r));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  default <T> T callInContext(Callable<T> callable) throws Exception {
    State state = enter();
    try {
      return callable.call();
    } finally {
      state.exit();
    }
  }

  default <T> T getInContext(Supplier<T> supplier) {
    try {
      return callInContext(Source.extendSupplier(supplier));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  default <I, O> O applyInContext(I input, Function<? super I, ? extends O> fn) {
    try {
      return callInContext(() -> fn.apply(input));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  default <I> void acceptInContext(I input, Consumer<? super I> consumer) {
    try {
      callInContext(() -> {
                consumer.accept(input);
                return null;
              });
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  default <T> FunFuture<T> callInContextAsync(Callable<? extends ListenableFuture<T>> futureBlock) {
    final State state = enter();
    try {
      return FunFuture.callSafely(futureBlock)
              .uponCompletion(state::exit);
    } catch (Exception e) {
      return FunFuture.immediateFailedFuture(e);
    }
  }

  default SideEffect wrapRunnable(Runnable block) {
    return () -> runInContext(block);
  }

  default <O> Callable<O> wrapCallable(Callable<O> block) {
    return () -> callInContext(block);
  }

  default <I, O> F<I, O> wrapFunction(Function<I, O> function) {
    return input -> applyInContext(input, function);
  }

  default <T> Sink<T> wrapSink(Consumer<T> sink) {
    return input -> acceptInContext(input, sink);
  }

  default <T> Callable<FunFuture<T>> wrapAsyncCallable(Callable<? extends ListenableFuture<T>> futureBlock) {
    return () -> callInContextAsync(futureBlock);
  }

  default <I, O> AsyncF<I, O> wrapAsync(AsyncF<I, O> asyncFunction) {
    return input -> callInContextAsync(asyncFunction.bind(input));
  }

  default <T> FunFuture<T> wrapFutureListeners(ListenableFuture<T> future) {
    return new FunFuture.ForwardingFunFuture<T>(future) {
      @Override
      public void addListener(Runnable listener, Executor exec) {
        super.addListener(wrapRunnable(listener), exec);
      }
    };
  }

  TransientContext NULL = NullContext.INSTANCE;

  enum NullContext implements TransientContext {
    INSTANCE;

    @Override
    public State enter() {
      return State.NULL;
    }

    @Override
    public TransientContext andThen(TransientContext innerContext) {
      return innerContext;
    }

    @Override
    public void runInContext(Runnable r) {
      r.run();
    }

    @Override
    public <T> T callInContext(Callable<T> callable) throws Exception {
      return callable.call();
    }

    @Override
    public <T> T getInContext(Supplier<T> supplier) {
      return supplier.get();
    }

    @Override
    public <I, O> O applyInContext(I input, Function<? super I, ? extends O> fn) {
      return fn.apply(input);
    }

    @Override
    public <I> void acceptInContext(I input, Consumer<? super I> consumer) {
      consumer.accept(input);
    }

    @Override
    public <T> FunFuture<T> callInContextAsync(Callable<? extends ListenableFuture<T>> futureBlock) {
      return FunFuture.callSafely(futureBlock);
    }

    @Override
    public SideEffect wrapRunnable(Runnable block) {
      return SideEffect.extendRunnable(block);
    }

    @Override
    public <O> Callable<O> wrapCallable(Callable<O> block) {
      return block;
    }

    @Override
    public <I, O> F<I, O> wrapFunction(Function<I, O> function) {
      return F.extendFunction(function);
    }

    @Override
    public <T> Sink<T> wrapSink(Consumer<T> sink) {
      return Sink.extendConsumer(sink);
    }

    @Override
    public <I, O> AsyncF<I, O> wrapAsync(AsyncF<I, O> asyncFunction) {
      return asyncFunction;
    }

    @Override
    public <T> FunFuture<T> wrapFutureListeners(ListenableFuture<T> future) {
      return FunFuture.extendFuture(future);
    }
  }
}
