package joshng.util.blocks;

/**
 * User: josh
 * Date: Jul 19, 2010
 * Time: 9:05:41 PM
 */
public interface Consumer<T> extends ThrowingConsumer<T> {
    void handle(T value);
}
