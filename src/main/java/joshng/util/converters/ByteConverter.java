package joshng.util.converters;

import com.google.common.base.Converter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import joshng.util.ByteSerializable;
import joshng.util.Reflect;
import joshng.util.blocks.F;
import joshng.util.collect.Maybe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

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
      return getByteSerializableConverter(byteSerializableClass, ByteSerializable.getRepresentativeType(byteSerializableClass));
    }
  });

  static {
    BY_TYPE.put(Integer.class, IntByteConverter.INSTANCE);
    BY_TYPE.put(Long.class, LongByteConverter.INSTANCE);
    BY_TYPE.put(String.class, StringUtf8Converter.INSTANCE);
    BY_TYPE.put(UUID.class, UuidByteConverter.INSTANCE);
    BY_TYPE.put(byte[].class, IDENTITY);
  }

  public static ByteConverter<byte[]> identity() {
    return IDENTITY;
  }

  public static <T> void register(Class<T> key, ByteConverter<? super T> value) {
    BY_TYPE.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public static <T> ByteConverter<T> forType(Class<T> conversionType) {
    return (ByteConverter<T>) BY_TYPE.getUnchecked(conversionType);
  }

  @SuppressWarnings("unchecked")
  public static <K extends ByteSerializable> ByteConverter<K> getByteSerializableConverter(Class<K> serializableType) {
    return (ByteConverter<K>) BY_TYPE.getUnchecked(serializableType);
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
