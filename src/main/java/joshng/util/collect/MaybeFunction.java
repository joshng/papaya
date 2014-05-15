package joshng.util.collect;

/**
 * User: josh
 * Date: 4/2/13
 * Time: 2:48 PM
 */
public interface MaybeFunction<I, O> {
  O whenDefined(I input);

  O whenEmpty();
}
