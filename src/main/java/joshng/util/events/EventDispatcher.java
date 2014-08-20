package joshng.util.events;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Reflect;
import joshng.util.blocks.F;
import joshng.util.blocks.Pred;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Nothing;
import joshng.util.concurrent.ErrorCollectingCompletionTracker;
import joshng.util.concurrent.FunFuture;
import joshng.util.concurrent.SameThreadTrampolineExecutor;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 6/5/14
 * Time: 1:03 PM
 */
public class EventDispatcher {
  private final Function<Class<?>, ? extends Iterable<? extends EventObserver<?>>> eventTypeObserverLocator;
  private final SameThreadTrampolineExecutor trampolineExecutor = new SameThreadTrampolineExecutor();

  public static EventDispatcher build(Iterable<? extends EventObserver<?>> eventObservers) {
    Multimap<Class, EventObserver<Object>> observersByEventType = extend(eventObservers).<EventObserver<Object>>cast().groupBy(
            EventObserver::getObservedEventType);

    LoadingCache<Class<?>, Iterable<EventObserver<Object>>> cacheByEventType = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Iterable<EventObserver<Object>>>() {
      @Override
      public Iterable<EventObserver<Object>> load(Class<?> key) throws Exception {
        Set<Class<?>> allInterfaces = Reflect.getAllInterfaces((Class)key, true);
        return extend(allInterfaces).flatMap(F.forMap(observersByEventType.asMap(), ImmutableList.of())).toSet().delegate();
      }
    });

    return new EventDispatcher(cacheByEventType::getUnchecked);
  }

  public static FunIterable<EventObserver<Object>> getAnnotatedEventObservers(Object obj) {
    return Reflect.getAllDeclaredMethods(obj.getClass())
            .filter(Pred.annotatedWith(Observer.class))
            .map(method -> new AnnotatedMethodEventObserver(obj, method));
  }

  public EventDispatcher(Function<Class<?>, ? extends Iterable<? extends EventObserver<?>>> eventTypeObserverLocator) {
    this.eventTypeObserverLocator = eventTypeObserverLocator;
  }


  public FunFuture<Nothing> dispatch(Object event) {
    return trampolineExecutor.submitAsync(() -> new ErrorCollectingCompletionTracker().trackAll(
                    extend(eventTypeObserverLocator.apply(event.getClass()))
                            .<EventObserver<Object>>cast()
                            .map(observer -> FunFuture.callSafely(() -> observer.onEvent(event)))
            ).setNoMoreJobs()
    );
  }

  public interface EventObserver<E> {
    Class getObservedEventType();
    FunFuture<Nothing> onEvent(E event) throws Exception;
  }

  private static class AnnotatedMethodEventObserver implements EventObserver<Object> {
    public static final TypeVariable<Class<FunFuture>> FUTURE_TYPE_PARAMETER = FunFuture.class.getTypeParameters()[0];
    private final Object obj;
    private final Method method;
    private final Class<?> observedEventType;

    public AnnotatedMethodEventObserver(Object obj, Method method) {
      this.obj = obj;
      this.method = method;
      TypeToken<?> returnType = TypeToken.of(method.getGenericReturnType());

      checkArgument(ListenableFuture.class.isAssignableFrom(returnType.getRawType())
              && returnType.resolveType(FUTURE_TYPE_PARAMETER).getRawType() == Nothing.class, "Observer methods must return FunFuture<Nothing>", method);
      method.setAccessible(true);
      observedEventType = extend(method.getParameterTypes()).getOnlyElement();
    }

    @Override
    public Class getObservedEventType() {
      return observedEventType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public FunFuture<Nothing> onEvent(Object event) throws Exception {
      return (FunFuture<Nothing>) method.invoke(obj, event);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AnnotatedMethodEventObserver that = (AnnotatedMethodEventObserver) o;

      return method.equals(that.method) && obj.equals(that.obj);
    }

    @Override
    public int hashCode() {
      int result = obj.hashCode();
      result = 31 * result + method.hashCode();
      return result;
    }
  }
}
