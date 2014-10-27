package joshng.util.blocks;

import joshng.util.collect.Nothing;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:14:03 AM
 */
@FunctionalInterface
public interface Sink<T> extends F<T, Nothing>, Consumer<T>, ThrowingConsumer<T> {
  Sink<Object> NOOP = obj -> {};

  public static <T> Sink<T> sink(Sink<T> sink) {
    return sink;
  }

  public static <T> Sink<T> extendConsumer(final Consumer<T> handler) {
    if (handler instanceof Sink) return (Sink<T>) handler;
    return handler::accept;
  }

  public static Sink<Object> ignoreInput(Runnable sideEffect) {
    return object -> sideEffect.run();
  }

  @SuppressWarnings("unchecked")
  public static <T> Sink<T> asSink(final Function<T, ?> handler) {
    if (handler instanceof Sink) return (Sink<T>) handler;
    return handler::apply;
  }

  public static <T> Sink<T> warningLogger(final Logger logger, final String format) {
    return value -> logger.warn(format, value);
  }

  public static <T> Sink<T> infoLogger(final Logger logger, final String format) {
    return value -> logger.info(format, value);
  }

  void accept(T value);

  default <U extends T> U acceptAndReturn(U value) {
    accept(value);
    return value;
  }

  @Override
  default <I0> Sink<I0> compose(Function<? super I0, ? extends T> first) {
    return value -> accept(first.apply(value));
  }

  default Nothing apply(T input) {
    accept(input);
    return Nothing.NOTHING;
  }

  @Override
  default SideEffect bind(final T input) {
    return () -> accept(input);
  }

  default Sink<T> filter(final Predicate<? super T> inputFilter) {
    return value -> {
      if (inputFilter.test(value)) Sink.this.accept(value);
    };
  }
}
