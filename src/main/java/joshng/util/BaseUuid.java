package joshng.util;

import joshng.util.converters.UuidByteConverter;

import java.util.UUID;

/**
 * User: josh
 * Date: 5/27/14
 * Time: 4:30 PM
 */
public class BaseUuid extends Identifier<UUID> {
  public BaseUuid(UUID identifier) {
    super(identifier);
  }

  public BaseUuid(byte[] bytes) {
    this(UuidByteConverter.toUuid(bytes));
  }

  public byte[] getBytes() {
    return UuidByteConverter.toBytes(getIdentifier());
  }
}
