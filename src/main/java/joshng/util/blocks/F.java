package joshng.util.blocks;

import com.google.common.base.Functions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Modifiers;
import joshng.util.collect.*;
import joshng.util.concurrent.AsyncF;
import joshng.util.exceptions.ExceptionPolicy;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Maybe.definitely;
import static joshng.util.concurrent.FunFuture.extendFuture;

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

  public static <I, O> F<I, O> method(F<I, O> f) {
    return f;
  }

  public static <I, O> F<I, O> extendF(final Function<I, O> f) {
    if (f instanceof F) return (F<I, O>) f;
    return f::apply;
  }

  public static <I, O> F<I, O> extendGuava(com.google.common.base.Function<I, O> f) {
    if (f instanceof F) return (F<I, O>) f;
    return f::apply;
  }

  default <O2> F<I, O2> andThen(final Function<? super O, ? extends O2> next) {
    return (I i) -> next.apply(apply(i));
  }

  default <O2> AsyncF<I, O2> andThenAsync(final Function<? super O, ? extends ListenableFuture<O2>> next) {
    return from -> extendFuture(next.apply(apply(from)));
  }

  default Sink<I> andThenSink(final Consumer<? super O> sink) {
    return value -> sink.accept(apply(value));
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
      public boolean test(I input) {
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
        return definitely(F.this.apply(input));
      } catch (RuntimeException e) {
        exceptionPolicy.applyOrThrow(e);
        return Maybe.not();
      }
    };
  }

  @SuppressWarnings({"unchecked"})
  public static <T> F<T, T> identity() {
    return IDENTITY;
  }

  public static <K, V> F<K, V> forMap(Map<K, V> map) {
    return extendGuava(Functions.forMap(map));
  }

  public static <K, V> F<K, V> forMap(Map<K, V> map, @Nullable V defaultValue) {
    return extendGuava(Functions.forMap(map, defaultValue));
  }

  public static <T> F<Integer, T> forList(final List<T> list) {
    return list::get;
  }

  public static <T> F<String, T> stringToValueOf(final Class<T> conversionClass) {
    final Method valueOfMethod;
    try {
      valueOfMethod = conversionClass.getMethod("valueOf", String.class);
      checkArgument(Modifiers.Static.matches(valueOfMethod), "valueOf(String) method is not static", valueOfMethod);
    } catch (NoSuchMethodException e) {
      throw Throwables.propagate(e);
    }

    valueOfMethod.setAccessible(true);
    return input -> {
      try {
        return conversionClass.cast(valueOfMethod.invoke(null, input));
      } catch (IllegalAccessException e) {
        throw Throwables.propagate(e);
      } catch (InvocationTargetException e) {
        throw Throwables.propagate(e.getCause());
      }
    };
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
      return F.extendF(next);
    }

    @Override
    public F compose(Function first) {
      return F.extendF(first);
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
