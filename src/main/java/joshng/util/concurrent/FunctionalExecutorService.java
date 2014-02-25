package joshng.util.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import joshng.util.blocks.Consumer;
import joshng.util.blocks.F;
import joshng.util.blocks.SideEffect;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;
import joshng.util.collect.Functional;
import joshng.util.collect.Nothing;

import java.util.concurrent.Callable;

import static joshng.util.concurrent.FunFutures.extendFuture;

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

    FunctionalExecutorService(ListeningExecutorService delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    public <T> AsyncF<Callable<T>, T> submitter() {
        return submitter;
    }

    public AsyncF<Iterable<Runnable>, Long> batchSubmitter() {
        return new AsyncF<Iterable<Runnable>, Long>() {
            @Override
            public FunFuture<Long> applyAsync(Iterable<Runnable> input) {
                return submitAll(input);
            }
        };
    }

    public <T> Source<FunFuture<T>> wrapSource(Supplier<T> supplier) {
        return this.<T>submitter().bind(Source.extendSupplier(supplier));
    }

    public <I, O> AsyncF<I, O> wrapFunction(Function<I, O> function) {
        return AsyncF.extendAsyncFunction(F.extendF(function).binder().andThen(this.<O>submitter()));
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
        return wrapFunction(Sink.extendHandler(sink));
    }

    /**
     * Given an {@code itemProcessor}, returns a function that submits a stream of inputs for processing by the
     * itemProcessor within this executor, and yields a Future of the number of jobs eventually completed.<p>
     *
     * This is similar to {@link FunFutures#allAsList}, but more applicable when the number of jobs may be large,
     * and it is undesirable to retain futures for each individual result. With this variant, only the completion
     * of the entire batch is visible.<p>
     *
     * IMPORTANT: Note that this process ignores exceptions thrown by individual tasks: no {@link java.util.concurrent.ExecutionException}
     * will ever be thrown from the returned Future if a task fails. This is because retaining a very large number of
     * exceptions could be problematic.<p>
     *
     * Exception-handling should thus be performed within the itemProcessor implementation.
     * @see FunFutures#allAsList(Iterable)
     * @see
     * @param itemProcessor a routine to run in parallel for each input
     * @return a Future containing the number of items eventually submitted (both succeeded and failed)
     */
    public <T> AsyncF<Iterable<? extends T>, Long> createBatchProcessor(Consumer<T> itemProcessor) {
        return Functional.<T, Runnable>mapper(Sink.extendHandler(itemProcessor).binder().andThen(Source.AS_RUNNABLE)).andThenAsync(batchSubmitter());
    }

    public <I, O> AsyncF<ListenableFuture<? extends I>, O> mapper(final Function<? super I, ? extends O> mapper) {
        return new AsyncF<ListenableFuture<? extends I>, O>() {
            @Override
            public FunFuture<O> applyAsync(ListenableFuture<? extends I> input) {
                return extendFuture(Futures.transform(input, mapper, getDelegate()));
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

    /**
     * Submits all jobs produced by the Iterable, and returns a future of the number of jobs completed.
     *
     *
     * @param jobs the sequence of jobs to be submitted
     * @returns a future that is done when all of the jobs have completed
     */
    public FunFuture<Long> submitAll(Iterable<Runnable> jobs) {
        return new FutureCompletionTracker().submitAll(SideEffect.RUNNABLE_WRAPPER.andThenAsync(this.<Nothing>submitter()).transform(jobs)).setNoMoreJobs();
    }

    @Override
    protected ListeningExecutorService delegate() {
        return getDelegate();
    }

    protected ListeningExecutorService getDelegate() {
        return delegate;
    }
}
