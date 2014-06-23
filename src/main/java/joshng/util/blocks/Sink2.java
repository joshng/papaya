package joshng.util.blocks;

import joshng.util.collect.Nothing;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * User: josh
 * Date: 10/2/12
 * Time: 1:57 AM
 */
public interface Sink2<T, U> extends BiConsumer<T, U>, F2<T, U, Nothing>, Consumer<Map.Entry<? extends T, ? extends U>>, ThrowingConsumer<Map.Entry<? extends T, ? extends U>> {
  static <T, U> Sink2<T, U> sink2(Sink2<T, U> method) {
    return method;
  }

  static <T, U> Sink2<T, U> extendBiConsumer(BiConsumer<T, U> consumer) {
    if (consumer instanceof Sink2) return (Sink2<T, U>) consumer;
    return consumer::accept;
  }

  default public Nothing apply(T input1, U input2) {
    accept(input1, input2);
    return Nothing.NOTHING;
  }

  default public void accept(Map.Entry<? extends T, ? extends U> value) {
    accept(value.getKey(), value.getValue());
  }

  default <I> Sink<I> compose(Unzipper<? super I, ? extends T, ? extends U> unzipper) {
    return input -> accept(unzipper.getKey(input), unzipper.getValue(input));
  }

  @Override
  default Sink<U> bindFirst(final T input1) {
    return input2 -> accept(input1, input2);
  }

  @Override
  default Sink<T> bindSecond(final U input2) {
    return input1 -> accept(input1, input2);
  }

  abstract void accept(T input1, U input2);

  @SuppressWarnings("unchecked")
  @Override
  default Sink<Map.Entry<? extends T, ? extends U>> tupled() {
    return F2.super.tupled().asSink();
  }
}
