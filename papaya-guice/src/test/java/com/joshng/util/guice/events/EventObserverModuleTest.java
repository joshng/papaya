package com.joshng.util.guice.events;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.joshng.util.collect.Nothing;
import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.concurrent.services.IdleService;
import com.joshng.util.concurrent.services.ServiceDependencyManager;
import com.joshng.util.guice.ManagedServicesModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: josh
 * Date: 11/16/18
 * Time: 11:25 AM
 */
public class EventObserverModuleTest {
  private EventDispatchModule dispatchModule;

  @Before
  public void setUp() throws Exception {
    dispatchModule = new EventDispatchModule();
    dispatchModule.registerEventType(EventClass.class);
    dispatchModule.registerEventObserver(EventInterfaceObserver.class);
  }

  @Test
  public void testSimpleWiring() {
    Injector injector = Guice.createInjector(dispatchModule);
    EventClass event = new EventClass();
    injector.getInstance(TestDispatcher.class).dispatcher.dispatch(event);

    assertEquals(1, injector.getInstance(EventInterfaceObserver.class).invocationCount);
  }

  @Test
  public void testServiceIntegration() {
    dispatchModule.registerEventObserver(SubscribingService.class);
    Injector injector = Guice.createInjector(dispatchModule,
            binder -> ManagedServicesModule.manager(binder).manage(SubscribingService.class)
                    .manage(PublishingService.class)
    );
    ServiceDependencyManager mgr = injector.getInstance(ServiceDependencyManager.class);
    System.out.println(mgr);
    try {
      injector.getInstance(PublishingService.class).dispatcher.dispatcher.dispatch(new EventClass()).getUnchecked();
      fail("Should have thrown");
    } catch (AssertionError e) {
      assertTrue(e.getMessage().equals("invoked before running"));
    }
    mgr.startAndWait();
    injector.getInstance(PublishingService.class).dispatcher.dispatcher.dispatch(new EventClass()).getUnchecked();
    mgr.stopAndWait();
  }

  interface EventInterface {

  }

  static class EventClass implements EventInterface {
  }

  @Singleton
  static class EventInterfaceObserver implements EventObserver<EventInterface> {
    private int invocationCount;

    @Override
    public FunFuture<Nothing> onEvent(EventInterface event) {
      invocationCount++;
      return FunFuture.NOTHING;
    }
  }

  static class TestDispatcher {
    final EventDispatcher<EventInterface> dispatcher;

    @Inject
    public TestDispatcher(EventDispatcher<EventInterface> dispatcher) {
      this.dispatcher = dispatcher;
    }
  }

  @Singleton
  static class PublishingService extends IdleService {
    private final TestDispatcher dispatcher;

    @Inject
    PublishingService(TestDispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }
  }

  @Singleton
  static class SubscribingService extends IdleService implements EventObserver<EventInterface> {
    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    public FunFuture<Nothing> onEvent(EventInterface event) {
      return FunFuture.runSafely(() -> Assert.assertTrue("invoked before running", isRunning()));
    }
  }

}