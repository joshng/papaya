package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.Executor;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:17 PM
 */
public abstract class IdleService extends BaseService {
  private final InnerIdleService delegate = new InnerIdleService();

  @Override protected final Service delegate() { return delegate; }


  /**
   * Start the service.
   */
  protected abstract void startUp() throws Exception;

  /**
   * Stop the service.
   */
  protected abstract void shutDown() throws Exception;

  protected boolean startUpOnSeparateThread() {
    return true;
  }

  protected boolean shutDownOnSeparateThread() {
    return true;
  }

  private class InnerIdleService extends AbstractIdleService {
    @Override
    protected void startUp() throws Exception {
      Thread.currentThread().setName(serviceStateString(state()));
      IdleService.this.startUp();
      // superclass takes care of cleaning up the thread-name later
    }

    @Override
    protected void shutDown() throws Exception {
      Thread.currentThread().setName(serviceStateString(state()));
      IdleService.this.shutDown();
      // superclass takes care of cleaning up the thread-name later
    }

    @Override
    protected Executor executor() {
      State state = state();
      if (state == State.STARTING && startUpOnSeparateThread() || shutDownOnSeparateThread()) {
        return super.executor();
      } else {
        return MoreExecutors.sameThreadExecutor();
      }
    }

    @Override
    protected String serviceName() {
      return IdleService.this.serviceName();
    }
  }
}
