package joshng.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * User: josh
 * Date: Oct 26, 2010
 * Time: 10:34:45 AM
 * <p>
 * Properties that defer to System.getProperty() (without including all system properties in propertyNames())
 * <p>
 * This is useful to allow file-based configuration properties to be overridden by command-line definitions
 * (eg, -Dproperty=value).
 */
public class SystemOverridableProperties extends Properties {
  public SystemOverridableProperties() {
  }

  public SystemOverridableProperties(Properties defaults) {
    super(defaults);
  }

  @Override
  public String getProperty(String key) {
    return System.getProperty(key, super.getProperty(key));
  }

  public static String get(String key, Properties properties) {
    return System.getProperty(key, properties.getProperty(key));
  }

  public static SystemOverridableProperties load(Iterable<URL> bundleUrls) throws IOException {
    return new SystemOverridableProperties(PropertiesUtils.load(bundleUrls));
  }
}
