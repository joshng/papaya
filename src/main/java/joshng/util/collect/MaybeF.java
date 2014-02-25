package joshng.util.collect;

import joshng.util.blocks.F;

/**
 * User: josh
 * Date: 3/12/13
 * Time: 3:35 PM
 */
public abstract class MaybeF<I, O> extends F<Maybe<? extends I>, O> implements MaybeFunction<I,O> {
    @Override
    public O apply(Maybe<? extends I> input) {
        return input.map(this);
    }

}
