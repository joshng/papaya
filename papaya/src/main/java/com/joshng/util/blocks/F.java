package com.joshng.util.blocks;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.joshng.util.collect.*;
import com.joshng.util.concurrent.AsyncF;
import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.exceptions.ExceptionPolicy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 8:47:46 AM
 */
@FunctionalInterface
public interface F<I, O> extends Function<I, O>, com.google.common.base.Function<I, O>, ThrowingFunction<I, O> {
  @SuppressWarnings({"unchecked"})
  F IDENTITY = new IdentityF();

  @Override
  O apply(I input);

  public static <I, O> F<I, O> function(F<I, O> f) {
    return f;
  }

  public static <I, O> F<I, O> extendFunction(final Function<I, O> f) {
    if (f instanceof F) return (F<I, O>) f;
    return f::apply;
  }

  /**
   * @deprecated intended to help identify and optimize spots where extendFunction is not needed
   */
  @Deprecated
  public static <I, O> F<I, O> extendFunction(F<I, O> f) {
    return f;
  }

  public static <I, O> F<I, O> extendGuavaFunction(com.google.common.base.Function<I, O> f) {
    if (f instanceof F) return (F<I, O>) f;
    return f::apply;
  }

  default <O2> F<I, O2> andThen(final Function<? super O, ? extends O2> next) {
    return (I i) -> next.apply(apply(i));
  }

  default <O2> AsyncF<I, O2> andThenAsync(final Function<? super O, ? extends ListenableFuture<O2>> next) {
    return from -> FunFuture.extendFuture(next.apply(apply(from)));
  }

  default Sink<I> andThenSink(final Consumer<? super O> sink) {
    return value -> sink.accept(apply(value));
  }

  default Sink<I> asSink() {
    return Sink.asSink(this);
  }

  @Override default F<I, O> unchecked() {
    return this;
  }

  default F<I, O> withSideEffect(final Runnable sideEffect) {
    final F<I, O> function = F.this;
    return input -> applyWithSideEffect(input, function, sideEffect);
  }

  public static <I, O> O applyWithSideEffect(I input, Function<I, O> function, Runnable sideEffect) {
    try {
      return function.apply(input);
    } finally {
      sideEffect.run();
    }
  }

  default Pred<I> resultMatches(final Predicate<? super O> predicate) {
    return new Pred<I>() {
      public boolean apply(I input) {
        return predicate.test(F.this.apply(input));
      }

      @Override
      public String toString() {
        return F.this.toString() + " matches " + predicate;
      }
    };
  }

  default Pred<I> resultEqualTo(O predicateValue) {
    return resultMatches(Pred.equalTo(predicateValue));
  }

  default Pred<I> resultNotEqualTo(O predicateValue) {
    return resultMatches(Pred.notEqualTo(predicateValue));
  }

  default <I0> F<I0, O> compose(final Function<? super I0, ? extends I> first) {
    return input -> apply(first.apply(input));
  }

  default FunIterable<O> transform(Iterable<? extends I> inputs) {
    return FunIterable.map(inputs, this);
  }

  default <I1> F2<I1, I, O> f2ignoringFirst() {
    return (input1, input2) -> apply(input2);
  }

  default <I2> F2<I, I2, O> f2ignoringSecond() {
    return (input1, input2) -> F.this.apply(input1);
  }

  default Source<O> bind(final I input) {
    return () -> F.this.apply(input);
  }

  default Source<O> bindFrom(final Supplier<? extends I> inputSupplier) {
    return () -> F.this.apply(inputSupplier.get());
  }

  default F<I, Source<O>> binder() {
    return this::bind;
  }

  default F<I, Either<Exception, O>> exceptionToLeft() {
    return input -> applyExceptionToLeft(input, F.this);
  }

  default <O2> F<I, Pair<O, O2>> asKeyWithValue(final F<? super I, ? extends O2> valueSupplier) {
    return input -> Pair.of(F.this.apply(input), valueSupplier.apply(input));
  }

  default F<I, Maybe<O>> handlingExceptions(final ExceptionPolicy exceptionPolicy) {
    return input -> {
      try {
        return Maybe.definitely(F.this.apply(input));
      } catch (RuntimeException e) {
        exceptionPolicy.applyOrThrow(e);
        return Maybe.not();
      }
    };
  }

  @SuppressWarnings({"unchecked"})
  public static <T> F<T, T> identityF() {
    return IDENTITY;
  }

  public static <K, V> F<K, V> forMap(Map<K, V> map) {
    return extendGuavaFunction(Functions.forMap(map));
  }

  public static <K, V> F<K, V> forMap(Map<K, V> map, @Nullable V defaultValue) {
    return extendGuavaFunction(Functions.forMap(map, defaultValue));
  }

  public static <T> F<Integer, T> forList(final List<T> list) {
    return list::get;
  }

  default public ImmutableListMultimap<O, I> group(Iterable<I> inputs) {
    return FunIterable.groupBy(inputs, this);
  }

  public static <I, O> Either<Exception, O> applyExceptionToLeft(@Nullable I input, Function<? super I, ? extends O> function) {
    try {
      return Either.right(function.apply(input));
    } catch (Exception e) {
      return Either.left(e);
    }
  }

  @SuppressWarnings({"unchecked"})
  class IdentityF implements F<Object, Object> {
    @Override
    public Object apply(Object input) {
      return input;
    }

    @Override
    public F andThen(Function next) {
      return F.extendFunction(next);
    }

    @Override
    public F compose(Function first) {
      return F.extendFunction(first);
    }

    @Override
    public Sink andThenSink(Consumer sink) {
      return Sink.extendConsumer(sink);
    }

    @Override
    public AsyncF andThenAsync(Function next) {
      return AsyncF.extendAsyncFunction(next);
    }

    @Override
    public FunIterable transform(Iterable inputs) {
      return Functional.extend(inputs);
    }
  }
}
