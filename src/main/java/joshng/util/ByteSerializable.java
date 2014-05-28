package joshng.util;

import com.google.common.reflect.TypeToken;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:25 PM
 */
public interface ByteSerializable<T> {
  static Class<?> getRepresentativeType(Class<? extends ByteSerializable> serializableClass) {
    return TypeToken.of(serializableClass).getSupertype(ByteSerializable.class).resolveType(Reflect.getMethod(ByteSerializable.class, "getIdentifier").getGenericReturnType()).getRawType();
  }

  byte[] toBytes();

  T getIdentifier();
}
