package joshng.util.converters;

import com.google.common.base.Converter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import joshng.util.ByteSerializable;
import joshng.util.Reflect;
import joshng.util.blocks.F;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:06 PM
 */
public abstract class ByteConverter<T> extends Converter<T, byte[]> implements F<T, byte[]> {
  public static final ByteConverter<byte[]> IDENTITY = new Identity();

  private static final Map<Class<?>, ByteConverter<?>> BY_TYPE = ImmutableMap.of(
          Integer.class, IntByteConverter.INSTANCE,
          Long.class, LongByteConverter.INSTANCE,
          String.class, StringUtf8Converter.INSTANCE,
          UUID.class, UuidByteConverter.INSTANCE,
          byte[].class, IDENTITY
  );

  private static final LoadingCache<Class<? extends ByteSerializable>, ByteConverter<?>> BYTE_SERIALIZABLE_CONVERTERS = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends ByteSerializable>, ByteConverter<?>>() {
    @Override
    public ByteConverter<?> load(Class<? extends ByteSerializable> key) throws Exception {
      return getByteSerializableConverter(key, ByteSerializable.getRepresentativeType(key));
    }
  });

  public static ByteConverter<byte[]> identity() {
    return IDENTITY;
  }

  @SuppressWarnings("unchecked")
  public static <T> ByteConverter<T> forType(Class<T> conversionType) {
    if (ByteSerializable.class.isAssignableFrom(conversionType)) {
      return (ByteConverter<T>) getByteSerializableConverter(conversionType.asSubclass(ByteSerializable.class));
    }
    return (ByteConverter<T>)checkNotNull(BY_TYPE.get(conversionType), "No ByteConverter registered for type: %s", conversionType);
  }

  @SuppressWarnings("unchecked")
  public static <K extends ByteSerializable> ByteConverter<K> getByteSerializableConverter(Class<K> serializableType) {
    return (ByteConverter<K>) BYTE_SERIALIZABLE_CONVERTERS.getUnchecked(serializableType);
  }

  private static <K extends ByteSerializable<I>, I> ByteConverter<K> getByteSerializableConverter(Class<K> keyType, Class<I> identifierType) {
    Constructor<K> constructor = Reflect.getConstructor(keyType, identifierType);
    constructor.setAccessible(true);
    ByteConverter<I> byteConverter = forType(identifierType);
    return byteConverter.compose(new Converter<K, I>() {
      @Override
      protected I doForward(K k) {
        return k.getIdentifier();
      }

      @Override
      protected K doBackward(I i) {
        try {
          return constructor.newInstance(i);
        } catch (InstantiationException | IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw Throwables.propagate(e.getCause());
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
