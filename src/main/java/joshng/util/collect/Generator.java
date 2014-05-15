package joshng.util.collect;

import com.google.common.base.Throwables;
import joshng.util.Reflect;
import joshng.util.blocks.F;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * User: josh
 * Date: Sep 6, 2010
 * Time: 1:24:13 PM
 */
public class Generator<P, T> implements F<P, T> {
  private final Constructor constructor;

  public static <P, T> Generator<P, T> on(Class<P> parameterClass, Class<T> generatedClass) {
    return new Generator<>(parameterClass, generatedClass);
  }

  public static <P1, P2, T> Generator2<P1, P2, T> on(Class<P1> parameter1Class, Class<P2> paramater2Class, Class<T> generatedClass) {
    return new Generator2<>(parameter1Class, paramater2Class, generatedClass);
  }

  public Generator(Class<P> parameterClass, Class<T> generatedClass) {
    constructor = Reflect.getConstructor(generatedClass, parameterClass);
    constructor.setAccessible(true);
  }

  @SuppressWarnings({"unchecked"})
  public T apply(P parameter) {
    try {
      return (T) constructor.newInstance(parameter);
    } catch (InstantiationException | IllegalAccessException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }
}
