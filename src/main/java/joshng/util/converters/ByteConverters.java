package joshng.util.converters;

import com.google.common.base.Converter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import joshng.util.ByteSerializable;
import joshng.util.Modifiers;
import joshng.util.Reflect;
import joshng.util.blocks.Pred;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Maybe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 11/13/14
 * Time: 9:37 PM
 */
public class ByteConverters {
  public static final String DESERIALIZER_ANNOTATION_NAME = "@" + ByteSerializable.Deserializer.class.getSimpleName();
  private static final LoadingCache<Class<?>, Converter<?, byte[]>> BY_TYPE = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Converter<?, byte[]>>() {
    @Override
    public Converter<?, byte[]> load(Class<?> key) throws Exception {
      Class<? extends ByteSerializable> byteSerializableClass = Maybe.asSubclass(key, ByteSerializable.class).getOrThrow("No ByteConverter registered for non-ByteSerializable type", key);
      return getByteSerializableConverter(byteSerializableClass);
    }
  });

  static {
    ByteConverters.register(Short.class, ShortByteConverter.INSTANCE);
    ByteConverters.register(Integer.class, IntByteConverter.INSTANCE);
    ByteConverters.register(Long.class, LongByteConverter.INSTANCE);
    ByteConverters.register(Instant.class, InstantByteConverter.INSTANCE);
    ByteConverters.register(String.class, StringUtf8Converter.INSTANCE);
    ByteConverters.register(UUID.class, UuidByteConverter.INSTANCE);
    ByteConverters.register(ByteBuffer.class, ByteBufferConverter.INSTANCE);
    ByteConverters.register(byte[].class, ByteConverter.IDENTITY);
  }

  public static <T> void register(Class<T> convertibleType, Converter<? super T, byte[]> converter) {
    BY_TYPE.put(convertibleType, converter);
  }

  @SuppressWarnings("unchecked")
  public static <T> Converter<T, byte[]> forType(Class<T> conversionType) {
    return (Converter<T, byte[]>) BY_TYPE.getUnchecked(conversionType);
  }

  private static <I, T extends ByteSerializable<I>> Converter<T, byte[]> getByteSerializableConverter(Class<T> serializableType) {
    Class<I> identifierType = ByteSerializable.getRepresentativeType(serializableType);
    Converter<I, byte[]> byteConverter = forType(identifierType);

    FunIterable<Method> deserializerMethods = extend(serializableType.getDeclaredMethods()).filter(
            Pred.annotatedWith(
                    ByteSerializable.Deserializer.class)).toList();

    ThrowingFunction<I, T> deserializer;
    if (deserializerMethods.isEmpty()) {
      Constructor<T> constructor = Reflect.getConstructor(serializableType, identifierType);
      constructor.setAccessible(true);
      deserializer = constructor::newInstance;
    } else {
      Method method = deserializerMethods.getOnlyElement();
      checkArgument(Modifiers.Static.matches(method), DESERIALIZER_ANNOTATION_NAME + " method must be static", method);
      Class<?> paramType = Primitives.wrap(extend(method.getParameters()).getOnlyElement().getType());
      checkArgument(paramType.isAssignableFrom(identifierType), "Incorrect " + DESERIALIZER_ANNOTATION_NAME + " method signature; parameter should be of type %s", identifierType, method);
      method.setAccessible(true);
      deserializer = repr -> serializableType.cast(method.invoke(null, repr));
    }

    return new Converter<T, I>() {
      @Override
      protected I doForward(T k) {
        return k.getSerializableValue();
      }

      @Override
      protected T doBackward(I i) {
        try {
          return deserializer.apply(i);
        } catch (InvocationTargetException e) {
          throw Throwables.propagate(e.getCause());
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
    }.andThen(byteConverter);
  }
}
