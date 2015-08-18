package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import joshng.util.collect.Nothing;
import joshng.util.concurrent.FunFuture;

import javax.annotation.Nullable;

/**
 * User: josh
 * Date: 8/4/14
 * Time: 9:37 PM
 */
public abstract class AsyncService extends BaseService<AsyncService.InnerAbstractService> {
  protected AsyncService() {
    super(new InnerAbstractService());
    delegate().wrapper = this;
  }

  protected abstract FunFuture<Nothing> beginStartUp();

  protected abstract FunFuture<Nothing> beginShutDown();

  protected final void notifyFailed(Throwable t) {
    delegate().doNotifyFailed(t);
  }

  static class InnerAbstractService extends AbstractService {
    private AsyncService wrapper;

    @Override
    protected void doStart() {
      wrapper.beginStartUp().uponCompletion(new FutureCallback<Nothing>() {
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
      wrapper.beginShutDown().uponCompletion(new FutureCallback<Nothing>() {
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
      return wrapper.serviceName() + " [" + state() + "]";
    }
  }
}
