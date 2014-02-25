package joshng.util.concurrent;

import com.google.common.base.Throwables;
import joshng.util.Identifier;
import joshng.util.collect.Maybe;
import joshng.util.context.StackContext;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: josh
 * Date: 11/30/11
 * Time: 9:43 PM
 */

/**
 * A utility that enables temporarily locking access to an arbitrary resource, then unlocking it from a different thread.
 * This is useful when the requests to pause() and resume() may be triggered externally in a multithreaded server.
 * <br/><br/>
 * Example usage:
 *
 * <pre>{@code
 *
 * class ResourceConsumer {
 *     private final PausableResource requiredResource = ... ;
 *
 *     public void doSomeWork() {
 *         requiredResource.obtain();
 *         try {
 *             // ... do something ...
 *         } finally {
 *             requiredResource.release();
 *         }
 *     }
 * }
 *
 * // a mocked-up web controller that allows pausing and resuming via the web
 * class ResourceAdministratorController {
 *     private final PausableResource resource = ... ; // the same resource as above
 *
 *     public Response handlePauseRequest() {
 *         // attempt to pause access; wait a maximum of 10 seconds, and arrange to auto-resume after 30
 *         Maybe<PausableResource.PauseId> pauseIdMaybe = resource.pause(10, 30, TimeUnit.SECONDS);
 *
 *         for (PauseId pauseId : pauseIdMaybe) {
 *             return new Response(200, pauseId);  // render the pauseId for the requestor to save
 *         }
 *
 *         // pause request failed
 *         return new Response(408); // HTTP 408: Request Timeout
 *     }
 *
 *     public Response handleResumeRequest(@Param String pauseId) {
 *        if (resource.resume(PausableResource.PauseId.valueOf(pauseId)) {
 *             return new Response(200);
 *        } else {
 *            // resume request failed
 *            return new Response(403); // HTTP 403: Unprocessable 
 *        }
 *     }
 * }
 * }</pre>
 */
public class PausableResource extends StackContext {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final Condition resumeCondition = writeLock.newCondition();
    private PauseId currentPauseId = null;
    private DateTime pauseExpiry = null;

    private final State lockedState = new State() {
        public void exit() {
            release();
        }
    };

    public State enter() {
        try {
            obtain();
            return lockedState;
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Obtains permission to access the resource, preventing a pause() operation from succeeding until release() is
     * invoked by the same thread.
     *
     * If the resource is currently "paused", this will block and wait until access is "resumed" again.
     *
     * @throws InterruptedException if the calling thread is interrupted prior to obtaining permission
     */
    public void obtain() throws InterruptedException {
        readLock.lockInterruptibly();
        if (isPaused()) {
            // need to obtain write-lock to wait for resumeCondition,
            // but we can't upgrade directly from read-lock, so unlock first
            readLock.unlock();
            writeLock.lockInterruptibly();
            try {
                while (isStillPaused()) resumeCondition.await(remainingPauseMillis(), TimeUnit.MILLISECONDS);
                // downgrade to read-lock
                readLock.lockInterruptibly();
            } finally {
                writeLock.unlock();
            }
        }
    }

    public boolean isPaused() {
        return currentPauseId != null;
    }

    /**
     * Releases access to this resource. Must be called exactly once after every successful call to obtain().
     */
    public void release() {
        readLock.unlock();
    }

    /**
     * Attempts to pause access to this resource.
     * <br/><br/>
     * If the pause succeeds (ie, does not return Maybe.not()), then all requests to obtain() the resource will block
     * until either
     * <ol><li>
     *     the returned PauseId is passed back to a call to resume(), or
     *     </li><li>
     *     the indicated maxPauseDuration expires.
     * </li></ol>
     * @param tryTimeout the time to wait before aborting the pause-attempt
     * @param maxPauseDuration the maximum time to permit the resource to remain paused before auto-resuming
     * @param timeUnit the timeunit for both the tryTimeout and maxPauseDuration
     * @return a Maybe containing a PauseId if the pause succeeded, or Maybe.not() if the tryTimeout expired
     * @throws InterruptedException if the thread is interrupted before the successfully pausing
     */
    public Maybe<PauseId> pause(int tryTimeout, int maxPauseDuration, TimeUnit timeUnit) throws InterruptedException {
        final PauseId pauseId = new PauseId();
        if (!writeLock.tryLock(tryTimeout, timeUnit)) return Maybe.not();

        try {
            if (isPaused()) return Maybe.not();

            currentPauseId = pauseId;
            pauseExpiry = new DateTime().plusMillis((int)timeUnit.toMillis(maxPauseDuration));
            onPaused();
            return Maybe.definitely(currentPauseId);
        } finally {
            writeLock.unlock();
        }
    }

    protected void onPaused() { }

    /**
     * Resumes access to this resource, allowing requests to obtain() the resource to proceed.
     * @param pauseId a unique identifier obtained from a call to pause() this resource.
     * @param timeout the time to wait before giving up (while unlikely, it's possible that resume() could be prevented
     * by extreme contention with other threads trying to obtain() the resource. In this case, the caller may wish to
     * fail and return rather than waiting indefinitely. The maxPauseDuration should still
     * @param timeUnit
     * @return <code>true</code> if the resource was fully paused for the entire duration since the corresponding call to pause(),<br/>
     * or <code>false</code> if the pause was already cancelled (either by another thread using this same PauseId, or because
     * the maxPauseDuration expired first).
     * @throws InterruptedException
     */
    public boolean resume(PauseId pauseId, int timeout, TimeUnit timeUnit) throws InterruptedException {
        if (!writeLock.tryLock(timeout, timeUnit)) return false;
        try {
            boolean matched = pauseId.equals(currentPauseId);
            if (matched) {
                currentPauseId = null;
                pauseExpiry = null;
                resumeCondition.signalAll();
                onResumed();
            }
            return matched;
        } finally {
            writeLock.unlock();
        }
    }

    protected void onResumed() { }

    private long remainingPauseMillis() {
        return pauseExpiry.getMillis() - System.currentTimeMillis();
    }

    // must be invoked while holding the writeLock
    private boolean isStillPaused() throws InterruptedException {
        return isPaused() && (remainingPauseMillis() > 0 || !resume(currentPauseId, 0, TimeUnit.SECONDS));
    }

    public static class PauseId extends Identifier {
        public static PauseId valueOf(String id) {
            return new PauseId(id);
        }

        private PauseId() {
            this(generateRandomIdentifier());
        }

        private PauseId(String identifier) {
            super(identifier);
        }
    }
}

