package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Service;
import joshng.util.collect.Nothing;
import joshng.util.concurrent.FunFuture;

import javax.annotation.Nullable;

/**
 * User: josh
 * Date: 8/4/14
 * Time: 9:37 PM
 */
public abstract class AsyncService extends BaseService {
  private final InnerAbstractService delegate = new InnerAbstractService();

  @Override protected Service delegate() { return delegate; }

  protected abstract FunFuture<Nothing> beginStartUp();

  protected abstract FunFuture<Nothing> beginShutDown();

  protected final void notifyFailed(Throwable t) {
    delegate.doNotifyFailed(t);
  }

  private class InnerAbstractService extends AbstractService {
    @Override
    protected void doStart() {
      beginStartUp().uponCompletion(new FutureCallback<Nothing>() {
        @Override public void onSuccess(@Nullable Nothing result) {
          notifyStarted();
        }

        @Override public void onFailure(Throwable t) {
          notifyFailed(t);
        }
      });
    }

    @Override
    protected void doStop() {
      beginShutDown().uponCompletion(new FutureCallback<Nothing>() {
        @Override public void onSuccess(@Nullable Nothing result) {
          notifyStopped();
        }

        @Override public void onFailure(Throwable t) {
          notifyFailed(t);
        }
      });
    }

    private void doNotifyFailed(Throwable t) {
      notifyFailed(t);
    }

    @Override public String toString() {
      return serviceName() + " [" + state() + "]";
    }
  }
}
