package joshng.util.retries;

/**
 * User: josh
 * Date: Oct 22, 2010
 * Time: 12:30:55 PM
 */
public class TryAgain extends RuntimeException {
    public TryAgain(String message) {
        super(message);
    }

    public TryAgain() {
    }

    public static TryAgain please() {
        throw new TryAgain();
    }
    public static TryAgain please(String message) {
        throw new TryAgain(message);
    }
}
