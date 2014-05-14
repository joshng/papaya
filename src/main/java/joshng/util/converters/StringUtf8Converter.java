package joshng.util.converters;

import com.google.common.base.Charsets;
import com.google.common.base.Converter;

/**
* User: josh
* Date: 5/14/14
* Time: 12:35 PM
*/
public class StringUtf8Converter extends Converter<String, byte[]> {
  public static final StringUtf8Converter INSTANCE = new StringUtf8Converter();
  private StringUtf8Converter() {}

  @Override
  protected byte[] doForward(String string) {
    return string.getBytes(Charsets.UTF_8);
  }

  @Override
  protected String doBackward(byte[] bytes) {
    return new String(bytes, Charsets.UTF_8);
  }
}
