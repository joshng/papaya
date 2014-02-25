package joshng.util.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * User: josh
 * Date: 7/2/13
 * Time: 11:51 AM
 */
public abstract class CancellingRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public final void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        cancelFuture(r);
        handleRejectedTask(r);
    }

    public static boolean cancelFuture(Object r) {
        boolean isFuture = r instanceof Future;
        if (isFuture) ((Future<?>)r).cancel(false);
        return isFuture;
    }

    protected abstract void handleRejectedTask(Runnable r);
}
