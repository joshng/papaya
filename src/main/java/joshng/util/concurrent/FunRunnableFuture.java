package joshng.util.concurrent;

import java.util.concurrent.RunnableFuture;

/**
 * Created by: josh 10/14/13 2:36 PM
 */
public interface FunRunnableFuture<T> extends FunFuture<T>, RunnableFuture<T> {
    public boolean isInterruptOnCancel();
    public void setInterruptOnCancel(boolean interruptOnCancel);
}
