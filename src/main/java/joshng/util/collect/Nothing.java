package joshng.util.collect;

import joshng.util.blocks.Source;
import joshng.util.concurrent.FunFuture;
import joshng.util.concurrent.FunFutures;

/**
 * User: josh
 * Date: 2/13/13
 * Time: 6:46 PM
 *
 * A "Unit" type; like {@link Void}, but useful for representing the lack of information without resorting to 'null'
 */
public final class Nothing {
    public static final Nothing NOTHING = new Nothing();
    public static final FunFuture<Nothing> FUTURE = FunFutures.immediateFuture(NOTHING);
    public static final Source<Nothing> SOURCE = Source.ofInstance(NOTHING);

    private Nothing() {}
}
