package com.joshng.util.converters;

import com.google.common.primitives.Longs;

import java.time.Instant;

/**
 * User: josh
 * Date: 5/14/14
 * Time: 11:50 AM
 */
public class InstantByteConverter extends ByteConverter<Instant> {
  public static final InstantByteConverter INSTANCE = new InstantByteConverter();

  public InstantByteConverter() {
  }

  @Override
  protected byte[] doForward(Instant instant) {
    return Longs.toByteArray(instant.toEpochMilli());
  }


  @Override
  protected Instant doBackward(byte[] bytes) {
    return Instant.ofEpochMilli(Longs.fromByteArray(bytes));
  }
}
