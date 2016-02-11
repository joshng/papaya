package com.joshng.util.converters;

import com.google.common.primitives.Shorts;

/**
 * User: josh
 * Date: 5/14/14
 * Time: 11:50 AM
 */
public class ShortByteConverter extends ByteConverter<Short> {
  public static final ShortByteConverter INSTANCE = new ShortByteConverter();

  public ShortByteConverter() {
  }

  @Override
  protected byte[] doForward(Short integer) {
    return Shorts.toByteArray(integer);
  }


  @Override
  protected Short doBackward(byte[] bytes) {
    return Shorts.fromByteArray(bytes);
  }
}
