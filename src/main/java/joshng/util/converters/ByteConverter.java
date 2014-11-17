package joshng.util.converters;

import com.google.common.base.Converter;
import joshng.util.blocks.F;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:06 PM
 */
public abstract class ByteConverter<T> extends Converter<T, byte[]> implements F<T, byte[]> {
  public static final ByteConverter<byte[]> IDENTITY = new Identity();


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
