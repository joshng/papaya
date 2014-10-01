package joshng.util.events;

/**
 * User: josh
 * Date: 9/30/14
 * Time: 11:23 PM
 */
public abstract class BaseEventObserver<E> implements EventDispatcher.EventObserver<E> {
  private final Class<E> eventClass;

  public BaseEventObserver(Class<E> eventClass) {this.eventClass = eventClass;}

  @Override public final Class getObservedEventType() {
    return eventClass;
  }
}
