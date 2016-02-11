package com.joshng.util.collect;

import com.joshng.util.blocks.F;
import com.joshng.util.Reflect;

import java.util.function.Function;

import static com.joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 11/15/12
 * Time: 3:47 PM
 */
public abstract class Either<L, R> {
  private static final F LEFT_GETTER = new F<Either, Maybe>() {
    public Maybe apply(Either input) {
      return input.left();
    }
  };
  private static final F RIGHT_GETTER = new F<Either, Maybe>() {
    public Maybe apply(Either input) {
      return input.right();
    }
  };


  @SuppressWarnings("unchecked")
  public static <L,R> Builder<L,R> builder() {
    return BUILDER;
  }

  private Either() {}

  public static <L, R> Either<L, R> left(L value) {
    return new Left<L, R>(Maybe.definitely(value));
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Right<L, R>(Maybe.definitely(value));
  }

  public abstract boolean isRight();

  public boolean isLeft() {
    return !isRight();
  }

  public abstract Maybe<L> left();

  public abstract Maybe<R> right();

  public abstract <O> Either<O, R> mapLeft(Function<? super L, O> transformer);

  public abstract <O> Either<L, O> mapRight(Function<? super R, O> transformer);

  public abstract <O> Either<L, O> flatMapRight(Function<? super R, ? extends Either<L, O>> transformer);

  public abstract <O> O fold(Function<? super L, ? extends O> leftFn, Function<? super R, ? extends O> rightFn);

  public <O> O fold(Folder<? super L, ? super R, O> folder) {
    return fold(folder::foldLeft, folder::foldRight);
  }

  public interface Folder<L,R,O> {
    O foldLeft(L leftValue);
    O foldRight(R rightValue);
  }

  private static class Left<L, X> extends Either<L, X> {
    private final Maybe<L> value;

    private Left(Maybe<L> definitely) {
      assert definitely.isDefined();
      this.value = definitely;
    }

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public Maybe<L> left() {
      return value;
    }

    @Override
    public Maybe<X> right() {
      return Maybe.not();
    }

    @Override
    public <O> Either<O, X> mapLeft(Function<? super L, O> transformer) {
      return new Left<O, X>(value.map(transformer));
    }

    @Override
    public <O> Either<L, O> mapRight(Function<? super X, O> transformer) {
      return Reflect.blindCast(this);
    }

    @Override public <O> Either<L, O> flatMapRight(Function<? super X, ? extends Either<L, O>> transformer) {
      return Reflect.blindCast(this);
    }

    @Override
    public <O> O fold(Function<? super L, ? extends O> leftFn, Function<? super X, ? extends O> rightFn) {
      return leftFn.apply(value.getOrThrow());
    }
  }


  private static class Right<X, R> extends Either<X, R> {
    private final Maybe<R> value;

    private Right(Maybe<R> definitely) {
      assert definitely.isDefined();
      this.value = definitely;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public Maybe<X> left() {
      return Maybe.not();
    }

    @Override
    public Maybe<R> right() {
      return value;
    }

    @Override
    public <O> Either<O, R> mapLeft(Function<? super X, O> transformer) {
      return Reflect.blindCast(this);
    }

    @Override
    public <O> Either<X, O> mapRight(Function<? super R, O> transformer) {
      return new Right<X, O>(value.map(transformer));
    }

    @Override public <O> Either<X, O> flatMapRight(Function<? super R, ? extends Either<X, O>> transformer) {
      return transformer.apply(value.getOrThrow());
    }

    @Override
    public <O> O fold(Function<? super X, ? extends O> leftFn, Function<? super R, ? extends O> rightFn) {
      return rightFn.apply(value.getOrThrow());
    }
  }

  private static final Builder BUILDER = new Builder();

  public static class Builder<L,R> {
    public Either<L,R> left(L leftValue) {
      return Either.left(leftValue);
    }
    public Either<L,R> right(R rightValue) {
      return Either.right(rightValue);
    }
  }
}
