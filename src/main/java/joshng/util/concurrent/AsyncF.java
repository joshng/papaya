package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Reflect;
import joshng.util.blocks.F;
import joshng.util.collect.Nothing;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.concurrent.FunFuture.extendFuture;

/**
 * User: josh
 * Date: 12/7/12
 * Time: 11:38 AM
 */
public interface AsyncF<I, O> extends F<I, FunFuture<O>>, IAsyncFunction<I, O> {
  static final AsyncF IDENTITY = (AsyncF<ListenableFuture<Object>, Object>) FunFuture::extendFuture;

  @SuppressWarnings("unchecked")
  public static <T> AsyncF<ListenableFuture<? extends T>, T> asyncIdentity() {
    return IDENTITY;
  }

  public static <I, O> AsyncF<I, O> extendAsyncFunction(final Function<? super I, ? extends ListenableFuture<O>> asyncFunction) {
    if (asyncFunction instanceof AsyncF) return Reflect.blindCast(asyncFunction);
    return new AsyncF<I, O>() {
      public FunFuture<O> applyAsync(I input) {
        return extendFuture(asyncFunction.apply(input));
      }
    };
  }

  public static <I,O> AsyncF<I, O> liftFunction(Function<I, O> function) {
    return input -> FunFuture.immediateFuture(function.apply(input));
  }

  public static <I, O> AsyncF<I, O> asyncF(final AsyncFunction<? super I, O> function) {
    if (function instanceof AsyncF) return Reflect.blindCast(function);
    return new AsyncF<I, O>() {
      public FunFuture<O> applyAsync(I input) throws Exception {
        return extendFuture(function.apply(input));
      }
    };
  }

  public static <T> AsyncF<T, Nothing> asyncConsumer(Consumer<T> consumer) {
    return input -> {
      consumer.accept(input);
      return Nothing.FUTURE;
    };
  }

  default AsyncF<Object, O> bindAsync(I input) {
    return extendAsyncFunction(bind(input));
  }

  @Nonnull
  FunFuture<O> applyAsync(I input) throws Throwable;

  @Override
  @Nonnull
  default FunFuture<O> apply(I input) {
    try {
      return checkNotNull(applyAsync(input), "AsyncFunction must not return null!", this);
    } catch (Throwable e) {
      return FunFuture.immediateFailedFuture(e);
    }
  }
}
