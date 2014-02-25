package joshng.util.context;

/**
 * User: josh
 * Date: 2/3/12
 * Time: 12:14 AM
 */
public interface TransientContext {
    public interface State {
        void exit();
    }

    State enter();
}
