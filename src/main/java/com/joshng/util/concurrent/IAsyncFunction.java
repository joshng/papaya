package com.joshng.util.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.joshng.util.blocks.ThrowingFunction;

/**
 * User: josh
 * Date: 5/16/13
 * Time: 6:06 PM
 */
public interface IAsyncFunction<I, O> extends Function<I, FunFuture<O>>, ThrowingFunction<I, FunFuture<O>>, AsyncFunction<I, O> {
  @Override
  FunFuture<O> apply(I input);
}
