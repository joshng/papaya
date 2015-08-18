package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:17 PM
 */
public abstract class IdleService extends BaseService<IdleService.InnerIdleService> {


  protected IdleService() {
    super(new InnerIdleService());
    delegate().wrapper = this;
  }

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

  static class InnerIdleService extends AbstractIdleService {
    private IdleService wrapper;
    @Override
    protected void startUp() throws Exception {
      Thread.currentThread().setName(wrapper.serviceStateString(state()));
      wrapper.startUp();
      // superclass takes care of cleaning up the thread-name later
    }

    @Override
    protected void shutDown() throws Exception {
      Thread.currentThread().setName(wrapper.serviceStateString(state()));
      wrapper.shutDown();
      // superclass takes care of cleaning up the thread-name later
    }

    @Override
    protected Executor executor() {
      State state = state();
      if (state == State.STARTING && wrapper.startUpOnSeparateThread() || wrapper.shutDownOnSeparateThread()) {
        return super.executor();
      } else {
        return MoreExecutors.directExecutor();
      }
    }

    @Override
    protected String serviceName() {
      return wrapper.serviceName();
    }
  }
}
