package com.joshng.test.nested;

import com.joshng.util.reflect.Reflect;
import com.joshng.util.blocks.ThrowingFunction;
import com.joshng.util.collect.FunIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.function.Predicate;

import static com.joshng.util.collect.Functional.extend;
import static com.joshng.util.collect.Functional.funListOf;

public class Nested extends Runner {
  private final Runner classRunner;
  private final ArrayList<Runner> childRunners = new ArrayList<>();
  private static final FunIterable<Class<? extends Annotation>> TEST_ANNOTATIONS = funListOf(Test.class, Before.class, After.class);
  private static final Predicate<Class<?>> isNestedTestClass = method -> Reflect.getAllDeclaredMethods(method).any(m -> TEST_ANNOTATIONS.any(m::isAnnotationPresent));

  public Nested(final Class<?> testClass) throws InitializationError {
    classRunner = new InnerRunner(testClass);

    extend(testClass.getDeclaredClasses())
            .filter(isNestedTestClass)
            .foreach(ThrowingFunction.unchecked(Nested::new)
                            .andThenSink(childRunners::add)
            );
  }

  @Override
  public Description getDescription() {
    Description suiteDescription = classRunner.getDescription();

    for (Runner childRunner : childRunners) {
      suiteDescription.addChild(childRunner.getDescription());
    }

    return suiteDescription;
  }

  @Override
  public void run(RunNotifier notifier) {
    classRunner.run(notifier);

    for (Runner childRunner : childRunners) {
      childRunner.run(notifier);
    }
  }
}
