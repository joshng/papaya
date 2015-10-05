package joshng.util.collect;

import joshng.util.ByteSerializable;
import joshng.util.StringUtils;
import joshng.util.converters.ByteBufferConverter;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * User: josh
 * Date: 5/1/14
 * Time: 7:06 PM
 */
public class ByteArrayKey implements ByteSerializable<byte[]> {
  private final byte[] bytes;
  private final int hashCode;

  public ByteArrayKey(byte[] bytes) {
    this.hashCode = Arrays.hashCode(bytes);
    this.bytes = bytes;
  }

  public static ByteArrayKey copyOf(ByteBuffer buffer) {
    return new ByteArrayKey(ByteBufferConverter.copyBytes(buffer));
  }

  public static ByteArrayKey valueOf(String hex) {
    return new ByteArrayKey(StringUtils.bytesFromHexString(hex));
  }

  public byte[] bytes() {
    return bytes;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && o.getClass() == getClass() && isEqual((ByteArrayKey)o);
  }

  private boolean isEqual(ByteArrayKey that) {
    return that.hashCode == hashCode && Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return StringUtils.toHexStringTruncatedWithEllipsis(bytes, 0, 32);
  }

  @Override
  public byte[] getSerializableValue() {
    return bytes;
  }

  public int getLength() {
    return bytes.length;
  }
}
