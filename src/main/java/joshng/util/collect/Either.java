package joshng.util.collect;

import com.google.common.base.Function;
import joshng.util.Reflect;
import joshng.util.blocks.F;

import static joshng.util.collect.Maybe.definitely;

/**
 * User: josh
 * Date: 11/15/12
 * Time: 3:47 PM
 */
public abstract class Either<L,R> {
    private static final F LEFT_GETTER = new F<Either, Maybe>() { public Maybe apply(Either input) { return input.left(); } };
    private static final F RIGHT_GETTER = new F<Either, Maybe>() { public Maybe apply(Either input) { return input.right(); } };

    private Either() {}

    public static <L, R> Either<L, R> left(L value) {
        return new Left<L, R>(definitely(value));
    }

    public static <L, R> Either<L, R> right(R value) {
        return new Right<L, R>(definitely(value));
    }

    @SuppressWarnings("unchecked")
    public static <L> F<Either<? extends L, ?>, Maybe<L>> leftGetter() { return LEFT_GETTER; }

    @SuppressWarnings("unchecked")
    public static <R> F<Either<?, ? extends R>, Maybe<R>> rightGetter() { return RIGHT_GETTER; }

    public static <L, R, O> F<Either<L, R>, Either<O, R>> liftLeft(final Function<? super L, ? extends O> mapper) {
        return new F<Either<L, R>, Either<O, R>>() {
            @Override
            public Either<O, R> apply(Either<L, R> input) {
                return input.mapLeft(mapper);
            }
        };
    }

    public abstract boolean isRight();
    public boolean isLeft() { return !isRight(); }

    public abstract Maybe<L> left();
    public abstract Maybe<R> right();

    public abstract <O> Either<O, R> mapLeft(Function<? super L, ? extends O> transformer);
    public abstract <O> Either<L, O> mapRight(Function<? super R, ? extends O> transformer);

    public abstract <O> O map(EitherFunction<? super L, ? super R, ? extends O> mapper);

    private static class Left<L, X> extends Either<L, X>  {
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
        public <O> Either<O, X> mapLeft(Function<? super L, ? extends O> transformer) {
            return new Left<O, X>(value.map(transformer));
        }

        @Override
        public <O> Either<L, O> mapRight(Function<? super X, ? extends O> transformer) {
            return Reflect.blindCast(this);
        }

        @Override
        public <O> O map(EitherFunction<? super L, ? super X, ? extends O> mapper) {
            return mapper.applyLeft(value.getOrThrow());
        }
    }

    private static class Right<X,R> extends Either<X,R> {
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
        public <O> Either<O, R> mapLeft(Function<? super X, ? extends O> transformer) {
            return Reflect.blindCast(this);
        }

        @Override
        public <O> Either<X, O> mapRight(Function<? super R, ? extends O> transformer) {
            return new Right<X, O>(value.map(transformer));
        }

        @Override
        public <O> O map(EitherFunction<? super X, ? super R, ? extends O> f) {
            return f.applyRight(value.getOrThrow());
        }
    }

}
