package joshng.util.blocks;

/**
 * User: josh
 * Date: 6/22/12
 * Time: 2:51 PM
 */

/**
 * This is the "K combinator": a type of function which applies side-effects to its input, then
 * returns the input itself.
 * This can be convenient when applying side-effects or validations to a value prior to returning it.
 */
public abstract class Tapper<T> extends F<T, T> implements Tap<T> {
    @Override
    public final T apply(T input) {
        tap(input);
        return input;
    }
}

interface Tap<T> {
    void tap(T value);
}

