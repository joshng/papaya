package com.joshng.util.guice;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.joshng.util.collect.Ref;
import com.joshng.util.concurrent.services.BaseService;
import com.joshng.util.concurrent.services.ServiceDependencyManager;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

import static com.joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 11/30/15
 * Time: 1:32 PM
 */
public enum ManagedServicesModule implements Module {
  INSTANCE;

  private static TypeLiteral<Ref<Key<? extends BaseService<?>>>> MANAGED_SERVICE_KEY_TYPE = new TypeLiteral<Ref<Key<? extends BaseService<?>>>>() { };

  @Override public void configure(Binder binder) {
    binder.bind(ServiceDependencyManager.class).toProvider(ServiceDependencyManagerProvider.class).asEagerSingleton();
    manager(binder); // ensure multibinder is defined even if no services are managed
  }

  public static ManagedServiceBinder manager(Binder binder) {
    binder.install(INSTANCE);
    return new ManagedServiceBinder(binder);
  }

  static class ServiceDependencyManagerProvider implements Provider<ServiceDependencyManager> {
    private final Injector injector;
    private final Set<Ref<Key<? extends BaseService<?>>>> managedServiceKeyRefs;

    @Inject
    ServiceDependencyManagerProvider(Injector injector, Set<Ref<Key<? extends BaseService<?>>>> managedServiceKeyRefs) {
      this.injector = injector;
      this.managedServiceKeyRefs = managedServiceKeyRefs;
    }

    @Override public ServiceDependencyManager get() {
      return GuiceDependencyGraph.buildServiceDependencyManager(
              injector,
              extend(managedServiceKeyRefs).map(Ref::get));
    }
  }

  public static class ManagedServiceBinder {
    private final Multibinder<Ref<Key<? extends BaseService<?>>>> multibinder;

    private ManagedServiceBinder(Binder binder) {
      multibinder = Multibinder.newSetBinder(binder, MANAGED_SERVICE_KEY_TYPE);
    }

    public ManagedServiceBinder manage(Class<? extends BaseService<?>> implementation) {
      return manage(Key.get(implementation));
    }

    public ManagedServiceBinder manage(TypeLiteral<? extends BaseService<?>> implementation) {
      return manage(Key.get(implementation));
    }

    public ManagedServiceBinder manage(Key<? extends BaseService<?>> targetKey) {
      multibinder.addBinding().toInstance(Ref.of(targetKey));
      return this;
    }
  }
}
