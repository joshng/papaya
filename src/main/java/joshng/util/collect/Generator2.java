package joshng.util.collect;

import com.google.common.base.Throwables;
import joshng.util.Reflect;
import joshng.util.blocks.F2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * User: josh
 * Date: 4/17/12
 * Time: 7:12 PM
 */
public class Generator2<I1, I2, O> implements F2<I1, I2, O> {
  private final Constructor constructor;

  public Generator2(Class<I1> parameterClass1, Class<I2> parameterClass2, Class<O> generatedClass) {
    constructor = Reflect.getConstructor(generatedClass, parameterClass1, parameterClass2);
    constructor.setAccessible(true);
  }

  @SuppressWarnings({"unchecked"})
  public O apply(I1 input1, I2 input2) {
    try {
      return (O) constructor.newInstance(input1, input2);
    } catch (InstantiationException | IllegalAccessException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }
}
