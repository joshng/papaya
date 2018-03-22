package com.joshng.util.guice;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.joshng.util.blocks.F;
import com.joshng.util.blocks.Pred;
import com.joshng.util.blocks.Source;
import com.joshng.util.collect.FunSet;
import com.joshng.util.collect.Functional;
import com.joshng.util.collect.PersistentList;

import java.util.Set;


/**
 * User: josh
 * Date: 6/24/13
 * Time: 9:14 AM
 */
public class ResolvedBinding<T> implements Source<T> {
  public static <T> F<Key<? extends T>, ResolvedBinding<T>> resolver(final Injector injector) {
    return input -> resolve(input, injector);
  }

  private final Binding<? extends T> binding;
  private final Injector injector;

  private ResolvedBinding(Binding<? extends T> binding, Injector injector) {
    this.binding = binding;
    this.injector = injector;
  }

  public static <T> ResolvedBinding<T> resolve(final Key<? extends T> key, final Injector injector) {
    return injector.getBinding(key).acceptTargetVisitor(new ResolvingBindingTargetVisitor<T>(injector));
  }

  public Set<ResolvedBinding<?>> findTransitiveDependencies(final Predicate<? super ResolvedBinding<?>> filter) {
    final Set<ResolvedBinding<?>> result = Sets.newHashSet();

    PersistentList<ResolvedBinding<?>> context = PersistentList.<ResolvedBinding<?>>of(this);
    traverseDependencies(this, context, Predicates.and(Pred.newDeduplicator(), new Pred<ResolvedBinding<?>>() {
      public boolean apply(ResolvedBinding<?> input) {
        boolean matches = filter.apply(input);
        if (matches) result.add(input);
        return !matches;
      }
    }));

    return result;
  }

  private void traverseDependencies(ResolvedBinding<?> binding, PersistentList<ResolvedBinding<?>> context, Predicate<? super ResolvedBinding<?>> visitor) {
    try {
      for (ResolvedBinding<?> dependency : binding.getDirectDependencies()) {
        if (visitor.apply(dependency)) {
          traverseDependencies(dependency, context.with(dependency), visitor);
        }
      }
    } catch (BindingResolutionException e) {
      throw e;
    } catch (Exception e) {
      throw new BindingResolutionException(context, e);
    }
  }

  public Set<ResolvedBinding<?>> getDirectDependencies() {
    return binding.acceptTargetVisitor(new DependencyResolvingVisitor());
  }

  private static FunSet<ResolvedBinding<?>> getDependencyBindings(HasDependencies binding, final Injector injector) {
    return Functional.extend(binding.getDependencies()).<ResolvedBinding<?>>map(dependency -> resolve(dependency.getKey(), injector)).toSet();
  }

  public Binding<? extends T> getBinding() {
    return binding;
  }

  public Object getSource() {
    return getBinding().getSource();
  }

  public String toString() {
    return getBinding().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResolvedBinding that = (ResolvedBinding) o;

    // use reference-equality: binding.equals() is incorrect for similar bindings from PrivateModules
    return binding == that.binding;
  }

  @Override
  public int hashCode() {
    return binding.hashCode();
  }

  @Override
  public T get() {
    // note: don't use binding.getProvider().get(), it doesn't honor scoping rules (eg, SINGLETON)
    return injector.getInstance(binding.getKey());
  }

  private static class ResolvingBindingTargetVisitor<T> extends DefaultBindingTargetVisitor<T, ResolvedBinding<T>> {
    private final Injector injector;

    public ResolvingBindingTargetVisitor(Injector injector) {
      this.injector = injector;
    }

    @Override
    public ResolvedBinding<T> visit(ProviderKeyBinding<? extends T> providerKeyBinding) {
      //noinspection unchecked
      return injector.getBinding(((Key<? extends T>)providerKeyBinding.getProviderKey())).acceptTargetVisitor(this);
    }

    @Override
    public ResolvedBinding<T> visit(LinkedKeyBinding<? extends T> linkedKeyBinding) {
      return traverse(linkedKeyBinding.getLinkedKey());
    }

    @Override
    public ResolvedBinding<T> visit(ExposedBinding<? extends T> exposedBinding) {
      return resolve(exposedBinding.getKey(), exposedBinding.getPrivateElements().getInjector());
    }

    @Override
    public ResolvedBinding<T> visit(ProviderBinding<? extends T> providerBinding) {
      //noinspection unchecked
      return traverse((Key<? extends T>)providerBinding.getProvidedKey());
    }

    @Override
    protected ResolvedBinding<T> visitOther(Binding<? extends T> binding) {
      return new ResolvedBinding<T>(binding, injector);
    }

    private ResolvedBinding<T> traverse(Key<? extends T> nextKey) {
      return injector.getBinding(nextKey).acceptTargetVisitor(this);
    }
  }

  static class BindingResolutionException extends RuntimeException {
    public BindingResolutionException(PersistentList<ResolvedBinding<?>> context, Exception e) {
      super("Exception traversing dependencies for:\n  " + Joiner.on("\n  -> ").join(context.reversed()), e);
    }
  }

  private class DependencyResolvingVisitor extends DefaultBindingTargetVisitor<Object, Set<ResolvedBinding<?>>> implements MultibindingsTargetVisitor<Object, Set<ResolvedBinding<?>>> {
    @Override
    public Set<ResolvedBinding<?>> visit(ProviderInstanceBinding<?> providerInstanceBinding) {
      return getDependencyBindings(providerInstanceBinding);
    }

    @Override
    public Set<ResolvedBinding<?>> visit(ConstructorBinding<?> constructorBinding) {
      return getDependencyBindings(constructorBinding);
    }

    @Override
    public Set<ResolvedBinding<?>> visit(InstanceBinding<?> instanceBinding) {
      return getDependencyBindings(instanceBinding);
    }

    @Override
    protected Set<ResolvedBinding<?>> visitOther(Binding<?> binding) {
      return ImmutableSet.of();
    }

    private FunSet<ResolvedBinding<?>> getDependencyBindings(HasDependencies binding) {
      return ResolvedBinding.getDependencyBindings(binding, injector);
    }

    @Override
    public Set<ResolvedBinding<?>> visit(MultibinderBinding<?> multibinding) {
      return resolveMultiDependencies(multibinding.getElements());
    }

    // TODO: is this correct? why don't both ControllerServiceRegistry maps come through here??
    @Override
    public Set<ResolvedBinding<?>> visit(MapBinderBinding<?> mapbinding) {
      return resolveMultiDependencies(Functional.funPairs(mapbinding.getEntries()).values());
    }

    @Override public Set<ResolvedBinding<?>> visit(OptionalBinderBinding<?> optionalbinding) {
      throw new UnsupportedOperationException("DependencyResolvingVisitor.visit(OptionalBinderBinding) has not been implemented");
    }

    private Set<ResolvedBinding<?>> resolveMultiDependencies(Iterable<Binding<?>> elements) {
      return Functional.extend(elements).<ResolvedBinding<?>>map(input -> input.acceptTargetVisitor(new ResolvingBindingTargetVisitor<Object>(injector))).toSet();
    }

  }
}
