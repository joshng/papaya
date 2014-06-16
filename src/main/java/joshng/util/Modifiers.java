package joshng.util;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * User: josh
 * Date: 3/8/12
 * Time: 5:04 PM
 */
public enum Modifiers {
  Public(Modifier.PUBLIC),
  Private(Modifier.PRIVATE),
  Protected(Modifier.PROTECTED),
  Static(Modifier.STATIC),
  Final(Modifier.FINAL),
  Synchronized(Modifier.SYNCHRONIZED),
  Volatile(Modifier.VOLATILE),
  Transient(Modifier.TRANSIENT),
  Native(Modifier.NATIVE),
  Interface(Modifier.INTERFACE),
  Abstract(Modifier.ABSTRACT),
  Concrete(0) {
    protected boolean matches(int modifiers) {
      return (modifiers & Modifier.ABSTRACT) == 0;
    }
  };

  private final int flag;

  Modifiers(int flag) {
    this.flag = flag;
  }

  public boolean matches(Class<?> c) {
    return matches(c.getModifiers());
  }

  public boolean matches(Member field) {
    return matches(field.getModifiers());
  }

  protected boolean matches(int modifiers) {
    return (modifiers & flag) != 0;
  }
}
