package joshng.util;

import com.google.common.base.Charsets;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 4:27 PM
 */
public class StringIdentifier extends Identifier<String> {
  private static final Pattern HYPHEN = Pattern.compile("-");

  public StringIdentifier(String identifier) {
    super(identifier);
  }

  public static String generateRandomIdentifier() {
    return UUID.randomUUID().toString();
  }

  public static String generate32HexIdentifier() {
    return HYPHEN.matcher(generateRandomIdentifier()).replaceAll("");
  }

  @Override
  public byte[] toBytes() {
    return getIdentifier().getBytes(Charsets.UTF_8);
  }
}
