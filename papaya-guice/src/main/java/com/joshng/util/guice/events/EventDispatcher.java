package com.joshng.util.guice.events;

import com.joshng.util.collect.Nothing;
import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.reflect.Reflect;

import javax.inject.Inject;
import java.util.Set;

import static com.joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 11/16/18
 * Time: 9:42 AM
 */
public class EventDispatcher<T> {
  private final Set<EventObserver<T>> observers;
  private final ObserverRegistration<T> observerRegistration;

  @Inject
  public EventDispatcher(Set<EventObserver<T>> observers, ObserverRegistration<T> observerRegistration) {
    this.observers = observers;
    this.observerRegistration = observerRegistration;
  }

  public FunFuture<Nothing> dispatch(T event) {
    observerRegistration.checkEventSubtype(Reflect.getUnenhancedClass(event));
    return FunFuture.trackSuccess(extend(observers).map(o -> o.onEvent(event)));
  }

  static class ObserverRegistration<T> {
    private final Set<Class<?>> registeredEventSubtypes;

    ObserverRegistration(Set<Class<?>> registeredEventSubtypes) {
      this.registeredEventSubtypes = registeredEventSubtypes;
    }

    void checkEventSubtype(Class<?> eventType) {
      if (!registeredEventSubtypes.contains(eventType))
        throw new IllegalStateException("Tried to dispatch unregistered event-type: " + eventType);
    }
  }
}
