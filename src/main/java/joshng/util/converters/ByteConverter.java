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
import joshng.util.blocks.F;
import joshng.util.blocks.Pred;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Maybe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:06 PM
 */
public abstract class ByteConverter<T> extends Converter<T, byte[]> implements F<T, byte[]> {
  public static final ByteConverter<byte[]> IDENTITY = new Identity();

  private static final LoadingCache<Class<?>, ByteConverter<?>> BY_TYPE = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, ByteConverter<?>>() {
    @Override
    public ByteConverter<?> load(Class<?> key) throws Exception {
      Class<? extends ByteSerializable> byteSerializableClass = Maybe.asSubclass(key, ByteSerializable.class).getOrThrow("No ByteConverter registered for non-ByteSerializable type", key);
      return getByteSerializableConverter(byteSerializableClass);
    }
  });
  public static final String DESERIALIZER_ANNOTATION_NAME = "@" + ByteSerializable.Deserializer.class.getSimpleName();

  public static <T> void register(Class<T> convertibleType, ByteConverter<? super T> converter) {
    BY_TYPE.put(convertibleType, converter);
  }

  static {
    register(Short.class, ShortByteConverter.INSTANCE);
    register(Integer.class, IntByteConverter.INSTANCE);
    register(Long.class, LongByteConverter.INSTANCE);
    register(String.class, StringUtf8Converter.INSTANCE);
    register(UUID.class, UuidByteConverter.INSTANCE);
    register(byte[].class, IDENTITY);
  }

  public static ByteConverter<byte[]> identity() {
    return IDENTITY;
  }


  @SuppressWarnings("unchecked")
  public static <T> ByteConverter<T> forType(Class<T> conversionType) {
    return (ByteConverter<T>) BY_TYPE.getUnchecked(conversionType);
  }

  private static <I, T extends ByteSerializable<I>> ByteConverter<T> getByteSerializableConverter(Class<T> serializableType) {
    Class<I> identifierType = ByteSerializable.getRepresentativeType(serializableType);
    ByteConverter<I> byteConverter = forType(identifierType);

    FunIterable<Method> deserializerMethods = extend(serializableType.getDeclaredMethods()).filter(Pred.annotatedWith(ByteSerializable.Deserializer.class)).toList();

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

    return byteConverter.compose(new Converter<T, I>() {
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
    });
  }

  public <U> ByteConverter<U> compose(Converter<U, T> first) {
    return new ForwardingByteConverter<>(first.andThen(this));
  }

  private static class ForwardingByteConverter<T> extends ByteConverter<T> {
    private final Converter<T, byte[]> delegate;

    private ForwardingByteConverter(Converter<T, byte[]> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected byte[] doForward(T t) {
      return delegate.convert(t);
    }

    @Override
    protected T doBackward(byte[] bytes) {
      return delegate.reverse().convert(bytes);
    }
  }

  public static class Identity extends ByteConverter<byte[]> {
    private Identity() {}

    @Override
    protected byte[] doForward(byte[] bytes) {
      return bytes;
    }

    @Override
    protected byte[] doBackward(byte[] bytes) {
      return bytes;
    }

    @Override
    public ByteConverter<byte[]> reverse() {
      return this;
    }

    @Override
    public <C> Converter<byte[], C> andThen(Converter<byte[], C> secondConverter) {
      return secondConverter;
    }
  }
}
