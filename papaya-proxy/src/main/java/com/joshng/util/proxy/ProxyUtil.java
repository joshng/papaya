// Modified or written by Lambdascale SRL for inclusion with lambdaj.
// Copyright (c) 2009-2010 Mario Fusco.
// Licensed under the Apache License, Version 2.0 (the "License")

package com.joshng.util.proxy;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * ripped (and heavily pruned) from lambdaj, which ripped it from Mockito, which ripped it from jMock.
 * open source FTW! -joshg, 2012/04/23
 */
@SuppressWarnings("unchecked")
public final class ProxyUtil {

  private ProxyUtil() {
  }

  // ////////////////////////////////////////////////////////////////////////
  // /// Generic Proxy
  // ////////////////////////////////////////////////////////////////////////

  /**
   * Check if the given class is nor final neither a primitive one
   *
   * @param clazz The class to be checked
   * @return True if the class is proxable, false otherwise
   */
  public static boolean isProxable(Class<?> clazz) {
    return !clazz.isPrimitive() && !Modifier.isFinal(clazz.getModifiers()) && !clazz.isAnonymousClass();
  }

  /**
   * Creates a dynamic proxy
   *
   * @param interceptor          The interceptor that manages the invocations to the created proxy
   * @param clazz                The class to be proxied
   * @param failSafe             If true return null if it is not possible to proxy the request class, otherwise throws an UnproxableClassException
   * @param implementedInterface The interfaces that has to be implemented by the new proxy
   * @return The newly created proxy
   */
  public static <I extends MethodInterceptor & InvocationHandler, T> T createProxy(I interceptor, Class<T> clazz, boolean failSafe, Class<?>... implementedInterface) {
    if (clazz.isInterface())
      return (T) createNativeJavaProxy(clazz.getClassLoader(), interceptor, concatClasses(new Class<?>[]{clazz}, implementedInterface));
    RuntimeException e;
    try {
      return (T) createEnhancer(interceptor, clazz, implementedInterface).create();
    } catch (RuntimeException ex) {
      e = ex;
    }

    if (Proxy.isProxyClass(clazz))
      return (T) createNativeJavaProxy(clazz.getClassLoader(), interceptor, concatClasses(implementedInterface, clazz.getInterfaces()));
    if (isProxable(clazz)) return ClassImposterizer.INSTANCE.imposterise(interceptor, clazz, implementedInterface);
    if (failSafe) return null;
    throw e;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Class<T> getUnenhancedClass(T object) {
    return (Class<T>) getUnenhancedClass(object.getClass());
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Class<T> getUnenhancedClass(Class<? extends T> entityClass) {
    Class c = entityClass;
    while (c.getName().contains("$$")) {
      c = c.getSuperclass();
    }
    return (Class<T>) c;
  }

  // ////////////////////////////////////////////////////////////////////////
  // /// Private
  // ////////////////////////////////////////////////////////////////////////

  private static Enhancer createEnhancer(MethodInterceptor interceptor, Class<?> clazz, Class<?>... interfaces) {
    Enhancer enhancer = new Enhancer();
    enhancer.setNamingPolicy(ClassImposterizer.DEFAULT_POLICY);
    enhancer.setCallback(interceptor);
    enhancer.setSuperclass(clazz);
    if (interfaces != null && interfaces.length > 0) enhancer.setInterfaces(interfaces);
    return enhancer;
  }

  private static Object createNativeJavaProxy(ClassLoader classLoader, InvocationHandler interceptor, Class<?>... interfaces) {
    return Proxy.newProxyInstance(classLoader, interfaces, interceptor);
  }

  private static Class<?>[] concatClasses(Class<?>[] first, Class<?>[] second) {
    if (first == null || first.length == 0) return second;
    if (second == null || second.length == 0) return first;
    Class<?>[] concatClasses = new Class[first.length + second.length];
    System.arraycopy(first, 0, concatClasses, 0, first.length);
    System.arraycopy(second, 0, concatClasses, first.length, second.length);
    return concatClasses;
  }
}
