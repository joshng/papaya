package com.joshng.util.reflect;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.joshng.util.blocks.F;
import com.joshng.util.blocks.Pred;
import com.joshng.util.blocks.Sink2;
import com.joshng.util.collect.AbstractFunIterable;
import com.joshng.util.collect.FunIterable;
import com.joshng.util.collect.FunPairs;
import com.joshng.util.collect.Maybe;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: Aug 4, 2010
 * Time: 4:59:24 PM
 */
public class Reflect {
  private static final F FIRST_GENERIC_TYPE_FINDER = genericTypeFinder(0);

  public static String verifyMethodName(Class<?> klass, String expectedName, Class<?>... parameterClasses) {
    return getMethod(klass, expectedName, parameterClasses).getName();
  }

  public static Method getMethod(Class<?> klass, String methodName, Class<?>... parameterClasses) {
    Method method;
    try {
      method = klass.getMethod(methodName, parameterClasses);
    } catch (NoSuchMethodException e) {
      try {
        method = klass.getDeclaredMethod(methodName, parameterClasses);
      } catch (NoSuchMethodException f) {
        throw Throwables.propagate(f);
      }
    }
    return method;
  }

  public static <T> Constructor<T> getConstructor(Class<T> klass, Class<?>... parameterClasses) {
    Constructor<T> method;
    try {
      method = klass.getDeclaredConstructor(parameterClasses);
    } catch (NoSuchMethodException e) {
      try {
        method = klass.getConstructor(parameterClasses);
      } catch (NoSuchMethodException f) {
        throw Throwables.propagate(f);
      }
    }
    return method;
  }

  public static <O, T> F<O, T> fieldReader(Class<? extends O> objectClass, final Class<T> fieldClass, final Field field) {
    checkFieldType(objectClass, fieldClass, field);
    return input -> {
      try {
        return fieldClass.cast(field.get(input));
      } catch (IllegalAccessException e) {
        throw Throwables.propagate(e);
      }
    };
  }

