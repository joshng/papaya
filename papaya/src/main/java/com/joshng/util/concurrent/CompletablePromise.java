package com.joshng.util.concurrent;

import com.joshng.util.blocks.ThrowingConsumer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A simple extension of {@link CompletableFuture} with utility methods for completing a "CompletablePromise" in various ways.
 */
public class CompletablePromise<T> extends CompletableFuture<T> implements BiConsumer<T, Throwable> {
    /**
     * Encapsulates the common pattern of allocating a Future, performing some initialization to prepare for its
     * asynchronous completion, and then returning that Future. For example, the following common pattern:
     * <pre>{@code
     * CompletableFuture<X> future = new CompletableFuture<X>();
     * remoteService.submitWithCallback(args, (result, error) -> {
     *   if (error == null) {
     *     future.complete(result));
     *   } else {
     *       future.completeExceptionally(error);
     *   }
     * });
     * return future;
     * }</pre>
     *
     * ... can be more concise:
     *
     * <pre>{@code
     * return CompletablePromise.thatCompletes(completablePromise -> remoteService.submitWithCallback(args, completablePromise::apply));
     * }</pre>
     *
     */
    public static <T> CompletablePromise<T> thatCompletes(ThrowingConsumer<? super CompletablePromise<T>> initializer) {
        CompletablePromise<T> completablePromise = new CompletablePromise<>();
        try {
            initializer.accept(completablePromise);
        } catch (Throwable e) {
            completablePromise.completeExceptionally(e);
        }
        return completablePromise;
    }

    public static <T> CompletablePromise<T> completeAsync(Callable<? extends CompletionStage<? extends T>> completionSupplier) {
        return completeAsync(completionSupplier, ForkJoinPool.commonPool());
    }

    public static <T> CompletablePromise<T> completeAsync(Callable<? extends CompletionStage<? extends T>> completionSupplier, Executor executor) {
        return CompletablePromise.thatCompletes(completablePromise -> CompletableFuture.runAsync(() -> completablePromise.tryCompleteWith(completionSupplier), executor));
    }

    public static <T> CompletablePromise<T> callAsync(Callable<? extends T> callable, Executor executor) {
        return completeAsync(() -> CompletableFuture.completedFuture(callable.call()), executor);
    }

    /**
     * Complete this Future with the result of calling the given {@link Callable}, or with any exception that it throws.
     */
    public CompletablePromise<T> tryComplete(Callable<T> completion) {
        try {
            complete(completion.call());
        } catch (Throwable e) {
            completeExceptionally(e);
        }
        return this;
    }

    /**
     * Complete this Future with the result of the Future returned by the given {@link Callable} (or any exception that it throws).
     */
    public CompletablePromise<T> tryCompleteWith(Callable<? extends CompletionStage<? extends T>> completion) {
        try {
            completeWith(completion.call().toCompletableFuture());
        } catch (Throwable e) {
            completeExceptionally(e);
        }
        return this;
    }

    public CompletablePromise<T> fulfill(T result) {
        complete(result);
        return this;
    }

    /**
     * Complete this Future with the same result as the given {@link CompletableFuture completion}.
     */
    public <V extends T> CompletableFuture<V> completeWith(CompletionStage<V> completion) {
        return completion.whenComplete(this).toCompletableFuture();
    }

//    public CompletablePromise<T> withTimeout(Duration timeout, Scheduler scheduler) {
//        return FutureUtils.withTimeout(this, timeout, scheduler);
//    }

    public CompletableFuture<T> uponCompletion(Runnable sideEffect) {
        return whenComplete((t, e) -> sideEffect.run());
    }

    public <U> CompletableFuture<U> thenReplace(Supplier<U> supplier) {
        return thenApply(__ -> supplier.get());
    }

    public <U> CompletableFuture<U> thenReplaceFuture(Supplier<? extends CompletionStage<U>> supplier) {
        return thenCompose(__ -> supplier.get());
    }

    public <U> CompletablePromise<U> thenApply(Function<? super T, ? extends U> fn) {
        return CompletablePromise.thatCompletes(completablePromise -> completablePromise.completeWith(super.thenApply(fn)));
    }

    @Override
    public void accept(T t, Throwable throwable) {
        if (throwable == null) {
            complete(t);
        } else {
            completeExceptionally(throwable);
        }
    }

}
