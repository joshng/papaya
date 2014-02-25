package joshng.util.blocks;

import com.google.common.base.Defaults;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import joshng.util.Modifiers;
import joshng.util.ThreadLocalRef;
import joshng.util.ThreadLocals;
import joshng.util.proxy.UniversalInvocationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.*;
import static joshng.util.proxy.ProxyUtil.createProxy;

/**
 * User: josh
 * Date: 2/1/12
 * Time: 12:18 AM
 */
public class FBuilder<I> {
    private static final InvocationInterceptor INTERCEPTOR = new InvocationInterceptor();
    private static final ThreadLocalRef<Invocation> INVOCATION_HOLDER = ThreadLocals.newThreadLocalRef();
    private static final LoadingCache<Class<?>, FBuilder> BUILDERS = CacheBuilder.newBuilder().softValues().build(new CacheLoader<Class<?>, FBuilder>() {
        @SuppressWarnings({"unchecked"})
        public FBuilder load(Class<?> targetClass) throws Exception {
            return new FBuilder(targetClass);
        }
    });

    public final I input;

    private FBuilder(Class<I> targetClass) {
        input = createProxy(INTERCEPTOR, targetClass, false);
    }

    // parameter-type is Class<? super I> to support convenient proxying of generic types
    @SuppressWarnings({"unchecked"})
    public static <I> FBuilder<I> on(Class<? super I> inputClass) {
        return BUILDERS.getUnchecked(inputClass);
    }

    public static <I> FBuilder<I> on(TypeToken<I> type) {
        return on(type.getRawType());
    }

    public static <I, O> F<I, O> on(Class<I> inputClass, O invokedResult) {
        return buildFunction();
    }

    public static <I> Pred<I> predicateOn(Class<I> inputClass, boolean invokedResult) {
        return buildPredicate();
    }

    public <O2> F<I, O2> returning(O2 proxyInvocationResult) {
        return buildFunction();
    }

    public Pred<I> predicate(boolean proxyInvocationResult) {
        return buildPredicate();
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> F<I, O> buildFunction() {
        final Invocation invocation = consumeInvocation();
        return new FBuilderFunction(invocation);
    }

    private static <I> Pred<I> buildPredicate() {
        final Invocation invocation = consumeInvocation();
        return new Pred<I>() {
            public boolean apply(I input) {
                return (Boolean) invocation.invoke(input);
            }

            @Override
            public String toString() {
                return invocation.appendDescription(new StringBuilder("Pred")).toString();
            }
        };
    }

    public static <I> I input(Class<I> targetClass) {
        return (I) on(targetClass).input;
    }


    private static Invocation consumeInvocation() {
        Invocation invocation = checkNotNull(INVOCATION_HOLDER.get(), "Incorrect FBuilder usage: missing invocation");
        INVOCATION_HOLDER.remove();
        return invocation;
    }

    private static class InvocationInterceptor extends UniversalInvocationHandler {
        @Override
        public Object handle(Object proxy, Method method, Object[] args) throws Throwable {
            checkArgument(!Modifiers.Private.matches(method), "FBuilder cannot invoke private methods: %s", method);
            checkArgument(!Modifiers.Final.matches(method), "FBuilder cannot invoke final methods: %s", method);
            if (!INVOCATION_HOLDER.compareAndSet(null, new Invocation(method, args))) {
                Invocation brokenInvocation = INVOCATION_HOLDER.get();
                INVOCATION_HOLDER.remove();
                throw new AssertionError("Incorrect FBuilder usage: found incomplete invocation of " + brokenInvocation + " (note that FBuilder invocations do not currently support chaining)");
            }
            return Defaults.defaultValue(method.getReturnType());
        }
    }

    private static class Invocation {
        private static final Joiner ARG_JOINER = Joiner.on(", ");
        private final Method method;
        private final Object[] args;

        private Invocation(Method method, Object[] args) {
            checkArgument(!Modifiers.Private.matches(method), "FBuilder cannot invoke private methods: %s", method);
            checkArgument(!Modifiers.Final.matches(method), "FBuilder cannot invoke final methods: %s", method);
            this.method = method;
            this.args = args;
            method.setAccessible(true);
        }

        private Object invoke(Object target) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw Throwables.propagate(Objects.firstNonNull(e.getCause(), e));
            }
        }

        @Override
        public String toString() {
            return appendDescription(new StringBuilder("Invocation{"))
                    .append('}')
                    .toString();
        }

        private StringBuilder appendDescription(StringBuilder builder) {
            builder.append("{").append(method.getDeclaringClass().getName())
                    .append(" -> _.")
                    .append(method.getName())
                    .append('(');
            ARG_JOINER.appendTo(builder, args);
            builder.append(")}");
            return builder;
        }
    }

    private static class FBuilderFunction extends F {
        private final Invocation invocation;

        public FBuilderFunction(Invocation invocation) {
            this.invocation = invocation;
        }

        @Override
        public Object apply(Object input) {
            return invocation.invoke(input);
        }

        @Override
        public String toString() {
            return invocation.appendDescription(new StringBuilder("F")).toString();
        }
    }
}
