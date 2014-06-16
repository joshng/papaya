package joshng.util;

import com.google.common.reflect.TypeToken;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:25 PM
 */

/**
 * Implementations of ByteSerializable must also provide either a one-argument constructor
 * accepting an instance of type {@code T}, or a static method annotated with @{@link joshng.util.ByteSerializable.Deserializer}
 * that accepts the same.
 * @param <T> the "representative type", which must be a type with a converter registered with
 *           the {@link joshng.util.converters.ByteConverter#register(Class, joshng.util.converters.ByteConverter)} ()} method
 */
public interface ByteSerializable<T> {
  Type SERIALIZABLE_VALUE_TYPE = Reflect.getMethod(ByteSerializable.class, "getIdentifier").getGenericReturnType();

  @SuppressWarnings("unchecked")
  static <T, S extends ByteSerializable<T>> Class<T> getRepresentativeType(Class<S> serializableClass) {
    return (Class<T>) TypeToken.of(serializableClass).getSupertype(ByteSerializable.class).resolveType(SERIALIZABLE_VALUE_TYPE).getRawType();
  }

  T getSerializableValue();

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Deserializer { }
}
