package com.joshng.test;

import com.google.common.base.Throwables;
import com.joshng.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: josh
 * Date: Jul 17, 2010
 * Time: 1:02:19 PM
 */
public class MoreAssertions {
    private static final Logger LOG = LoggerFactory.getLogger(MoreAssertions.class);

    @SuppressWarnings({"unchecked"})
    public static <E extends Throwable> E expectException(Class<E> exceptionClass, TestBlock block) {
        try {
            block.test();
            fail("Expected exception: " + exceptionClass.getSimpleName());
            return null; // not reached
        } catch (Throwable e) {
            if (!exceptionClass.isAssignableFrom(e.getClass())) throw Throwables.propagate(e);
            return (E)e;
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <E extends Throwable> E expectExceptionCausedBy(Class<E> exceptionClass, TestBlock block) {
        try {
            block.test();
            fail("Expected exception caused by: " + exceptionClass.getSimpleName());
            return null; // not reached
        } catch (Throwable e) {
            E cause = Exceptions.extractCauseOrNull(e, exceptionClass);
            if (cause == null) throw Throwables.propagate(e);
            return cause;
        }
    }

    public static void waitFor(CountDownLatch latch, long timeout, TimeUnit timeUnit) throws InterruptedException {
        waitFor(latch, timeout, timeUnit, "Waited too long");
    }

    public static void waitFor(CountDownLatch latch, long timeout, TimeUnit timeUnit, String message) throws InterruptedException {
        if (!latch.await(timeout, timeUnit)) {
            failUnlessWaitingForever(message, timeout, timeUnit);
            latch.await();
        }
    }

    public static void waitFor(CyclicBarrier barrier, long timeout, TimeUnit timeUnit) throws InterruptedException, BrokenBarrierException {
        waitFor(barrier, timeout, timeUnit, "Waited too long");
    }

    public static void waitFor(CyclicBarrier barrier, long timeout, TimeUnit timeUnit, String message) throws InterruptedException, BrokenBarrierException {
        try {
            barrier.await(timeout, timeUnit);
            return;
        } catch (TimeoutException e) {
            failUnlessWaitingForever(message, timeout, timeUnit);
            barrier.await();
        }
    }

    public static <T> T waitFor(BlockingQueue<T> queue, long timeout, TimeUnit timeUnit) throws InterruptedException {
        return waitFor(queue, timeout, timeUnit, "Waited too long");
    }

    public static <T> T waitFor(BlockingQueue<T> queue, long timeout, TimeUnit timeUnit, String message) throws InterruptedException {
        T result = queue.poll(timeout, timeUnit);
        if (result == null) {
            failUnlessWaitingForever(message, timeout, timeUnit);
            result = queue.take();
        }
        return result;
    }

    public static <T> T waitFor(Future<T> future, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
        return waitFor(future, timeout, timeUnit, "Waited too long");
    }

    public static <T> T waitFor(Future<T> future, long timeout, TimeUnit timeUnit, String message) throws InterruptedException, TimeoutException, ExecutionException {
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            failUnlessWaitingForever(message, timeout, timeUnit);
            return future.get();
        }
    }

    public static void waitFor(Thread thread, long timeout, TimeUnit timeUnit) throws InterruptedException {
        waitFor(thread, timeout, timeUnit, "Waited too long for thread to exit");
    }

    public static void waitFor(Thread thread, long timeout, TimeUnit timeUnit, String message) throws InterruptedException {
        timeUnit.timedJoin(thread, timeout);
        if (thread.isAlive()) failUnlessWaitingForever(message, timeout, timeUnit);
        thread.join();
    }

    public static boolean testsWaitForever() {
        return "true".equals(System.getenv().get("TESTS_WAIT_FOREVER"));
    }

    public static void failUnlessWaitingForever(String message, long timeout, TimeUnit timeUnit) {
        if (testsWaitForever()) {
            LOG.warn(message + " -- already waited " + timeUnit.toMillis(timeout) + "ms, now waiting indefinitely because ENV.TESTS_WAIT_FOREVER=true");
        } else {
            fail(message);
        }
    }

    public static <T> T assertNotNull(T obj) {
        return assertNotNull("Should not be null", obj);
    }

    public static <T> T assertNotNull(String message, T obj) {
        assertTrue(message, obj != null);
        return obj;
    }
}
