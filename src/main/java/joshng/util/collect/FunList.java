package joshng.util.collect;

import com.google.common.collect.ImmutableList;
import joshng.util.blocks.Consumer;

import java.util.List;

/**
 * User: josh
 * Date: 10/3/11
 * Time: 4:05 PM
 */
public interface FunList<T> extends List<T>, FunCollection<T> {
    FunList<T> tail();
    FunList<T> foreach(Consumer<? super T> visitor);
    ImmutableList<T> delegate();
    FunList<T> reverse();
    <S> FunList<S> cast();
    FunIterable<FunList<T>> partition(int size);
}
