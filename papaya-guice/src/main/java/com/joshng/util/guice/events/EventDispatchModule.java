package com.joshng.util.guice.events;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Types;
import com.joshng.util.reflect.Reflect;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * User: josh
 * Date: 11/16/18
 * Time: 12:27 PM
 */
public class EventDispatchModule extends AbstractModule {
  private static final Type EVENT_PARAM_TYPE = eventParamType();
  private final LoadingCache<Class<?>, Set<Class<?>>> eventRegistrationBuilder = CacheBuilder.newBuilder()
          .build(new CacheLoader<Class<?>, Set<Class<?>>>() {
            @Override
            public Set<Class<?>> load(Class<?> key) {
              Set<Class<?>> subtypes = new HashSet<>();
              subtypes.add(key);
              for (Class<?> supertype : Reflect.getAllInterfaces(key, true)) {
                if (supertype == key) continue;
                eventRegistrationBuilder.getUnchecked(supertype).add(key);
              }
              return subtypes;
            }
          });
  private boolean frozen = false;
  private Map<Class<?>, Set<Class<?>>> registration;
  private final Set<Key<? extends EventObserver<?>>> observerKeys = new HashSet<>();

  public synchronized EventDispatchModule registerEventType(Class<?> eventType) {
    checkFrozen();
    eventRegistrationBuilder.getUnchecked(eventType);
    return this;
  }

  public synchronized <T, O extends EventObserver<T>> EventDispatchModule registerEventObserver(Class<O> observerClass) {
    return registerEventObserver(Key.get(observerClass));
  }

  public synchronized <T, O extends EventObserver<T>> EventDispatchModule registerEventObserver(Key<O> observerKey) {
    checkFrozen();
    observerKeys.add(observerKey);
    return this;
  }

  private void checkFrozen() {
    checkState(!frozen, "Registered an event-wiring after dispatch was configured");
  }

  @Override
  protected synchronized void configure() {
    frozen = true;
    registration = ImmutableMap.copyOf(eventRegistrationBuilder.asMap());
    for (Map.Entry<Class<?>, Set<Class<?>>> subtypes : registration.entrySet()) {
      TypeLiteral<EventDispatcher.ObserverRegistration<Object>> registrationType = (TypeLiteral<EventDispatcher.ObserverRegistration<Object>>) TypeLiteral.get(Types.newParameterizedTypeWithOwner(EventDispatcher.class, EventDispatcher.ObserverRegistration.class, subtypes.getKey()));
      bind(registrationType).toInstance(new EventDispatcher.ObserverRegistration<>(subtypes.getValue()));
    }
    for (Key<? extends EventObserver<?>> observerKey : observerKeys) {
      configureObserver(observerKey);
    }
  }

  private <T, O extends EventObserver<T>> void configureObserver(Key<? extends EventObserver<?>> observerKeyUnsafe) {
    Key<O> observerKey = (Key<O>) observerKeyUnsafe;
    Class<?> observedType = TypeToken.of(observerKey.getTypeLiteral().getType())
            .resolveType(EVENT_PARAM_TYPE)
            .getRawType();
    Set<Class<?>> subtypes = checkNotNull(registration.get(observedType));
    for (Class<?> subtype : subtypes) {
      TypeLiteral<EventObserver<T>> observerLiteral = (TypeLiteral<EventObserver<T>>) TypeLiteral.get(Types.newParameterizedType(EventObserver.class, subtype));
      Multibinder.newSetBinder(binder(), observerLiteral)
              .addBinding().to(observerKey);
    }
  }

  private static Type eventParamType() {
    try {
      return EventObserver.class.getMethod("onEvent", Object.class).getGenericParameterTypes()[0];
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }
}
