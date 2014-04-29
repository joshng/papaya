package joshng.util.blocks;

import joshng.util.collect.Nothing;

/**
 * User: josh
 * Date: 5/23/13
 * Time: 10:59 AM
 */
public interface SideEffect extends Source<Nothing>, Runnable {
    public static SideEffect extendRunnable(final Runnable runnable) {
        if (runnable instanceof SideEffect) return (SideEffect)runnable;
        return runnable::run;
    }

    default <T> Tapper<T> asTapper() {
        return new Tapper<T>() {
            @Override public void tap(T value) {
                run();
            }
        };
    }

    @Override
    default Runnable asRunnable() {
        return this;
    }

    @Override
    default Nothing get() {
        run();
        return Nothing.NOTHING;
    }
}