  public static <O, T> Sink2<O, T> fieldWriter(Class<? extends O> objectClass, final Class<T> fieldClass, final Field field) {
    checkFieldType(objectClass, fieldClass, field);
    field.setAccessible(true);
    return (obj, value) -> {
      try {
        field.set(obj, value);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static <O, T> void checkFieldType(Class<? extends O> objectClass, Class<T> fieldClass, Field field) {
    checkArgument(fieldClass.isAssignableFrom(field.getType()), "%s is not castable to %s", field, fieldClass);
    Class<?> declaringClass = field.getDeclaringClass();
    checkArgument(declaringClass.isAssignableFrom(objectClass), "%s does not declare %s", objectClass, field);
    field.setAccessible(true);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> LinkedHashSet<Class<? super T>> getAllInterfaces(Class<T> type, boolean includeClasses) {
    LinkedHashSet<Class<? super T>> result = Sets.newLinkedHashSet();
    Queue<Class<?>> toCheck = Queues.newArrayDeque();

    toCheck.offer(type);
    Class<?> inheritedType;
    while ((inheritedType = toCheck.poll()) != null) {
      if (includeClasses) result.add((Class<? super T>) inheritedType);
      for (Class<?> iface : inheritedType.getInterfaces()) {
        result.add((Class<? super T>) iface);
        toCheck.offer(iface);
        Class<?> superInterface = iface.getSuperclass();
        if (superInterface != null) {
          toCheck.offer(superInterface);
          result.add((Class<? super T>) superInterface);
        }
      }
      Class<?> superclass = inheritedType.getSuperclass();
      if (superclass != null) toCheck.offer(superclass);
    }
    return result;
  }

  public interface AnnotatedMethodHandler<A extends Annotation> {
    void handle(Method method, A annotation);
  }

  public interface AnnotatedFieldHandler<A extends Annotation> {
    void handle(Field field, A annotation);
  }

  public interface AnnotatedParameterHandler<A extends Annotation> {
    void handle(Method method, A annotation, int paramIndex);
  }

  public interface MetaAnnotationHandler<A extends Annotation> {
    void handle(A metaAnnotation, Annotation annotated);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Class<? extends T> classForName(String className) {
    try {
      return (Class<? extends T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  private static final LoadingCache<Class<?>, Constructor> ACCESSIBLE_CONSTRUCTORS = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Constructor>() {
    public Constructor load(@javax.annotation.Nullable Class<?> from) {
      Constructor constructor = getConstructor(from);
      constructor.setAccessible(true);
      return constructor;
    }
  });

  public static <T> T newInstance(Class<? extends T> c) {
    Constructor constructor = ACCESSIBLE_CONSTRUCTORS.getUnchecked(c);
    try {
      return c.cast(constructor.newInstance());
    } catch (InstantiationException | IllegalAccessException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  public static <A extends Annotation> void visitMethodsWithAnnotation(Class<?> klass, Class<A> annotationClass, AnnotatedMethodHandler<A> handler) {
    for (Method method : getAllDeclaredMethods(klass, Object.class).filter(Pred.annotatedWith(annotationClass)).unique()) {
      handler.handle(method, method.getAnnotation(annotationClass));
    }
  }

  public static <A extends Annotation> FunPairs<Field, A> getFieldsWithAnnotation(Class<?> klass, final Class<A> annotationClass) {
    return getAllFields(klass).filter(Pred.annotatedWith(annotationClass)).asKeysTo(annotationGetter(annotationClass));
  }

  public static <A extends Annotation> F<AnnotatedElement, A> annotationGetter(final Class<A> annotationClass) {
    return new F<AnnotatedElement, A>() {
      public A apply(AnnotatedElement input) {
        return input.getAnnotation(annotationClass);
      }
    };
  }

  public static <A extends Annotation> F<AnnotatedElement, Maybe<A>> maybeAnnotationGetter(final Class<A> annotationClass) {
    return annotationGetter(annotationClass).andThen(Maybe::of);
  }

  public static FunIterable<Field> getAllFields(Class<?> klass) {
    return getLineage(klass, Object.class).flatMap(new F<Class<?>, Iterable<Field>>() {
      public Iterable<Field> apply(Class<?> input) {
        return Arrays.asList(input.getDeclaredFields());
      }
    });
  }

  public static FunIterable<Method> getAllDeclaredMethods(Class<?> klass) {
    return getAllDeclaredMethods(klass, Object.class);
  }

  public static <S, T extends S> FunIterable<Method> getAllDeclaredMethods(Class<T> klass, @Nullable Class<S> stopClass) {
    return getLineage(klass, stopClass).flatMap(new F<Class<? super T>, Iterable<Method>>() {
      public Iterable<Method> apply(Class<? super T> input) {
        return Arrays.asList(input.getDeclaredMethods());
      }
    });
  }

  public static <A extends Annotation> void visitParameterAnnotations(Class<?> klass, Class<A> annotationClass, AnnotatedParameterHandler<A> handler) {
    while (klass != Object.class) {
      for (Method method : klass.getDeclaredMethods()) {
        for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
          for (int i = 0; i < paramAnnotations.length; i++) {
            Annotation paramAnnotation = paramAnnotations[i];
            if (paramAnnotation.annotationType() == annotationClass) {
              handler.handle(method, annotationClass.cast(paramAnnotation), i);
            }
          }
        }
      }
      klass = klass.getSuperclass();
    }
  }

  public static Type[] getGenericTypeParameters(Type klass) {
    Type superclass = klass;
    while (superclass instanceof Class) {
      superclass = ((Class) superclass).getGenericSuperclass();
      if (superclass == Object.class) throw new IllegalStateException("Missing type parameter for " + klass);
    }
    if (superclass == null) {
      throw new AssertionError("Invalid class: " + klass);
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    return parameterized.getActualTypeArguments();
  }

  @SuppressWarnings({"unchecked"})
  public static <T extends Type> T getGenericTypeParameter(Type type, int parameterIndex) {
    return (T) getGenericTypeParameters(type)[parameterIndex];
  }

  @SuppressWarnings({"unchecked"})
  public static <T extends Type> T getFirstGenericType(Type type) {
    return (T) getGenericTypeParameter(type, 0);
  }

  @SuppressWarnings({"unchecked"})
  public static <G> F<Type, Class<? extends G>> firstGenericTypeFinder() {
    return FIRST_GENERIC_TYPE_FINDER;
  }

  public static <G> F<Type, Class<? extends G>> genericTypeFinder(final int index) {
    return new F<Type, Class<? extends G>>() {
      public Class<? extends G> apply(Type input) {
        return getGenericTypeParameter(input, index);
      }
    };
  }

  public static <S, T extends S> AbstractFunIterable<Class<? super T>> getLineage(Class<T> klass, @Nullable Class<S> stopClass) {
    return () -> new LineageIterator<>(klass, stopClass);
  }


  public static <A extends Annotation> Maybe<A> getAnnotationMaybe(AnnotatedElement element, Class<A> annotationClass) {
    return Maybe.of(element.getAnnotation(annotationClass));
  }

  @Nullable
  public static <A extends Annotation> A findMetaAnnotation(Annotation annotation, Class<A> desiredAnnotationType) {
    if (desiredAnnotationType.isInstance(annotation)) return desiredAnnotationType.cast(annotation);
    for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
      // skip the ever-present (and recursively infinite) java.lang.annotation.Target, Retention, etc
      if (!isPlatformAnnotation(metaAnnotation)) {
        A foundAnnotation = findMetaAnnotation(metaAnnotation, desiredAnnotationType);
        if (foundAnnotation != null) return foundAnnotation;
      }
    }
    return null;
  }

  private static boolean isPlatformAnnotation(Annotation annotation) {
    return annotation.annotationType().getPackage().getName().startsWith("java");
  }

  public static <A extends Annotation> void visitMetaAnnotations(Annotation annotation, Class<A> desiredAnnotationType, MetaAnnotationHandler<A> handler) {
    if (desiredAnnotationType.isInstance(annotation)) {
      handler.handle(desiredAnnotationType.cast(annotation), annotation);
      return;
    }
    A foundMetaAnnotation = null;
    for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
      // skip the ever-present (and recursively infinite) java.lang.annotation.Target, Retention, etc
      if (!isPlatformAnnotation(metaAnnotation)) {
        if (desiredAnnotationType.isInstance(metaAnnotation)) {
          // save this annotation for later; we visit them in depth-first order
          foundMetaAnnotation = desiredAnnotationType.cast(metaAnnotation);
        } else {
          visitMetaAnnotations(metaAnnotation, desiredAnnotationType, handler);
        }
      }
    }
    if (foundMetaAnnotation != null) handler.handle(foundMetaAnnotation, annotation);
  }

  public static <A extends Annotation> void visitMetaAnnotations(AnnotatedElement element, Class<A> desiredAnnotationType, MetaAnnotationHandler<A> handler) {
    for (Annotation annotation : element.getAnnotations()) {
      visitMetaAnnotations(annotation, desiredAnnotationType, handler);
    }
  }

  @Nullable
  public static <T> T castOrNull(Object obj, Class<T> castClass) {
    return castClass.isInstance(obj) ? castClass.cast(obj) : null;
  }

  @Nullable
  public static <T, R> R ifInstance(Object obj, Class<T> castClass, Function<? super T, ? extends R> func) {
    T casted = castOrNull(obj, castClass);
    if (casted == null) return null;
    return func.apply(casted);
  }

  public static <T, R> R ifInstance(Object obj, Class<T> castClass, R elseValue, Function<? super T, ? extends R> func) {
    return Objects.firstNonNull(ifInstance(obj, castClass, func), elseValue);
  }

  public static <T> boolean ifInstance(Object obj, Class<T> castClass, Consumer<? super T> handler) {
    T casted = castOrNull(obj, castClass);
    if (casted == null) return false;
    handler.accept(casted);
    return true;
  }

  @Nullable
  public static <T, R> R ifNotNull(@Nullable T obj, Function<? super T, ? extends R> func) {
    return ifNotNull(obj, func, null);
  }

  public static <T, R> R ifNotNull(@Nullable T obj, Function<? super T, ? extends R> func, R valueForNull) {
    if (obj == null) return valueForNull;
    return func.apply(obj);
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T blindCast(Object object) {
    return (T) object;
  }

  private static class LineageIterator<S, T extends S> extends AbstractIterator<Class<? super T>> {
    private final Class<S> stopClass;
    private Class<? super T> currentClass;

    public LineageIterator(Class<T> klass, Class<S> stopClass) {
      this.stopClass = stopClass;
      currentClass = klass;
    }

    @Override
    protected Class<? super T> computeNext() {
      if (currentClass == stopClass) return endOfData();
      Class<? super T> current = currentClass;
      //noinspection unchecked
      currentClass = current.getSuperclass();
      return current;
    }
  }
}
