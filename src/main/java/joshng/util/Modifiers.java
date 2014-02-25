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
        protected boolean matches(int modifiers) { return (modifiers & Modifier.ABSTRACT) == 0; }
    };

    private final int flag;

    public final Pred<Class<?>> CLASS_PREDICATE = new Pred<Class<?>>() { public boolean apply(Class<?> input) {
        return matches(input);
    } };
    public final Pred<Method> METHOD_PREDICATE = new Pred<Method>() { public boolean apply(Method input) {
        return matches(input);
    } };
    public final Pred<Field> FIELD_PREDICATE = new Pred<Field>() { public boolean apply(Field input) {
        return matches(input);
    } };
    
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
