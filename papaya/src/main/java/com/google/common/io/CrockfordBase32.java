package com.google.common.io;

/**
 * User: josh
 * Date: 12/16/14
 * Time: 10:01 AM
 */
public class CrockfordBase32 {
  public static final BaseEncoding CROCKFORD_BASE_32_ENCODING = new BaseEncoding.StandardBaseEncoding("base32Hex()", "0123456789ABCDEFGHJKMNPQRSTVWXYZ", '=');

  public static BaseEncoding getInstance() {
    return CROCKFORD_BASE_32_ENCODING;
  }
}
