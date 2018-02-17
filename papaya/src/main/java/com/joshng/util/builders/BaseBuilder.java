package com.joshng.util.builders;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.joshng.util.blocks.Source;
import com.joshng.util.collect.PersistentMap;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: Aug 15, 2011
 * Time: 11:36:27 PM
 */
@Immutable
public abstract class BaseBuilder<T, SELF extends BaseBuilder<T, SELF>> implements Cloneable {
  private static final Source EMPTY_LIST_SUPPLIER = Source.<List>ofInstance(ImmutableList.of());

  PersistentMap<BuilderProperty, Supplier> properties = PersistentMap.empty();

  public class Property<P> extends BuilderProperty<P, Property<P>> {
  }

  public class DurationProperty extends BuilderProperty<Long, DurationProperty> {
    public PropertyValue<T, Long> of(long duration, TimeUnit timeUnit) {
      return as(timeUnit.toMillis(duration));
    }

    public PropertyValue<T, Long> of(Duration duration) {
      return of(duration.toMillis());
    }

    public DurationProperty withDefault(long duration, TimeUnit timeUnit) {
      return withDefault(timeUnit.toMillis(duration));
    }

    public Long getMillis() {
      return get(this);
    }
  }

  public class PositiveIntProperty extends BuilderProperty<Integer, PositiveIntProperty> {
    @Override
    protected void validate(Integer value) {
      checkArgument(value > 0, "Value must be positive", value);
    }
  }

  public class ListProperty<V> extends BuilderProperty<List<V>, ListProperty<V>> {
    @SuppressWarnings("unchecked")
    public ListProperty() {
      super((Supplier<List<V>>) EMPTY_LIST_SUPPLIER);
    }

    public PropertyValue<T, List<V>> listOf(V v1) {
      return of(ImmutableList.of(v1));
    }

    public PropertyValue<T, List<V>> listOf(V v1, V v2) {
      return of(ImmutableList.of(v1, v2));
    }

    public PropertyValue<T, List<V>> listOf(V v1, V v2, V v3) {
      return of(ImmutableList.of(v1, v2, v3));
    }

    @SafeVarargs
    public final PropertyValue<T, List<V>> listOf(V... values) {
      return of(Arrays.asList(values));
    }
  }

  public SELF with(Iterable<? extends PropertyValue<? extends T, ?>> propertyValues) {
    PersistentMap<BaseBuilder.BuilderProperty, Supplier> allProperties = properties;
    for (PropertyValue<? extends T, ?> propertyValue : propertyValues) {
      allProperties = allProperties.with(propertyValue.getProperty(), propertyValue.getValueSupplier());
    }
    try {
      @SuppressWarnings("unchecked") SELF clone = (SELF) clone();
      clone.properties = allProperties;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  public SELF with(PropertyValue<T, ?> propertyValue) {
    return with(ImmutableList.<PropertyValue<T, ?>>of(propertyValue));
  }

  public SELF with(PropertyValue<T, ?> first, PropertyValue<T, ?>... propertyValues) {
    return with(Lists.asList(first, propertyValues));
  }

  protected <P> Property<P> property() {
    return new Property<>();
  }

  protected <P> Property<P> propertyWithDefault(P defaultValue) {
    return this.<P>property().withDefault(defaultValue);
  }

  protected <P> Property<P> propertyWithDefaultFrom(Supplier<? extends P> supplier) {
    return this.<P>property().withDefaultFrom(supplier);
  }

  protected DurationProperty duration() {
    return new DurationProperty();
  }

  protected DurationProperty durationWithDefault(long defaultDuration, TimeUnit timeUnit) {
    return duration().withDefault(defaultDuration, timeUnit);
  }

  protected <V> ListProperty<V> listProperty() {
    return new ListProperty<V>();
  }

  @SuppressWarnings({"unchecked"})
  protected <V> V get(BuilderProperty<V, ?> property) {
    return get(property, property.getDefaultSupplier());
  }

  @SuppressWarnings({"unchecked"})
  protected <V> V get(BuilderProperty<V, ?> property, V defaultValue) {
    return get(property, Source.ofInstance(defaultValue));
  }

  @SuppressWarnings({"unchecked"})
  protected <V> V get(BuilderProperty<V, ?> property, Supplier<? extends V> defaultValueSupplier) {
    Supplier<? extends V> supplier = properties.get(property);
    if (supplier == null) supplier = firstNonNull(defaultValueSupplier, Source.<V>nullSource());
    return supplier.get();
  }

  public class BuilderProperty<V, SELF extends BuilderProperty<V, SELF>> implements Cloneable {
    Supplier<? extends V> defaultSupplier;

    public BuilderProperty() {
      this(Source.<V>nullSource());
    }

    public BuilderProperty(Supplier<? extends V> defaultSupplier) {
      this.defaultSupplier = defaultSupplier;
    }

    public SELF withDefault(V defaultValue) {
      return withDefaultFrom(Source.ofInstance(defaultValue));
    }

    public SELF withDefaultFrom(Supplier<? extends V> defaultValueSupplier) {
      SELF clone = copy();
      clone.defaultSupplier = defaultValueSupplier;
      return clone;
    }

    @SuppressWarnings("unchecked")
    protected SELF copy() {
      try {
        return (SELF) clone();
      } catch (CloneNotSupportedException e) {
        throw Throwables.propagate(e);
      }
    }

    /**
     * override this to apply validation of inputs: throw an exception for invalid values.
     * <p>IMPORTANT: note that the validation will only apply when explicit values are configured with {@link #as}
     * or {@link #of}.
     * <p>values configured via Suppliers via {@link #from} will not be validated via this method!
     *
     * @param value
     */
    protected void validate(V value) {
    }

    public PropertyValue<T, V> of(V value) {
      validate(value);
      return from(Source.ofInstance(value));
    }

    /**
     * alias for {@link BuilderProperty#of}
     *
     * @param value
     * @return
     */
    public PropertyValue<T, V> as(V value) {
      return of(value);
    }


    public PropertyValue<T, V> from(Supplier<? extends V> valueSupplier) {
      return new PropertyValue<T, V>(this, valueSupplier);
    }

    Supplier<? extends V> getDefaultSupplier() {
      return defaultSupplier;
    }
  }

  public static class PropertyValue<T, V> {
    private final BaseBuilder<T, ?>.BuilderProperty<V, ?> property;
    private final Supplier<? extends V> valueSupplier;

    public PropertyValue(BaseBuilder<T, ?>.BuilderProperty<V, ?> property, Supplier<? extends V> valueSupplier) {
      this.property = property;
      this.valueSupplier = valueSupplier;
    }

    public BaseBuilder<T, ?>.BuilderProperty<V, ?> getProperty() {
      return property;
    }

    public Supplier<? extends V> getValueSupplier() {
      return valueSupplier;
    }
  }
}
