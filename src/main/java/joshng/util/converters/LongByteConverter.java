package joshng.util.converters;

import com.google.common.base.Converter;
import com.google.common.primitives.Longs;

/**
 * User: josh
 * Date: 5/14/14
 * Time: 11:51 AM
 */
public class LongByteConverter extends Converter<Long, byte[]> {
  public static LongByteConverter INSTANCE = new LongByteConverter();

  private LongByteConverter() {
  }

  @Override
  protected byte[] doForward(Long value) {
    return Longs.toByteArray(value);
  }


  @Override
  protected Long doBackward(byte[] bytes) {
    return Longs.fromByteArray(bytes);
  }
}
