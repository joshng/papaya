package joshng.util.collect;

import joshng.util.blocks.Consumer;

import java.util.Collection;

/**
 * User: josh
 * Date: 11/2/11
 * Time: 12:59 PM
 */
public interface FunCollection<T> extends Collection<T>, FunIterable<T> {
    Collection<T> delegate();
    FunCollection<T> foreach(Consumer<? super T> visitor);
}
