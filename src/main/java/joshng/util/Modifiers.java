package joshng.util;

import joshng.util.blocks.Pred;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

  public final Pred<Class<?>> CLASS_PREDICATE = this::matches;
  public final Pred<Method> METHOD_PREDICATE = this::matches;
  public final Pred<Field> FIELD_PREDICATE = this::matches;

  Modifiers(int flag) {
    this.flag = flag;
  }

  public boolean matches(Class<?> c) {
    return matches(c.getModifiers());
  }

  public boolean matches(Method method) {
    return matches(method.getModifiers());
  }

  public boolean matches(Field field) {
    return matches(field.getModifiers());
  }

  protected boolean matches(int modifiers) {
    return (modifiers & flag) != 0;
  }
}
