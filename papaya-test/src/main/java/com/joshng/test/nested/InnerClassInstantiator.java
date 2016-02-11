package com.joshng.test.nested;

import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Instantiates a class which may be an inner class of arbitrary depth,
 * given that the class and all of its outer classes have an accessible
 * default constructor.
 */
class InnerClassInstantiator {
    @SuppressWarnings("unchecked")
    public <T> T instantiate(Class<T> aClass)
            throws IllegalAccessException, InvocationTargetException,
            InstantiationException {
        ArrayList<Class<?>> classes = getNestedClasses(aClass);
        return (T) instantiateNestedClasses(classes);
    }

    private Object instantiateNestedClasses(Iterable<Class<?>> classes)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Object[] lastInstance = new Object[0];
        for (Class<?> aClass : classes) {
            Constructor<?>[] constructors = aClass.getConstructors();
            Assert.assertEquals("Test class should be public, with one empty constructor: " + aClass.getName(), 1, constructors.length);
            Object instance = constructors[0].newInstance(lastInstance);
            lastInstance = new Object[]{instance};
        }

        return lastInstance[0];
    }

    private ArrayList<Class<?>> getNestedClasses(Class<?> aClass) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        do {
            classes.add(0, aClass);
            aClass = aClass.getDeclaringClass();
        } while (aClass != null);
        return classes;
    }
}
