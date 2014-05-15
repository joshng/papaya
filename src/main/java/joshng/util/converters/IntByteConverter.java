package joshng.util.converters;

import com.google.common.base.Converter;
import com.google.common.primitives.Ints;

/**
 * User: josh
 * Date: 5/14/14
 * Time: 11:50 AM
 */
public class IntByteConverter extends Converter<Integer, byte[]> {
  public static final IntByteConverter INSTANCE = new IntByteConverter();

  public IntByteConverter() {
  }

  @Override
  protected byte[] doForward(Integer integer) {
    return Ints.toByteArray(integer);
  }


  @Override
  protected Integer doBackward(byte[] bytes) {
    return Ints.fromByteArray(bytes);
  }


}
