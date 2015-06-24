package joshng.util.converters;

import java.nio.ByteBuffer;

/**
 * User: josh
 * Date: 9/22/14
 * Time: 10:06 AM
 */
public class ByteBufferConverter extends ByteConverter<ByteBuffer> {
  public static final ByteBufferConverter INSTANCE = new ByteBufferConverter();

  public static byte[] unwrapByteBuffer(ByteBuffer buf) {
    if (buf.hasArray() && buf.array().length == buf.remaining()) {
      return buf.array();
    }
    byte[] copy = new byte[buf.remaining()];
    buf.mark();
    buf.get(copy);
    buf.reset();
    return copy;
  }

  @Override protected byte[] doForward(ByteBuffer byteBuffer) {
    return unwrapByteBuffer(byteBuffer);
  }

  @Override protected ByteBuffer doBackward(byte[] bytes) {
    return ByteBuffer.wrap(bytes);
  }
}
