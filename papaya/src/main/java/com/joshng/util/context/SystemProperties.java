package com.joshng.util.context;

/**
 * User: josh
 * Date: 10/8/14
 * Time: 10:07 AM
 */
public class SystemProperties {
  public static TransientContext contextWithValue(String propertyName, String value) {
    return new ContextWithValue(propertyName, value);
  }

  static class ContextWithValue implements TransientContext {
    private final String key;
    private final String value;

    ContextWithValue(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override public State enter() {
      String prevValue = System.getProperty(key);
      System.setProperty(key, value);
      return prevValue != null
              ? () -> System.setProperty(key, prevValue)
              : () -> System.clearProperty(key);
    }
  }
}
