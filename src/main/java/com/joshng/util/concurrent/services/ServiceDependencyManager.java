package com.joshng.util.concurrent.services;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.joshng.util.blocks.F;
import com.joshng.util.collect.PersistentList;
import com.joshng.util.concurrent.IntegratedService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 6/14/13
 * Time: 11:18 AM
 */
public class ServiceDependencyManager extends AggregateService {
  private final ImmutableMap<Service, ServiceWithDependencies> dependencies;
  private final Graph<Service> graph;

  public ServiceDependencyManager(ImmutableMap<Service, ServiceWithDependencies> dependencies) {
    this.dependencies = dependencies;
    Set<ServiceWithDependencies> validated = Sets.newHashSet();
    for (ServiceWithDependencies service : dependencies.values()) {
      checkForCycles(PersistentList.of(service), validated);
    }
    graph = buildGraph(dependencies.values());
  }

  private void checkForCycles(PersistentList<ServiceWithDependencies> context, Set<ServiceWithDependencies> validated) {
    for (ServiceWithDependencies requirement : context.head().getRequiredServices()) {
      if (!validated.contains(requirement)) {
        PersistentList<ServiceWithDependencies> nestedContext = context.with(requirement);
        if (context.contains(requirement)) {
          throw new IllegalArgumentException(
                  "Cyclic dependency!!\n  Consider refactoring to isolate responsibilities, or break the cycle by having one of these services\n  implement "
                          + IntegratedService.class.getSimpleName() + ".getExcludedServiceDependencies():\nCycle: "
                          + Joiner.on(" -> ")
                                  .join(extend(nestedContext.reversed()).map(ServiceWithDependencies::getUnderlyingService))
          );
        }
        checkForCycles(nestedContext, validated);
        validated.add(requirement);
      }
    }
  }

  public static ServiceDependencyManager buildServicesWithDependencies(
          Iterable<? extends BaseService<?>> rootServices,
          Iterable<? extends Map.Entry<? extends BaseService<?>, ? extends BaseService<?>>> dependencies
  ) {
    Map<BaseService<?>, ServiceWithDependencies> wrappers = new HashMap<>();
    F<BaseService<?>, ServiceWithDependencies> wrapperFunction = F.forMap(wrappers);

    Function<BaseService<?>, ServiceWithDependencies> factory = service -> new ServiceWithDependencies(service,
            wrapperFunction);

    for (BaseService<?> service : rootServices) {
      wrappers.computeIfAbsent(service, factory); // ensure the root services are wrapped
    }

    for (Map.Entry<? extends BaseService<?>, ? extends BaseService<?>> dependency : dependencies) {
      BaseService<?> dependent = dependency.getKey();
      BaseService<?> required = dependency.getValue();

      wrappers.computeIfAbsent(dependent, factory)
              .addRequiredServices(ImmutableList.of(wrappers.get(required)));
    }

    return new ServiceDependencyManager(ImmutableMap.copyOf(wrappers));
  }

  @Override protected Iterable<? extends BaseService<?>> getComponentServices() {
    return dependencies.values();
  }

  public String toString() {
    GraphLayouter<Service> layouter = new GraphLayouter<>();
    layouter.setVertical(false);
    return layouter.layout(graph);
  }

  @SuppressWarnings("unchecked") private static Graph<Service> buildGraph(Iterable<ServiceWithDependencies> services) {
    GraphBuilder<Service> builder = new GraphBuilder<>();
    for (ServiceWithDependencies serviceWithDeps : services) {
      Service service = serviceWithDeps.getUnderlyingService();
      builder.addVertex(service);
      for (ServiceWithDependencies requirement : serviceWithDeps.getRequiredServices()) {
        builder.addEdge(service, requirement.getUnderlyingService());
      }
    }

    return builder.build();
  }
}
