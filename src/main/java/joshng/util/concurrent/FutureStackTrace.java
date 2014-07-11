package joshng.util.concurrent;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.MapMaker;
import joshng.util.Reflect;
import joshng.util.ThreadLocalRef;
import joshng.util.collect.PersistentList;
import joshng.util.context.TransientContext;
import joshng.util.proxy.ProxyUtil;
import joshng.util.proxy.UniversalInvocationHandler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

/**
 * User: josh
 * Date: 7/8/14
 * Time: 1:53 PM
 */
public class FutureStackTrace {
  private static final ThreadLocalRef<PersistentList<Object>> REF = new ThreadLocalRef<>(PersistentList.nil());
  private static final ConcurrentMap<Throwable, PersistentList<Object>> EXCEPTION_ANNOTATIONS = new MapMaker().weakKeys().makeMap();

  public static PersistentList<Object> get() {
    return REF.get();
  }

  public static TransientContext getCurrentContext() {
    PersistentList<Object> value = get();
    return getContext(value);
  }

  public static <E extends Throwable> E annotateWithCurrentContext(E throwable) {
    return annotate(throwable, get());
  }

  private static TransientContext getContext(final PersistentList<Object> value) {
    return value.isEmpty() ? TransientContext.NULL : new SnapshotFutureContext(value);
  }

  public static TransientContext getNestedContext(Object additionalState) {
    return new NestedFutureContext(additionalState);
  }

  private static <E extends Throwable> E annotate(E error, PersistentList<Object> value) {
    return annotate2(error, value);
//    EXCEPTION_ANNOTATIONS.putIfAbsent(error, value);
  }

  public static List<Object> getAnnotation(Throwable error) {
    return EXCEPTION_ANNOTATIONS.getOrDefault(error, PersistentList.nil());
  }

  public interface Annotated {
    PersistentList<Object> getContext();
  }

  private static final Method GET_CONTEXT = Reflect.getMethod(Annotated.class, "getContext");

  @SuppressWarnings("unchecked")
  public static <E extends Throwable> E annotate2(E throwable, PersistentList<Object> context) {
    if (context.isEmpty()) return throwable;
    //noinspection ThrowableResultOfMethodCallIgnored
    return (E) ProxyUtil.createProxy(new UniversalInvocationHandler() {
                                       @Override
                                       protected Object handle(Object proxy, Method method, Object[] args) throws Throwable {
                                         if (method == GET_CONTEXT) return context;
                                         if (method.getName().equals("getMessage") && args.length == 0) return buildMessage();
                                         if (method.getName().equals("fillInStackTrace") && args.length == 0) return proxy;
                                         method.setAccessible(true);
                                         return method.invoke(throwable, args);
                                       }

                                       private String buildMessage() {
                                         return Joiner.on("\n  ").appendTo(new StringBuilder(Strings.nullToEmpty(throwable.getMessage())).append("\nFuture Context:\n  "),
                                                 context).toString();
                                       }
                                     },
            throwable.getClass(), false, Annotated.class);
  }



  private abstract static class AbstractFutureContext implements TransientContext {
    @Override
    public State enter() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T callInContext(Callable<T> callable) throws Exception {
      PersistentList<Object> parent = get();
      PersistentList<Object> newContext = getNewContext(parent);
      REF.set(newContext);
      try {
        return callable.call();
      } catch (Exception e) {
        throw annotate(e, newContext);
      } finally {
        REF.set(parent);
      }
    }

    abstract PersistentList<Object> getNewContext(PersistentList<Object> existingContext);
  }

  private static class SnapshotFutureContext extends AbstractFutureContext {
    private final PersistentList<Object> value;

    public SnapshotFutureContext(PersistentList<Object> value) {
      this.value = value;
    }

    @Override
    PersistentList<Object> getNewContext(PersistentList<Object> existingContext) {
      return value;
    }

    @Override
    public String toString() {
      return Joiner.on("\n  ").appendTo(new StringBuilder("SnapshotFutureContext:\n  "), value).toString();
    }
  }

  private static class NestedFutureContext extends AbstractFutureContext {
    private final Object additionalContext;

    private NestedFutureContext(Object additionalContext) {
      this.additionalContext = additionalContext;
    }

    @Override
    PersistentList<Object> getNewContext(PersistentList<Object> existingContext) {
      return existingContext.with(additionalContext);
    }
  }

}
