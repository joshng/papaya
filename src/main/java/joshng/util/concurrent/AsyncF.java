package joshng.util.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Reflect;
import joshng.util.blocks.F;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.concurrent.FunFutures.extendFuture;

/**
 * User: josh
 * Date: 12/7/12
 * Time: 11:38 AM
 */
public abstract class AsyncF<I, O> extends F<I, FunFuture<O>> implements IAsyncFunction<I,O> {
    private static final AsyncF IDENTITY = new AsyncF<ListenableFuture<Object>, Object>() {
        @Override
        public FunFuture<Object> applyAsync(ListenableFuture<Object> input) {
            return extendFuture(input);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> AsyncF<ListenableFuture<? extends T>, T> asyncIdentity() {
        return IDENTITY;
    }

    public static <I, O> AsyncF<I, O> extendAsyncFunction(final Function<? super I, ? extends ListenableFuture<O>> asyncFunction) {
        if (asyncFunction instanceof AsyncF) return Reflect.blindCast(asyncFunction);
        return new AsyncF<I, O>() {
            public FunFuture<O> applyAsync(I input) {
                return extendFuture(asyncFunction.apply(input));
            }
        };
    }

    public static <I, O> AsyncF<I, O> asyncF(final AsyncFunction<? super I, O> function) {
        if (function instanceof AsyncF) return Reflect.blindCast(function);
        return new AsyncF<I, O>() {
            public FunFuture<O> applyAsync(I input) throws Exception {
                return extendFuture(function.apply(input));
            }
        };
    }

    public AsyncF<Object, O> bindAsync(I input) {
        return extendAsyncFunction(super.bind(input));
    }

    @Nonnull
    protected abstract FunFuture<O> applyAsync(I input) throws Throwable;

    @Override
    public final FunFuture<O> apply(I input) {
        try {
            return checkNotNull(applyAsync(input), "AsyncFunction must not return null!", this);
        } catch (Throwable e) {
            return FunFutures.immediateFailedFuture(e);
        }
    }
}
