package joshng.util.concurrent;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Maybe;
import joshng.util.collect.Pair;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: 9/3/13
 * Time: 12:09 PM
 */
public abstract class AbstractFunFuture<T> extends AbstractFuture<T> implements FunFuture<T> {
    @Override
    public <O> FunFuture<O> map(Executor executor, Function<? super T, ? extends O> f) {
        return FunFutures.map(this, executor, f);
    }

    @Override
    public <O> FunFuture<O> flatMap(Executor executor, AsyncFunction<? super T, ? extends O> f) {
        return FunFutures.flatMap(this, executor, f);
    }

    @Override
    public T getUnchecked() {
        return FunFutures.getUnchecked(this);
    }

    @Override
    public Maybe<T> getWithin(long timeout, TimeUnit timeUnit) {
        return FunFutures.getUnchecked(this, timeout, timeUnit);
    }

    @Override
    public <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E {
        return FunFutures.getChecked(this, exceptionClass);
    }

    @Override
    public <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E {
        return FunFutures.getChecked(this, timeout, timeUnit, exceptionClass);
    }

    @Override
    public <O> FunFuture<O> map(Function<? super T, ? extends O> f) {
        return FunFutures.map(this, f);
    }

    @Override
    public <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f) {
        return FunFutures.flatMap(this, f);
    }

    @Override
    public FunFuture<T> filter(Predicate<? super T> filter) {
        return FunFutures.filter(this, filter);
    }

    @Override
    public FunFuture<T> filter(AsyncFunction<? super T, Boolean> filter) {
        return FunFutures.filter(this, filter);
    }

    @Override
    public FunFuture<T> recover(ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recover(this, exceptionHandler);
    }

    @Override
    public FunFuture<T> recover(Executor executor, ThrowingFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recover(this, executor, exceptionHandler);
    }

    @Override
    public FunFuture<T> recoverWith(AsyncFunction<? super Throwable, ? extends T> exceptionHandler) {
        return FunFutures.recoverWith(this, exceptionHandler);
    }

    @Override
    public <C> FunFuture<C> filter(Class<C> castClass) {
        return FunFutures.filter(this, castClass);
    }

    @Override
    public FunFuture<T> uponCompletion(Runnable runnable) {
        return FunFutures.uponCompletion(this, MoreExecutors.sameThreadExecutor(), runnable);
    }

    @Override
    public FunFuture<T> uponCompletion(Executor executor, Runnable runnable) {
        return FunFutures.uponCompletion(this, executor, runnable);
    }

    @Override
    public <U> FunFuture<Pair<T, U>> zip(ListenableFuture<U> that) {
        return FunFutures.zip(this, that);
    }
}
