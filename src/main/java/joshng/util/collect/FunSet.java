package joshng.util.collect;

import com.google.common.collect.ImmutableSet;
import joshng.util.blocks.Consumer;

import java.util.Set;

/**
 * User: josh
 * Date: 10/3/11
 * Time: 4:04 PM
 */
public interface FunSet<T> extends Set<T>, FunCollection<T> {
    FunSet<T> foreach(Consumer<? super T> visitor);
    ImmutableSet<T> delegate();
    <S> FunSet<S> cast();
}
