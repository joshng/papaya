package joshng.util.concurrent.services;

/**
 * User: josh Date: 10/2/13 Time: 12:21 PM
 */
public abstract class InitializingService extends IdleService {
  protected abstract void init() throws Exception;

  @Override
  protected void startUp() throws Exception {
    init();
  }

  @Override
  protected void shutDown() throws Exception {
  }

  @Override
  protected boolean shutDownOnSeparateThread() {
    return false;
  }
}
