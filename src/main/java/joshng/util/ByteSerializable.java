package joshng.util;

import com.google.common.reflect.TypeToken;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:25 PM
 */

/**
 * Implementations of ByteSerializable must also provide a one-argument constructor
 * accepting an instance of type {@code T}
 * @param <T> the "representative type", which must be one of the types directly supported
 *           by the {@link joshng.util.converters.ByteConverter#forType(Class)} method
 */
public interface ByteSerializable<T> {
  static Class<?> getRepresentativeType(Class<? extends ByteSerializable> serializableClass) {
    return TypeToken.of(serializableClass).getSupertype(ByteSerializable.class).resolveType(Reflect.getMethod(ByteSerializable.class, "getIdentifier").getGenericReturnType()).getRawType();
  }

  T getIdentifier();
}
