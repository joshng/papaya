package joshng.util.converters;

import java.nio.ByteBuffer;

/**
 * User: josh
 * Date: 9/22/14
 * Time: 10:06 AM
 */
public class ByteBufferConverter extends ByteConverter<ByteBuffer> {
  public static final ByteBufferConverter INSTANCE = new ByteBufferConverter();

  @Override protected byte[] doForward(ByteBuffer byteBuffer) {
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }

  @Override protected ByteBuffer doBackward(byte[] bytes) {
    return ByteBuffer.wrap(bytes);
  }
}
