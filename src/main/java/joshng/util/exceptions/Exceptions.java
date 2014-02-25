package joshng.util.exceptions;

import com.google.common.base.Throwables;
import joshng.util.LineNumbers;
import joshng.util.blocks.Consumer;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * User: joey
 * Date: Aug 6, 2010
 * Time: 2:54:13 PM
 */
public class Exceptions {

    public static <E extends Throwable> E propagate(Throwable t, Class<E> acceptableType) throws E {
        Throwables.propagateIfPossible(t, acceptableType);
        throw new RuntimeException(t);
    }

    @Nullable
    public static <E extends Throwable> E extractCauseOrNull(Throwable t, Class<E> causeClass) {
        while (t != null && !causeClass.isInstance(t)) {
            t = t.getCause();
        }
        return causeClass.cast(t);
    }

    public static boolean isCausedBy(Throwable t, Class<? extends Throwable> causeClass) {
        return extractCauseOrNull(t, causeClass) != null;
    }

    @SafeVarargs
    public static boolean isCausedByAny(Throwable t, Class<? extends Throwable> ... causeClasses) {
        return isCausedByAny(t, Arrays.asList(causeClasses));
    }

    public static boolean isCausedByAny(Throwable t, List<Class<? extends Throwable>> causeClasses) {
        for(Class<? extends Throwable> causeclass : causeClasses) {
            if(isCausedBy(t, causeclass)) return true;
        }
        return false;
    }

    public static <T extends Throwable, C extends Throwable> C extractCauseOrThrow(T t, Class<C> causeClass) throws T {
        C cause = extractCauseOrNull(t, causeClass);
        if (cause != null) return cause;
        throw t;
    }

    public static <C extends Throwable> boolean handleCause(Throwable t, Class<C> causeClass, final Consumer<? super C> handler) {
        return handleCause(t, new ExceptionHandler<C>(causeClass) {
            public void handle(C error) {
                handler.handle(error);
            }
        });
    }

    public static boolean handleCause(Throwable t, IExceptionHandler... handlers) {
        return handleCause(t, Arrays.asList(handlers));
    }

    public static boolean handleCause(Throwable t, Iterable<IExceptionHandler> handlers) {
        for (IExceptionHandler handler : handlers) {
            if (handler.didHandle(t)) return true;
        }
        return false;
    }
    public static <T extends Throwable> void handleCauseOrThrow(T t, Iterable<IExceptionHandler> handlers) throws T {
        if (!handleCause(t, handlers)) throw t;
    }

    public static <T extends Throwable> void handleCauseOrThrow(T t, IExceptionHandler... handlers) throws T {
        handleCauseOrThrow(t, Arrays.asList(handlers));
    }

    public static void setFirstStackTraceElement(Throwable e, Method method) {
        setFirstStackTraceElement(e, LineNumbers.getStackTraceElement(method));
    }

    public static void setFirstStackTraceElement(Throwable e, StackTraceElement element) {
        StackTraceElement[] trace = e.getStackTrace();
        trace[0] = element;
        e.setStackTrace(trace);
    }
}
