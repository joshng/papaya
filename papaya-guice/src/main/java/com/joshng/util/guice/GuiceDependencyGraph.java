package com.joshng.util.guice;

import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.spi.BindingScopingVisitor;
import com.joshng.util.blocks.Pred;
import com.joshng.util.collect.FunSet;
import com.joshng.util.reflect.Reflect;
import com.joshng.util.blocks.F;
import com.joshng.util.collect.Functional;
import com.joshng.util.concurrent.services.BaseService;
import com.joshng.util.concurrent.services.ServiceDependencyManager;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * User: josh
 * Date: 6/14/13
 * Time: 1:40 PM
 */
public class GuiceDependencyGraph {
  public static <T> Multimap<ResolvedBinding<T>, ResolvedBinding<? extends T>> computeInterdependencies(Iterable<Key<? extends T>> keys, final Injector injector) {
    final FunSet<ResolvedBinding<T>> bindings = Functional.extend(keys).map(ResolvedBinding.<T>resolver(injector)).toSet();

    return bindings.asKeysToFlattened(binding -> {
      Set<ResolvedBinding<?>> transitiveDependencies = binding.findTransitiveDependencies(Pred.<ResolvedBinding<?>>notEqualTo(
              binding).and(Pred.<ResolvedBinding<?>>in(bindings)));
      return Reflect.<Set<ResolvedBinding<? extends T>>>blindCast(transitiveDependencies);
    }).toMultimap();
  }

  public static ServiceDependencyManager buildServiceDependencyManager(
          final Injector injector,
          Iterable<Key<? extends BaseService<?>>> serviceKeys
  ) {
    final Multimap<ResolvedBinding<BaseService<?>>, ResolvedBinding<? extends BaseService<?>>> graph = computeInterdependencies(
            serviceKeys,
            injector);
    for (ResolvedBinding<? extends BaseService<?>> binding : graph.values()) {
      checkArgument(binding.getBinding().acceptScopingVisitor(new BindingScopingVisitor<Boolean>() {
        public Boolean visitEagerSingleton() { return true; }

        public Boolean visitScope(Scope scope) { return scope == Scopes.SINGLETON; }

        public Boolean visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
          return scopeAnnotation.equals(Singleton.class);
        }

        public Boolean visitNoScoping() { return false; }
      }), "Managed service must be bound as Singleton", binding);
    }

    return ServiceDependencyManager.buildServicesWithDependencies(
            ResolvedBinding.<BaseService<?>>resolver(injector)
                           .andThen(ResolvedBinding::get)
                           .transform(serviceKeys)
                           .toList(),
            Functional.funPairs(graph.entries())
                    .mapKeys(ResolvedBinding::get)
                    .mapValues((F<ResolvedBinding<? extends BaseService<?>>, BaseService<?>>) binding -> binding.get())
                    .toList()
    );
  }
}
