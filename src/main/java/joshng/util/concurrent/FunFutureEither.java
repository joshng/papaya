package joshng.util.concurrent;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.Reflect;
import joshng.util.blocks.ThrowingFunction;
import joshng.util.collect.Either;

import java.util.function.Function;

/**
 * User: josh
 * Date: 9/24/14
 * Time: 9:16 PM
 */
public interface FunFutureEither<L, R> extends FunFuture<Either<L, R>> {
  static <L, R> FunFutureEither<L, R> immediateLeftFuture(L leftValue) {
    return immediateFutureEither(Either.<L, R>left(leftValue));
  }

  static <L, R> FunFutureEither<L, R> immediateRightFuture(R rightValue) {
    return immediateFutureEither(Either.<L, R>right(rightValue));
  }

  static <L, R> FunFutureEither<L, R> immediateFutureEither(Either<L, R> either) {
    return new ForwardingFunFutureEither<>(Futures.immediateFuture(either));
  }

  static <L, R> FunFutureEither<L, R> wrapFutureEither(ListenableFuture<Either<L, R>> future) {
    return new ForwardingFunFutureEither<>(future);
  }

  default FunFutureMaybe<R> right() {
    return mapMaybe(either -> either.right());
  }

  default FunFutureMaybe<L> left() {
    return mapMaybe(either -> either.left());
  }

  default <O> FunFuture<O> fold(Either.Folder<? super L, ? super R, O> folder) {
    return map(either -> either.fold(folder));
  }

  default <O> FunFuture<O> fold(Function<? super L, O> leftFunction, Function<? super R, O> rightFunction) {
    return map(either -> either.fold(leftFunction, rightFunction));
  }

  default <O> FunFutureEither<L, O> mapIfRight(ThrowingFunction<? super R, O> transformer) {
    return mapEither(either -> either.mapRight(transformer.unchecked()));
  }

  default <O> FunFutureEither<L, O> flatMapIfRight(AsyncF<? super R, O> transformer) {
    return flatMapEither(
            either -> either.fold(
                    new Either.Folder<L, R, FunFutureEither<L, O>>() {
                      @Override public FunFutureEither<L, O> foldLeft(L leftValue) {
                        return FunFutureEither.immediateFutureEither(Reflect.<Either<L, O>>blindCast(either));
                      }

                      @Override public FunFutureEither<L, O> foldRight(R rightValue) {
                        return transformer.apply(rightValue).mapEither(Either::right);
                      }
                    }));
  }

  default <O> FunFutureEither<L, O> mapEitherIfRight(ThrowingFunction<? super R, Either<L, O>> transformer) {
    return flatMapEitherIfRight(rightValue -> FunFuture.immediateFuture(transformer.apply(rightValue)));
  }

  default <O> FunFutureEither<L, O> flatMapEitherIfRight(AsyncF<? super R, Either<L, O>> transformer) {
    return flatMapEither(
            either -> either.fold(new Either.Folder<L, R, ListenableFuture<Either<L, O>>>() {
                                    @Override public ListenableFuture<Either<L, O>> foldLeft(L leftValue) {
                                      return FunFutureEither.immediateFutureEither(Reflect.<Either<L, O>>blindCast(either));
                                    }

                                    @Override
                                    public ListenableFuture<Either<L, O>> foldRight(R rightValue) {
                                      return transformer.apply(rightValue);
                                    }
                                  }));
  }

  class EitherPromise<L, R> extends Promise<Either<L, R>> implements FunFutureEither<L, R> {
    @Override public FunFutureEither<L, R> completeOrRecoverWith(
            ListenableFuture<Either<L, R>> future,
            AsyncFunction<? super Exception, ? extends Either<L, R>> exceptionHandler
    ) {
      super.completeOrRecoverWith(future, exceptionHandler);
      return this;
    }
  }
}

class ForwardingFunFutureEither<L, R> extends FunFuture.ForwardingFunFuture<Either<L, R>> implements FunFutureEither<L, R> {
  public ForwardingFunFutureEither(ListenableFuture<Either<L, R>> delegate) {
    super(delegate);
  }
}
