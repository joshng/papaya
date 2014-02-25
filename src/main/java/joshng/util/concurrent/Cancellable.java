package joshng.util.concurrent;

/**
* Created by: josh 10/22/13 2:04 PM
*/
public interface Cancellable {
    boolean cancel(boolean mayInterruptIfRunning);
}
