package com.joshng.test;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: Apr 21, 2011
 * Time: 10:46:13 AM
 */
public class ThreadOrchestrator<E extends Enum<E>> {
    private final CyclicBarrier barrier;
    private int currentStateIdx = -1;
    private final E[] stateSequence;

    public static <E extends Enum<E>> ThreadOrchestrator<E> create(int threadCount, Class<E> enumClass) {
        return new ThreadOrchestrator<E>(threadCount, enumClass);
    }

    public ThreadOrchestrator(int threadCount, Class<E> enumClass) {
        this(threadCount, enumClass.getEnumConstants());
    }

    @SafeVarargs
    public ThreadOrchestrator(int threadCount, E... stateSequence) {
        this.stateSequence = stateSequence;
        barrier = new CyclicBarrier(threadCount, new Runnable() {
            public void run() {
                currentStateIdx++;
            }
        });
    }

    public void awaitState(E desiredState, long timeout, TimeUnit timeUnit) throws BrokenBarrierException, InterruptedException {
        long abortTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (currentState() != desiredState) {
            long waitTime = abortTime - System.currentTimeMillis();
            MoreAssertions.waitFor(barrier, waitTime, TimeUnit.MILLISECONDS, "Waited too long for state: " + desiredState);
        }
    }

    @Nullable
    public E currentState() {
        return currentStateIdx >= 0 ? stateSequence[currentStateIdx] : null;
    }

    public <T> DelayedMockAnswer<T> delayThenReturn(final T returnValue, final E answerState, final long timeout, final TimeUnit timeUnit) {
        return new DelayedMockAnswer<T>(returnValue, answerState, timeout, timeUnit);
    }

    public <T> DelayedRealMethod<T> delayRealMethodUntil(final E answerState, final long timeout, final TimeUnit timeUnit) {
        return new DelayedRealMethod<T>(answerState, timeout, timeUnit);
    }

    public abstract class DelayedAnswer<T> implements Answer<T> {
        private final E answerState;
        private final long timeout;
        private final TimeUnit timeUnit;
        private boolean didDelay = false;

        protected DelayedAnswer(E answerState, long timeout, TimeUnit timeUnit) {
            this.answerState = answerState;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        protected abstract T getReturnValue(InvocationOnMock invocationOnMock) throws Throwable;

        public T answer(InvocationOnMock invocation) throws Throwable {
            if (!didDelay) {
                awaitState(answerState, timeout, timeUnit);
                didDelay = true;
            }
            return getReturnValue(invocation);
        }
    }

    public class DelayedMockAnswer<T> extends DelayedAnswer<T> {
        private final T mockAnswer;

        protected DelayedMockAnswer(T mockAnswer, E answerState, long timeout, TimeUnit timeUnit) {
            super(answerState, timeout, timeUnit);
            this.mockAnswer = mockAnswer;
        }

        public void when(T ongoingStubbing) {
            Mockito.when(ongoingStubbing).thenAnswer(this);
        }

        @Override
        protected T getReturnValue(InvocationOnMock invocationOnMock) throws Throwable {
            return mockAnswer;
        }
    }

    public class DelayedRealMethod<T> extends DelayedAnswer<T> {
        protected DelayedRealMethod(E answerState, long timeout, TimeUnit timeUnit) {
            super(answerState, timeout, timeUnit);
        }


        public <X> X whenSpy(X spy) {
            return Mockito.doAnswer(this).when(spy);
        }

        @SuppressWarnings({"unchecked"})
        protected T getReturnValue(InvocationOnMock invocation) throws Throwable {
            return (T) invocation.callRealMethod();
        }
    }
}
