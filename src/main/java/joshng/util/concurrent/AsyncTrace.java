package joshng.util.concurrent;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Reflect;
import joshng.util.collect.PersistentSet;
import joshng.util.context.TransientContext;
import joshng.util.proxy.ProxyUtil;
import joshng.util.proxy.UniversalInvocationHandler;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: 7/8/14
 * Time: 1:53 PM
 */
public abstract class AsyncTrace implements TransientContext {
  private static final AsyncContext<PersistentSet<Object>> REF = new AsyncContext<>(PersistentSet.nil());

  public static PersistentSet<Object> get() {
    return REF.get();
  }

  public static TransientContext getCurrentContext() {
    return getContext(get());
  }

  public static <E extends Throwable> E annotateWithCurrentContext(E throwable) {
    return annotateThrowable(throwable, get());
  }

  private static TransientContext getContext(final PersistentSet<Object> value) {
    return value.isEmpty() ? TransientContext.NULL : new SnapshotAsyncContext(value);
  }

  public static AsyncTrace getNestedContext(Object additionalState) {
    return new NestedAsyncContext(additionalState);
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> E annotateThrowable(E error, PersistentSet<Object> value) {
    if (value.isEmpty()) return error;
    //noinspection ThrowableResultOfMethodCallIgnored
    return MoreObjects.firstNonNull(
            (E) ProxyUtil.createProxy(
                    new UniversalInvocationHandler() {
                      private String message;

                      @Override
                      protected Object handle(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("fillInStackTrace") && args.length == 0) return proxy;
                        if (method.getName().equals("getMessage") && args.length == 0) return getMessage();
                        if (method.equals(GET_CONTEXT)) return value;
                        return ACCESSIBLE_THROWABLE_METHODS.getUnchecked(method).invoke(error, args);
                      }

                      private String getMessage() {
                        if (message == null) {
                          String rawMessage = Strings.nullToEmpty(error.getMessage());
                          message = Joiner.on("\n  ").appendTo(
                                  new StringBuilder(rawMessage)
                                          .append("\nAsyncContext:\n  "),
                                  value
                          ).toString();
                        }
                        return message;
                      }
                    },
                    error.getClass(), true, Annotated.class
            ), error);
  }

  public interface Annotated {
    PersistentSet<Object> getContext();
  }

  private static final Method GET_CONTEXT = Reflect.getMethod(Annotated.class, "getContext");
  private static final LoadingCache<Method,Method> ACCESSIBLE_THROWABLE_METHODS = CacheBuilder.newBuilder().weakKeys().weakValues().build(new CacheLoader<Method, Method>() {
    @Override
    public Method load(Method key) throws Exception {
      key.setAccessible(true);
      return key;
    }
  });


  @Override
  public State enter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T callInContext(Callable<T> callable) throws Exception {
    PersistentSet<Object> parent = get();
    PersistentSet<Object> newContext = getNewContext(parent);
    REF.set(newContext);
    try {
      return callable.call();
    } catch (Exception e) {
      throw annotateThrowable(e, newContext);
    } finally {
      REF.set(parent);
    }
  }

  @Override public <T> FunFuture<T> callInContextAsync(Callable<? extends ListenableFuture<T>> futureBlock) {
    PersistentSet<Object> newContext = getNewContext(get());
    return FunFuture.<T>callSafely(() -> callInContext(futureBlock)).recover(e -> !(e instanceof Annotated), e -> {
              throw annotateThrowable(e, newContext);
            });
  }

  abstract PersistentSet<Object> getNewContext(PersistentSet<Object> existingContext);

  private static class SnapshotAsyncContext extends AsyncTrace {
    private final PersistentSet<Object> value;

    public SnapshotAsyncContext(PersistentSet<Object> value) {
      this.value = value;
    }

    @Override
    PersistentSet<Object> getNewContext(PersistentSet<Object> existingContext) {
      return existingContext.isEmpty() ? value : PersistentSet.builder().addAll(value).buildOnto(existingContext);
    }

    @Override
    public String toString() {
      return Joiner.on("\n  ").appendTo(new StringBuilder("SnapshotAsyncContext:\n  "), value).toString();
    }
  }

  private static class NestedAsyncContext extends AsyncTrace {
    private final Object additionalContext;

    private NestedAsyncContext(Object additionalContext) {
      this.additionalContext = additionalContext;
    }

    @Override
    PersistentSet<Object> getNewContext(PersistentSet<Object> existingContext) {
      return existingContext.with(additionalContext);
    }
  }

}
