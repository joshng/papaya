package joshng.util.context;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.F;
import joshng.util.blocks.SideEffect;
import joshng.util.blocks.Sink;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.FunFuture;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * User: josh
 * Date: 2/3/12
 * Time: 12:14 AM
 */
public interface TransientContext {
  static CompositeStackContext of(TransientContext first, TransientContext second, TransientContext... rest) {
    return new CompositeStackContext(Lists.asList(first, second, rest));
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
  
  default void runInContext(Runnable r) {
    State state = enter();
    try {
      r.run();
    } finally {
      state.exit();
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
    State state = enter();
    try {
      return supplier.get();
    } finally {
      state.exit();
    }
  }

  default <I, O> O applyInContext(I input, Function<? super I, ? extends O> fn) {
    State state = enter();
    try {
      return fn.apply(input);
    } finally {
      state.exit();
    }
  }

  default <I> void acceptInContext(I input, Consumer<? super I> consumer) {
    State state = enter();
    try {
      consumer.accept(input);
    } finally {
      state.exit();
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

  default <I, O> AsyncF<I, O> wrapAsync(AsyncF<I, O> asyncFunction) {
    return input -> callInContextAsync(asyncFunction.bind(input));
  }
}
