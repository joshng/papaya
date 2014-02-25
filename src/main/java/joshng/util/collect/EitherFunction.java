package joshng.util.collect;

import com.google.common.base.Function;
import joshng.util.blocks.F;

/**
* User: josh
* Date: 11/27/12
* Time: 5:32 PM
*/
public abstract class EitherFunction<L, R, O> extends F<Either<? extends L, ? extends R>, O> {
    public static <L, R, O> EitherFunction<L, R, O> combined(final Function<? super L, ? extends O> leftMapper, final Function<? super R, ? extends O> rightMapper) {
        return new CombinedEitherFunction<L, R, O>(leftMapper, rightMapper);
    }

    public abstract O applyLeft(L leftValue);
    public abstract O applyRight(R rightValue);

    @Override
    public O apply(Either<? extends L, ? extends R> input) {
        return input.map(this);
    }

    private static class CombinedEitherFunction<L, R, O> extends EitherFunction<L, R, O> {
        private final Function<? super L, ? extends O> leftMapper;
        private final Function<? super R, ? extends O> rightMapper;

        public CombinedEitherFunction(Function<? super L, ? extends O> leftMapper, Function<? super R, ? extends O> rightMapper) {
            this.leftMapper = leftMapper;
            this.rightMapper = rightMapper;
        }

        @Override
        public O applyLeft(L leftValue) {
            return leftMapper.apply(leftValue);
        }

        @Override
        public O applyRight(R rightValue) {
            return rightMapper.apply(rightValue);
        }
    }
}
