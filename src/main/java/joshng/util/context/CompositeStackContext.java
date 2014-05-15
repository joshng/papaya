package joshng.util.context;

import com.google.common.collect.ImmutableList;
import joshng.util.exceptions.MultiException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * User: josh
 * Date: 11/21/11
 * Time: 12:41 PM
 */
public class CompositeStackContext extends StackContext {
  private final List<? extends TransientContext> contexts;

  public CompositeStackContext(List<? extends TransientContext> contexts) {
    this.contexts = ImmutableList.copyOf(contexts);
  }

  public CompositeState enter() {
    return new CompositeState();
  }

  private class CompositeState implements State {
    private final Deque<State> states = new ArrayDeque<>(contexts.size());

    CompositeState() {
      try {
        for (TransientContext context : contexts) {
          states.push(context.enter());
        }
      } catch (RuntimeException e) {
        exit(MultiException.Empty.with(e));
      }
    }

    public void exit() {
      exit(MultiException.Empty);
    }

    private void exit(MultiException exception) {
      while (!states.isEmpty()) {
        try {
          states.pop().exit();
        } catch (RuntimeException e) {
          exception = exception.with(e);
        }
      }

      exception.throwRuntimeIfAny();
    }
  }
}
