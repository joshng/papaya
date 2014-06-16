package joshng.util;

import joshng.util.collect.ImmutableMaybeMap;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: Apr 15, 2011
 * Time: 6:58:21 PM
 */
public abstract class Identifier<T> implements Serializable, ByteSerializable<T> {
  protected final T identifier;

  public Identifier(T identifier) {
    checkNotNull(identifier, "identifier");
    this.identifier = identifier;
  }

  public T getIdentifier() {
    return identifier;
  }

  public T getSerializableValue() {
    return identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Identifier that = (Identifier) o;

    return identifier.equals(that.identifier);
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public String toString() {
    return identifier.toString();
  }

  public static class SealedRegistry<I extends Identifier> extends ImmutableMaybeMap<String, I> {

    public SealedRegistry(Iterable<I> entries) {
      super(extend(entries).asValuesFrom(Object::toString));
    }

    public boolean isValid(String key) {
      return containsKey(key);
    }
  }
}
