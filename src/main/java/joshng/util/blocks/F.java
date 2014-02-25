package joshng.util.blocks;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Modifiers;
import joshng.util.collect.Either;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Functional;
import joshng.util.collect.Maybe;
import joshng.util.collect.Pair;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.FunFuture;
import joshng.util.exceptions.ExceptionPolicy;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Maybe.definitely;
import static joshng.util.concurrent.FunFutures.extendFuture;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 8:47:46 AM
 */
public abstract class F<I,O> implements Function<I,O>, ThrowingFunction<I, O> {
    @SuppressWarnings({"unchecked"})
    private static final F IDENTITY = new IdentityF();
    private static final F TO_STRING = new F<Object,String>() { public String apply(Object input) {
            return input.toString();
    } };
    private static F2 APPLY = new F2<Function<Object, Object>, Object, Object>() {
        @Override
        public Object apply(Function<Object, Object> input1, Object input2) {
            return input1.apply(input2);
        }
    };


    public static <I,O> F<I,O> extendF(final Function<I, O> f) {
        if (f instanceof F) return (F<I,O>) f;
        return new F<I,O>() { public O apply(I input) {
            return f.apply(input);
        } };
    }

    @SuppressWarnings("unchecked")
    public static <I, O> F2<Function<? super I, ? extends O>, I, O> apply() {
        return APPLY;
    }

    public abstract O apply(I input);

    public <O2> F<I,O2> andThen(final Function<? super O, O2> next) {
        return new F<I, O2>() { public O2 apply(I from) {
            return next.apply(F.this.apply(from));
        } };
    }

    public <O2> AsyncF<I,O2> andThenAsync(final Function<? super O, ? extends ListenableFuture<O2>> next) {
        return new AsyncF<I, O2>() { public FunFuture<O2> applyAsync(I from) {
            return extendFuture(next.apply(F.this.apply(from)));
        } };
    }

    public Sink<I> andThenSink(final Consumer<? super O> sink) {
        return new Sink<I>() { public void handle(I value) {
            sink.handle(F.this.apply(value));
        } };
    }

    public F<I, O> withSideEffect(final Runnable sideEffect) {
        final F<I, O> function = F.this;
        return new F<I, O>() {
            @Override
            public O apply(I input) {
                return applyWithSideEffect(input, function, sideEffect);
            }
        };
    }

    public static <I, O> O applyWithSideEffect(I input, Function<I, O> function, Runnable sideEffect) {
        try {
            return function.apply(input);
        } finally {
            sideEffect.run();
        }
    }

    public Pred<I> resultMatches(final Predicate<? super O> predicate) {
        return new Pred<I>() {
            public boolean apply(I input) {
                return predicate.apply(F.this.apply(input));
            }

            @Override
            public String toString() {
                return F.this.toString() + " matches " + predicate;
            }
        };
    }

    public Pred<I> resultEqualTo(O predicateValue) {
        return resultMatches(Pred.equalTo(predicateValue));
    }

    public Pred<I> resultNotEqualTo(O predicateValue) {
        return resultMatches(Pred.notEqualTo(predicateValue));
    }

    public <I0> F<I0, O> compose(final Function<I0, ? extends I> first) {
        return new F<I0, O>() { public O apply(I0 input) {
            return F.this.apply(first.apply(input));
        } };
    }

    public FunIterable<O> transform(Iterable<? extends I> inputs) {
        return Functional.map(inputs, this);
    }

    public <I1> F2<I1, I, O> f2ignoringFirst() {
        return new F2<I1, I, O>() { public O apply(I1 input1, I input2) {
            return F.this.apply(input2);
        } };
    }

    public <I2> F2<I, I2, O> f2ignoringSecond() {
        return new F2<I, I2, O>() { public O apply(I input1, I2 input2) {
            return F.this.apply(input1);
        } };
    }

    public Source<O> bind(final I input) {
        return new Source<O>() { public O get() {
            return F.this.apply(input);
        } };
    }

    public Source<O> bindFrom(final Supplier<? extends I> inputSupplier) {
        return new Source<O>() {
            @Override
            public O get() {
                return F.this.apply(inputSupplier.get());
            }
        };
    }

