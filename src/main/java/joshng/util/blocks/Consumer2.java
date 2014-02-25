package joshng.util.blocks;

import joshng.util.collect.Nothing;

import java.util.Map;

/**
 * User: josh
 * Date: 10/2/12
 * Time: 1:57 AM
 */
public abstract class Consumer2<T, U> extends F2<T, U, Nothing> implements Consumer<Map.Entry<? extends T, ? extends U>> {
    public Nothing apply(T input1, U input2) {
        handle(input1, input2);
        return Nothing.NOTHING;
    }

    public void handle(Map.Entry<? extends T, ? extends U> value) {
        handle(value.getKey(), value.getValue());
    }

    public abstract void handle(T input1, U input2);

    @SuppressWarnings("unchecked")
    @Override
    public Sink<Map.Entry<T, U>> tupled() {
        return Sink.extendFunction(super.tupled());
    }
}
