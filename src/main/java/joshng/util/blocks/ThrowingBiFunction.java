package joshng.util.blocks;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: 9/2/14
 * Time: 12:41 PM
 */
public interface ThrowingBiFunction<I1, I2, O> extends ThrowingFunction<Map.Entry<? extends I1, ? extends I2>, O> {
  O apply(I1 input1, I2 input2) throws Exception;

  default Callable<O> bind(I1 input1, I2 input2) {
    return () -> apply(input1, input2);
  }

  default ThrowingFunction<I2, O> bindFirst(I1 input1) {
    return input2 -> apply(input1, input2);
  }

  default ThrowingFunction<I1, O> bindSecond(I2 input2) {
    return input1 -> apply(input1, input2);
  }

  default O apply(Map.Entry<? extends I1, ? extends I2> pair) throws Exception {
    return apply(pair.getKey(), pair.getValue());
  }
}