    public F<I, Source<O>> binder() {
        return new F<I, Source<O>>() {
            @Override
            public Source<O> apply(I input) {
                return F.this.bind(input);
            }
        };
    }

    public static <I, O> F<F<I, O>, F<I, Source<O>>> getBinder() {
        return new F<F<I, O>, F<I, Source<O>>>() {
            @Override
            public F<I, Source<O>> apply(F<I, O> input) {
                return input.binder();
            }
        };
    }

    public F<I, Either<Exception, O>> exceptionToLeft() {
        return new F<I, Either<Exception, O>>() {
            @Override
            public Either<Exception, O> apply(I input) {
                return applyExceptionToLeft(input, F.this);

            }
        };
    }

    public <O2> F<I, Pair<O, O2>> asKeyWithValue(final F<? super I, ? extends O2> valueSupplier) {
        return new F<I, Pair<O, O2>>() {
            @Override
            public Pair<O, O2> apply(I input) {
                return Pair.of(F.this.apply(input), valueSupplier.apply(input));
            }
        };
    }

    public F<I,Maybe<O>> handlingExceptions(final ExceptionPolicy exceptionPolicy) {
        return new F<I, Maybe<O>>() {
            @Override
            public Maybe<O> apply(I input) {
                try {
                    return definitely(F.this.apply(input));
                } catch (RuntimeException e) {
                    exceptionPolicy.applyOrThrow(e);
                    return Maybe.not();
                }
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public static <T> F<T, T> identity() {
        return IDENTITY;
    }

    public static <K,V> F<K,V> forMap(Map<K,V> map) {
        return extendF(Functions.forMap(map));
    }

    public static <K,V> F<K,V> forMap(Map<K,V> map, @Nullable V defaultValue) {
        return extendF(Functions.forMap(map, defaultValue));
    }

    public static <T> F<Integer, T> forList(final List<T> list) {
        return new F<Integer, T>() {
            public T apply(Integer input) {
                return list.get(input);
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public static <T> F<T, String> toStringer() {
        return TO_STRING;
    }

    public static F<String, Iterable<String>> splitter(String separator) {
        return splitter(Splitter.on(separator));
    }

    public static F<String, Iterable<String>> splitter(final Splitter splitter) {
        return new F<String, Iterable<String>>() {
            @Override
            public Iterable<String> apply(String input) {
                return splitter.split(input);
            }
        };
    }

    public static <T> F<String, T> stringToValueOf(final Class<T> conversionClass) {
        final Method valueOfMethod;
        try {
            valueOfMethod = conversionClass.getMethod("valueOf", String.class);
            checkArgument(Modifiers.Static.matches(valueOfMethod), "valueOf(String) method is not static", valueOfMethod);
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }

        valueOfMethod.setAccessible(true);
        return new F<String, T>() {
            @Override
            public T apply(String input) {
                try {
                    return conversionClass.cast(valueOfMethod.invoke(null, input));
                } catch (IllegalAccessException e) {
                    throw Throwables.propagate(e);
                } catch (InvocationTargetException e) {
                    throw Throwables.propagate(e.getCause());
                }
            }
        };
    }
    
    public ImmutableListMultimap<O, I> group(Iterable<I> inputs) {
        return Functional.groupBy(inputs, this);
    }

    public static <I, O> Either<Exception, O> applyExceptionToLeft(@Nullable I input, Function<? super I, ? extends O> function) {
        try {
            return Either.right(function.apply(input));
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static class IdentityF extends F<Object, Object> {
        @Override
        public Object apply(Object input) {
            return input;
        }

        @Override
        public F andThen(Function next) {
            return F.extendF(next);
        }

        @Override
        public F compose(Function first) {
            return F.extendF(first);
        }

        @Override
        public Sink andThenSink(Consumer sink) {
            return Sink.extendHandler(sink);
        }

        @Override
        public AsyncF andThenAsync(Function next) {
            return AsyncF.extendAsyncFunction(next);
        }

        @Override
        public FunIterable transform(Iterable inputs) {
            return Functional.extend(inputs);
        }
    }
}
