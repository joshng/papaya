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
public interface Sink2<T, U> extends BiConsumer<T, U>, F2<T, U, Nothing>, Consumer<Map.Entry<? extends T, ? extends U>>,ThrowingConsumer<Map.Entry<? extends T,? extends U>> {
    default public Nothing apply(T input1, U input2) {
        accept(input1, input2);
        return Nothing.NOTHING;
    }

    default public void accept(Map.Entry<? extends T, ? extends U> value) {
        accept(value.getKey(), value.getValue());
    }

    abstract void accept(T input1, U input2);

    @SuppressWarnings("unchecked")
    @Override
    default Sink<Map.Entry<? extends T, ? extends U>> tupled() {
        F<Map.Entry<? extends T, ? extends U>, Nothing> tupled = F2.super.tupled();
        return Sink.extendFunction(tupled);
    }
}
