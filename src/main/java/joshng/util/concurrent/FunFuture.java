package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Maybe;
import joshng.util.collect.Pair;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: 12/8/12
 * Time: 1:03 PM
 */
public interface FunFuture<T> extends ListenableFuture<T>, Cancellable {
    T getUnchecked();
    Maybe<T> getWithin(long timeout, TimeUnit timeUnit);
    <E extends Throwable> T getChecked(Class<E> exceptionClass) throws E;
    <E extends Throwable> Maybe<T> getCheckedWithin(long timeout, TimeUnit timeUnit, Class<E> exceptionClass) throws E;
    <O> FunFuture<O> map(Function<? super T, ? extends O> f);
    <O> FunFuture<O> map(Executor executor, Function<? super T, ? extends O> f);
    <O> FunFuture<O> flatMap(AsyncFunction<? super T, ? extends O> f);
    <O> FunFuture<O> flatMap(Executor executor, AsyncFunction<? super T, ? extends O> f);
    FunFuture<T> filter(Predicate<? super T> filter);
    FunFuture<T> filter(AsyncFunction<? super T, Boolean> filter);
    <C> FunFuture<C> filter(Class<C> castClass);
    <U> FunFuture<Pair<T, U>> zip(ListenableFuture<U> that);
    FunFuture<T> recover(ThrowingFunction<? super Throwable, ? extends T> exceptionHandler);
    FunFuture<T> recover(Executor executor, ThrowingFunction<? super Throwable, ? extends T> exceptionHandler);
    FunFuture<T> recoverWith(AsyncFunction<? super Throwable, ? extends T> exceptionHandler);
    FunFuture<T> uponCompletion(Runnable runnable);
    FunFuture<T> uponCompletion(Executor executor, Runnable runnable);
    FunFuture<T> uponCompletion(final FutureCallback<? super T> callback);
    FunFuture<T> uponCompletion(Executor executor, final FutureCallback<? super T> callback);
    FunFuture<T> uponSuccess(Consumer<? super T> successObserver);
    FunFuture<T> uponSuccess(Executor executor, Consumer<? super T> successObserver);
    FunFuture<T> uponFailure(Consumer<? super Throwable> errorObserver);
    FunFuture<T> uponFailure(Executor executor, Consumer<? super Throwable> errorObserver);
}
