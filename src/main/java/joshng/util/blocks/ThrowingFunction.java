package joshng.util.blocks;

/**
 * User: josh
 * Date: 12/26/11
 * Time: 2:32 PM
 */
public interface ThrowingFunction<I, O> {
  public O apply(I input) throws Exception;
}
