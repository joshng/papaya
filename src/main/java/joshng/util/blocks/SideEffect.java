package joshng.util.blocks;

import joshng.util.collect.Nothing;

/**
 * User: josh
 * Date: 5/23/13
 * Time: 10:59 AM
 */
public abstract class SideEffect extends Source<Nothing> implements Runnable {
    public static final SideEffect NOOP = new SideEffect() {
        @Override
        public void run() {
        }
    };

    public static final F<Runnable, SideEffect> RUNNABLE_WRAPPER = new F<Runnable, SideEffect>() {
        @Override public SideEffect apply(Runnable input) {
            return extendRunnable(input);
        }
    };

    public static SideEffect extendRunnable(final Runnable runnable) {
        if (runnable instanceof SideEffect) return (SideEffect)runnable;
        return new SideEffect() {
            @Override public void run() {
                runnable.run();
            }
        };
    }

    public <T> Tapper<T> asTapper() {
        return new Tapper<T>() {
            @Override public void tap(T value) {
                run();
            }
        };
    }

    @Override
    public Runnable asRunnable() {
        return this;
    }

    @Override
    public Nothing get() {
        run();
        return Nothing.NOTHING;
    }
}
