package joshng.util.converters;

import com.google.common.base.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

/**
* User: josh
* Date: 5/14/14
* Time: 12:37 PM
*/
public class UuidByteConverter extends Converter<UUID, byte[]> {
  public static final UuidByteConverter INSTANCE = new UuidByteConverter();
  private static final int BYTE_LENGTH = 2 * Long.BYTES;
  public UuidByteConverter() {}


  @Override
  protected byte[] doForward(UUID identifier) {
    return ByteBuffer.allocate(BYTE_LENGTH)
            .putLong(identifier.getMostSignificantBits())
            .putLong(identifier.getLeastSignificantBits())
            .array();
  }

  @Override
  protected UUID doBackward(byte[] identifier) {
    int length = identifier.length;
    checkArgument(length == BYTE_LENGTH, "Unexpected identifier length %s (expected %s for UUID key)", length, BYTE_LENGTH);
    ByteBuffer buf = ByteBuffer.wrap(identifier);
    long msb = buf.getLong();
    long lsb = buf.getLong();
    return new UUID(msb, lsb);
  }
}
