package joshng.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * User: josh
 * Date: Jul 15, 2011
 * Time: 12:09:53 PM
 */
public class Localhost {
  public static final String UNKNOWN_HOST = "UNKNOWN_HOST";

  private static final Info INFO = buildInfo();

  private static Info buildInfo() {
    try {
      return new Info(InetAddress.getLocalHost());
    } catch (UnknownHostException e) {
      return new Info();
    }
  }

  public static String getHostName() {
    return INFO.hostName;
  }

  public static String getCanonicalHostName() {
    return INFO.canonicalHostName;
  }

  public static String getDescription() {
    return INFO.description;
  }

  private static class Info {
    String hostName;
    String canonicalHostName;
    String description;

    private Info(String hostName, String canonicalHostName, String description) {
      this.hostName = hostName;
      this.canonicalHostName = canonicalHostName;
      this.description = description;
    }

    private Info() {
      this(UNKNOWN_HOST, UNKNOWN_HOST, UNKNOWN_HOST);
    }

    private Info(InetAddress address) {
      this(address.getHostName(), address.getCanonicalHostName(), address.toString());
    }
  }
}
