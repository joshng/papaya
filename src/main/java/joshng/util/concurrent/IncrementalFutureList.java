package joshng.util.concurrent;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;
import joshng.util.collect.FunList;

import java.util.Queue;
import java.util.concurrent.ExecutionException;

/**
 * Created by: josh 11/15/13 1:39 PM
 */
public class IncrementalFutureList<T> extends AbstractCompletionTracker<T, FunList<T>> {
    private final Queue<ListenableFuture<? extends T>> futures = Queues.newLinkedBlockingQueue();

    public IncrementalFutureList() {
        super(MoreExecutors.sameThreadExecutor());
        getCompletionFuture().uponFailure(new Sink<Throwable>() {
            @Override public void handle(Throwable value) {
                for (ListenableFuture<? extends T> future : futures) {
                    future.cancel(false);
                }
            }
        });
    }

    @Override
    public <F extends ListenableFuture<? extends T>> F submit(F job) {
        futures.offer(job);
        return super.submit(job);
    }

    @Override
    protected void handleCompletedJob(ListenableFuture<? extends T> job) {
        try {
            Uninterruptibles.getUninterruptibly(job);
        } catch (ExecutionException e) {
            abort(e.getCause());
        } catch (Throwable t) {
            abort(t);
        }
    }

    @Override
    protected void completePromise(Promise<FunList<T>> completionPromise) {
        completionPromise.complete(new Source<FunList<T>>() {
            @Override public FunList<T> get() {
                return FunFutures.<T>getFromFuture().transform(futures).toList();
            }
        });
    }
}
