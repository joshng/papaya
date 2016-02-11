package com.joshng.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.joshng.util.collect.Maybe;
import com.joshng.util.Reflect;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 11:17 PM
 */
public interface AsyncMaybeF<I, O> extends AsyncF<I, Maybe<O>> {
  @SuppressWarnings("unchecked")
  static <I,O> AsyncMaybeF<I, O> extendMaybeF(Function<? super I, ? extends ListenableFuture<Maybe<O>>> f) {
    if (f instanceof AsyncMaybeF) return Reflect.blindCast(f);
    return new AsyncMaybeF<I, O>() {
      @Nonnull
      @Override
      public FunFutureMaybe<O> applyAsync(I input) throws Throwable {
        return new ForwardingFunFutureMaybe<>(f.apply(input));
      }
    };
  }

  @Nonnull
  @Override
  FunFutureMaybe<O> applyAsync(I input) throws Throwable;

  @Nonnull
  @Override
  default FunFutureMaybe<O> apply(I input) {
    return (FunFutureMaybe<O>) AsyncF.super.apply(input);
  }
}
