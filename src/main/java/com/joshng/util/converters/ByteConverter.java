package com.joshng.util.converters;

import com.google.common.base.Converter;
import com.joshng.util.blocks.F;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 5:06 PM
 */
public abstract class ByteConverter<T> extends Converter<T, byte[]> implements F<T, byte[]> {
  public static final Converter<byte[], byte[]> IDENTITY = new ForwardingByteConverter<>(Converter.identity());


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
  }
}
