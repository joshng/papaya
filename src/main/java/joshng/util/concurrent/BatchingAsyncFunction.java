package joshng.util.concurrent;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.Sink2;
import joshng.util.blocks.Source;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static joshng.util.collect.Functional.extend;
import static joshng.util.concurrent.Promise.newPromise;

/**
 * User: josh
 * Date: 9/21/13
 * Time: 6:02 PM
 */
public class BatchingAsyncFunction<I, O> implements AsyncF<I, O> {
    private static final Sink2 COMPLETE_PROMISE = new Sink2<Job<Object,Object>, ListenableFuture<Object>>() {
        @Override public void accept(Job<Object,Object> job, ListenableFuture<Object> result) {
            job.promise.completeWith(result);
        }
    };
    private final int batchSize;
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private final Executor executor;
    private final Function<? super ImmutableList<I>, ? extends Iterable<? extends ListenableFuture<O>>> batchConsumer;
    private final AtomicInteger pendingJobCount = new AtomicInteger();
    private final BlockingQueue<Job<I,O>> queue;
    private final SingleBatchWorker worker = new SingleBatchWorker();

    private BatchingAsyncFunction(int batchSize, long timeout, TimeUnit timeoutUnit, Executor executor, Function<? super ImmutableList<I>, ? extends Iterable<? extends ListenableFuture<O>>> batchConsumer) {
        this.batchSize = batchSize;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.executor = executor;
        this.batchConsumer = batchConsumer;
        queue = Queues.newLinkedBlockingQueue(batchSize);
    }

    public static <I, O> BatchingAsyncFunction<I, O> newAsyncBatcher(int batchSize, long timeout, TimeUnit timeoutUnit, Executor executor, Function<? super ImmutableList<I>, ? extends Iterable<? extends ListenableFuture<O>>> batchConsumer) {
        return new BatchingAsyncFunction<I, O>(batchSize, timeout, timeoutUnit, executor, batchConsumer);
    }

    public FunFuture<O> applyAsync(I input) throws InterruptedException {
        Job<I,O> job = new Job<>(input);
        queue.put(job); // may throw InterruptedException
        if (pendingJobCount.incrementAndGet() % batchSize == 1) {
            try {
                executor.execute(worker); // could throw RejectedExecutionException, etc
            } catch (RuntimeException e) {
                handleRejectedExecution(e);
            }
        }
        return job.promise;
    }

    private void handleRejectedExecution(RuntimeException e) {
        // we have to fail any
        int deadJobCount = pendingJobCount.getAndSet(0) % batchSize;
        List<Job> deadJobs = Lists.newArrayListWithCapacity(deadJobCount);
        queue.drainTo(deadJobs, deadJobCount);

        for (Job deadJob : deadJobs) {
            deadJob.promise.setFailure(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Sink2<Job, ListenableFuture<O>> completePromise() {
        return COMPLETE_PROMISE;
    }

    private class SingleBatchWorker implements Runnable {
        @Override public void run() {
            List<Job<I, O>> jobs = getJobs();
            try {
                Iterable<? extends ListenableFuture<O>> result = batchConsumer.apply(ImmutableList.copyOf(Lists.transform(jobs, Source.<I>getter())));
                extend(jobs).zip(result).foreach2(completePromise());
            } catch (Exception t) {
                for (Job job : jobs) {
                    job.promise.setFailure(t);
                }
            }
        }

        private List<Job<I,O>> getJobs() {
            List<Job<I,O>> jobs = Lists.newArrayListWithCapacity(batchSize);
            // synchronized to prevent batches from racing to consume jobs, which would make this MORE complicated
            synchronized (queue) {
                int consumed = Queues.drainUninterruptibly(queue, jobs, batchSize, timeout, timeoutUnit);

                int pending;
                int toConsume;
                do {
                    pending = pendingJobCount.get();
                    // it's possible that consumed can EXCEED pending here, because drain can proceed before
                    // pendingJobCount is incremented. no worries; pending will just go negative for a moment.
                    // this COULD cause a spurious batch to submitted; no big deal.
                    toConsume = Math.max(consumed, Math.min(pending, batchSize));
                } while (!pendingJobCount.compareAndSet(pending, pending - toConsume));
                int remaining = toConsume - consumed;
                if (remaining > 0) queue.drainTo(jobs, remaining);
                return jobs;
            }
        }
    }

    private static class Job<I, O> implements Source<I> {
        private final I input;
        private final Promise<O> promise = newPromise();

        private Job(I input) {
            this.input = input;
        }

        @Override
        public I get() {
            return input;
        }
    }
}
