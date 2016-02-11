package com.joshng.util;

import com.joshng.util.collect.Maybe;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * User: josh
 * Date: Jul 15, 2011
 * Time: 12:09:53 PM
 */
public class Localhost {
  public static final String UNKNOWN_HOST = "UNKNOWN_HOST";

  private static final Info INSTANCE = buildInfo();

  private static Info buildInfo() {
    try {
      return new Info(InetAddress.getLocalHost());
    } catch (UnknownHostException e) {
      return new Info();
    }
  }

  public static String getHostName() {
    return INSTANCE.hostName;
  }

  public static String getCanonicalHostName() {
    return INSTANCE.canonicalHostName;
  }

  public static String getDescription() {
    return INSTANCE.description;
  }

  public static Maybe<byte[]> getAddressBytes() {
    return INSTANCE.address;
  }

  private static class Info {
    final String hostName;
    final String canonicalHostName;
    final String description;
    final Maybe<byte[]> address;

    private Info(String hostName, String canonicalHostName, String description, Maybe<byte[]> address) {
      this.hostName = hostName;
      this.canonicalHostName = canonicalHostName;
      this.description = description;
      this.address = address;
    }

    private Info() {
      this(UNKNOWN_HOST, UNKNOWN_HOST, UNKNOWN_HOST, Maybe.not());
    }

    private Info(InetAddress address) {
      this(address.getHostName(), address.getCanonicalHostName(), address.toString(), Maybe.of(address.getAddress()));
    }
  }
}
