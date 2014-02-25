package joshng.util;

import com.google.common.base.Function;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import joshng.util.blocks.F;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
* User: josh
* Date: Jul 21, 2010
* Time: 9:04:21 AM
*/
@Deprecated /** this class needs more testing prior to use in production; particularly the schedule() calls */
public class OrderedExecutor extends AbstractExecutorService implements ScheduledExecutorService {
    private static final RejectedExecutionHandler DEFAULT_HANDLER = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable runnable, ExecutorService executorService) {
            throw new RejectedExecutionException();
        }
    };
    private final ListeningExecutorService threadPool;
    private final ScheduledExecutorService scheduler = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
    private final Queue<Runnable> jobQueue;
    protected final Object syncObject = new Object();
    private final ListeningExecutorService callbackExecutor;
    private final Runnable completionListener = new Runnable() {
        public void run() {
            prepareAndSubmitNextJob();
        }
    };

    private final RejectedExecutionHandler rejectedExecutionHandler;

    private boolean shutdown = false;
    protected Runnable activeJob = null;

    public OrderedExecutor(ExecutorService threadPool) {
        this(threadPool, Queues.<Runnable>newArrayDeque());
    }

    public OrderedExecutor(ExecutorService threadPool, Queue<Runnable> jobQueue) {
        this(threadPool, jobQueue, DEFAULT_HANDLER);
    }

    public OrderedExecutor(ExecutorService threadPool, Queue<Runnable> jobQueue, RejectedExecutionHandler rejectedExecutionHandler) {
        this.threadPool = MoreExecutors.listeningDecorator(threadPool);
        this.jobQueue = jobQueue;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        callbackExecutor = MoreExecutors.sameThreadExecutor();
    }

    public void execute(Runnable runnable) {

        boolean runImmediately;
        synchronized (syncObject) {
            if (shutdown) {
                rejectedExecutionHandler.rejectedExecution(runnable, this);
                return;
            }

            runImmediately = activeJob == null;
            if (runImmediately) {
                activeJob = runnable;
            } else {
                if (!jobQueue.offer(runnable)) {
                    rejectedExecutionHandler.rejectedExecution(runnable, this);
                }
            }
        }
        if (runImmediately) submitActiveJob();
    }

    void submitActiveJob() {
        try {
            threadPool.submit(activeJob).addListener(completionListener, callbackExecutor);
        } catch (RejectedExecutionException e) {
            rejectedExecutionHandler.rejectedExecution(activeJob, this);
            prepareAndSubmitNextJob();
        }
    }

    public void shutdown() {
        synchronized (syncObject) {
            shutdown = true;
        }
    }

    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("OrderedExecutor.shutdownNow has not been implemented.");
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isTerminated() {
        synchronized (syncObject) {
            return shutdown && activeJob == null && jobQueue.isEmpty();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
        synchronized (syncObject) {
            if (!isShutdown()) {
                timeUnit.timedWait(syncObject, timeout);
                return isShutdown();
            } else {
                return true;
            }
        }
    }

    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return scheduleSubmission(ListenableFutureTask.create(command, null), new F<Runnable, ScheduledFuture>() {
            public ScheduledFuture apply(Runnable task) {
                return scheduler.schedule(task, delay, unit);
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    private <T> ScheduledFuture<T> scheduleSubmission(ListenableFutureTask<T> task, Function<Runnable, ScheduledFuture> schedulingHandler) {
        FutureSubmission deferredTask = new FutureSubmission(task);

        ScheduledFuture scheduledFuture = schedulingHandler.apply(deferredTask);

        return new ForwardingScheduledFuture<T>(task, scheduledFuture);
    }

    private static class ForwardingScheduledFuture<T> extends ForwardingListenableFuture<T> implements ScheduledFuture<T> {
        private final ListenableFuture<T> delegate;
        private final ScheduledFuture<?> scheduledFuture;

        public ForwardingScheduledFuture(ListenableFuture<T> delegate, ScheduledFuture<?> scheduledFuture) {
            this.delegate = delegate;
            this.scheduledFuture = scheduledFuture;
        }

        public long getDelay(TimeUnit unit) {
            return scheduledFuture.getDelay(unit);
        }

        public int compareTo(Delayed o) {
            return scheduledFuture.compareTo(o);
        }

        @Override
        protected ListenableFuture<T> delegate() {
            return delegate;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            scheduledFuture.cancel(mayInterruptIfRunning);
            return delegate.cancel(mayInterruptIfRunning);
        }
    }

    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return scheduleSubmission(ListenableFutureTask.create(callable), new F<Runnable,ScheduledFuture>() {
            public ScheduledFuture apply(Runnable value) {
                return scheduler.schedule(value, delay, unit);
            }
        });
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return scheduleSubmission(ListenableFutureTask.create(command, null), new F<Runnable,ScheduledFuture>() {
            public ScheduledFuture apply(Runnable value) {
                return scheduler.scheduleAtFixedRate(value, initialDelay, period, unit);
            }
        });
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return scheduleSubmission(ListenableFutureTask.create(command, null), new F<Runnable, ScheduledFuture>() {
            public ScheduledFuture apply(Runnable value) {
                return scheduler.scheduleWithFixedDelay(value, initialDelay, delay, unit);
            }
        });
    }

    protected void prepareAndSubmitNextJob() {
        if (prepareNextJob()) {
            submitActiveJob();
        }
    }

    protected boolean prepareNextJob() {
        synchronized (syncObject) {
            activeJob = jobQueue.poll();
            boolean runNext = activeJob != null;
            if (isTerminated()) syncObject.notifyAll();
            return runNext;
        }
    }

    private class FutureSubmission implements Runnable {
        private final ListenableFutureTask<?> command;

        public FutureSubmission(ListenableFutureTask<?> command) {
            this.command = command;
        }

        public void run() {
            submit(command);
        }
    }

    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable runnable, ExecutorService executorService);
    }
}
