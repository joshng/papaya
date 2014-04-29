package joshng.util.context;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.blocks.F;
import joshng.util.blocks.SideEffect;
import joshng.util.blocks.Sink;
import joshng.util.blocks.Source;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.FunFuture;
import joshng.util.concurrent.FunFutures;

import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: 11/18/11
 * Time: 5:03 PM
 */
public abstract class StackContext implements TransientContext {
    public static StackContext of(TransientContext first, TransientContext second, TransientContext... rest) {
        return new CompositeStackContext(Lists.asList(first, second, rest));
    }

    public static NullState nullState() {
        return NullState.INSTANCE;
    }

    public static void runInContext(TransientContext context, Runnable r) {
        State state = context.enter();
        try {
            r.run();
        } finally {
            state.exit();
        }
    }

    public static <T> T callInContext(TransientContext context, Callable<T> callable) throws Exception {
        State state = context.enter();
        try {
            return callable.call();
        } finally {
            state.exit();
        }
    }

    public static <T> T getInContext(TransientContext context, Supplier<T> supplier) {
        State state = context.enter();
        try {
            return supplier.get();
        } finally {
            state.exit();
        }
    }


    public static <T> FunFuture<T> callInContextAsync(TransientContext context, Callable<? extends ListenableFuture<T>> futureBlock) {
        final State state = context.enter();
        try {
            return FunFutures.callSafely(futureBlock)
                    .uponCompletion(new SideEffect() {
                        public void run() {
                            state.exit();
                        }
                    });
        } catch (Exception e) {
            return FunFutures.immediateFailedFuture(e);
        }
    }

    public static SideEffect wrapInContext(final TransientContext context, final Runnable block) {
        return new RunnableInContext(context, block);
    }

    public static <O> Callable<O> wrapInContext(final TransientContext context, final Callable<O> block) {
        return new Callable<O>() {
            public O call() throws Exception {
                return callInContext(context, block);
            }
        };
    }

    public static <I,O> F<I,O> wrapInContext(final TransientContext context, final F<I, O> function) {
        return input -> getInContext(context, function.bind(input));
    }

    public static <T> Sink<T> wrapInContext(final TransientContext context, final Sink<T> sink) {
        return value -> getInContext(context, sink.bind(value));
    }

    public static <I, O> AsyncF<I, O> wrapInContextAsync(final TransientContext context, final AsyncF<I, O> asyncFunction) {
        return input -> callInContextAsync(context, asyncFunction.bind(input));
    }

    public static <T> Source<FunFuture<T>> wrapAsyncCallable(final TransientContext context, final Callable<? extends FunFuture<T>> asyncCallable) {
        return () -> callInContextAsync(context, asyncCallable);
    }

    public void runInContext(Runnable r) {
        runInContext(this, r);
    }

    public <T> T callInContext(Callable<T> callable) throws Exception {
        return callInContext(this, callable);
    }

    public <T> FunFuture<T> callInContextAsync(Callable<FunFuture<T>> asyncCallable) {
        return callInContextAsync(this, asyncCallable);
    }

    public <T> T getInContext(Supplier<T> supplier) {
        return getInContext(this, supplier);
    }

    public SideEffect wrap(final Runnable block) {
        return wrapInContext(this, block);
    }

    public F<Runnable, SideEffect> runnableWrapper() {
        return new F<Runnable, SideEffect>() { public SideEffect apply(Runnable input) {
            return wrap(input);
        } };
    }

    public <O> Callable<O> wrapCallable(final Callable<O> block) {
        return wrapInContext(this, block);
    }

    public <I,O> F<I,O> wrapFunction(F<I,O> function) {
        return wrapInContext(this, function);
    }

    public <T> Sink<T> wrapSink(Sink<T> sink) {
        return wrapInContext(this, sink);
    }

    public enum NullState implements State {
        INSTANCE;

        public void exit() {
            // nothing
        }
    }

    private static class RunnableInContext implements SideEffect {
        private final TransientContext context;
        private final Runnable block;

        public RunnableInContext(TransientContext context, Runnable block) {
            this.context = context;
            this.block = block;
        }

        public void run() {
            runInContext(context, block);
        }
    }
}
