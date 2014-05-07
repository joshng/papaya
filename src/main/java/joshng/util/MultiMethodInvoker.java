package joshng.util;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import joshng.util.blocks.Pred;
import joshng.util.collect.FunList;
import joshng.util.exceptions.MultiException;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * User: josh
 * Date: Jul 4, 2011
 * Time: 11:24:08 PM
 */
public class MultiMethodInvoker<T> {
    private final ImmutableList<Method> methods;
    private static final MultiMethodInvoker NULL_INVOKER = new MultiMethodInvoker<Object>(ImmutableList.<Method>of()) {
        @Override
        public void invoke(Object target, boolean continueAfterException, Object... args) {
            // noooop!
        }
    };

    public static <T> MultiMethodInvoker<T> forMethodsMatching(Class<? extends T> targetClass,
                                                               @Nullable Class<? super T> stopClass,
                                                               Predicate<? super Method> methodSelector
    ) {
        FunList<? extends Class<?>> lineage = Reflect.getLineage(targetClass, stopClass).toList()
                .reverse(); // reverse to invoke superclasses before subclasses
        ImmutableList.Builder<Method> selectedMethods = ImmutableList.builder();
        for (Class<?> superclass : lineage) {
            for (Method method : superclass.getDeclaredMethods()) {
                if (methodSelector.apply(method)) {
                    selectedMethods.add(method);
                }
            }
        }
        ImmutableList<Method> methods = selectedMethods.build();
        @SuppressWarnings("unchecked") MultiMethodInvoker<T> result = methods.isEmpty() ? NULL_INVOKER : new MultiMethodInvoker<>(methods);
        return result;
    }

    public static <T> MultiMethodInvoker<T> forMethodsMatching(Class<? extends T> targetClass, Predicate<? super Method> methodSelector) {
        return forMethodsMatching(targetClass, null, methodSelector);
    }

    public static <T> MultiMethodInvoker<T> forAnnotatedMethods(Class<? extends T> targetClass, Class<? extends Annotation> annotationClass) {
        return forMethodsMatching(targetClass, Pred.annotatedWith(annotationClass));
    }

    public MultiMethodInvoker(ImmutableList<Method> methods) {
        this.methods = methods;
        for (Method method : methods) {
            method.setAccessible(true);
        }
    }

    public void invoke(T target, boolean continueAfterException, Object... args) {
        MultiException multiException = MultiException.Empty;
        for (Method method : methods) {
            try {
                method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                if (!continueAfterException) throw Throwables.propagate(e.getCause());
                multiException = multiException.with(e.getCause());
            } catch (Exception e) {
                if (!continueAfterException) throw Throwables.propagate(e);
                multiException = multiException.with(e);
            }
        }
        multiException.throwRuntimeIfAny();
    }

    public boolean isEmpty() {
        return methods.isEmpty();
    }
}
