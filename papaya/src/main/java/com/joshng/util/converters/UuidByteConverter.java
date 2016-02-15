package com.joshng.util.converters;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 5/14/14
 * Time: 12:37 PM
 */
public class UuidByteConverter extends ByteConverter<UUID> {
  public static final UuidByteConverter INSTANCE = new UuidByteConverter();
  public static final int BYTE_LENGTH = 2 * Long.BYTES;

  public UuidByteConverter() {
  }

  @Override
  protected byte[] doForward(UUID identifier) {
    return toBytes(identifier);
  }

  public static byte[] toBytes(UUID identifier) {
    return putBytes(ByteBuffer.allocate(BYTE_LENGTH), identifier).array();
  }

  @Override
  protected UUID doBackward(byte[] identifier) {
    return toUuid(identifier);
  }

  public static ByteBuffer putBytes(ByteBuffer buf, UUID identifier) {
    return buf.putLong(identifier.getMostSignificantBits())
              .putLong(identifier.getLeastSignificantBits());
  }

  public static UUID toUuid(byte[] identifier) {
    int length = identifier.length;
    checkArgument(length == BYTE_LENGTH, "Unexpected identifier length %s (expected %s for UUID key)", length, BYTE_LENGTH);
    return toUuid(identifier, 0);
  }

  public static UUID toUuid(byte[] identifier, int offset) {
    ByteBuffer buf = ByteBuffer.wrap(identifier, offset, BYTE_LENGTH);
    long msb = buf.getLong();
    long lsb = buf.getLong();
    return new UUID(msb, lsb);
  }
}
