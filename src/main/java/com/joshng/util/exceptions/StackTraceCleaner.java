package com.joshng.util.exceptions;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.joshng.util.blocks.Pred;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: Jul 15, 2011
 * Time: 12:11:33 AM
 */
public class StackTraceCleaner {
  Predicate<String> classNameFilter;

  public StackTraceCleaner(Predicate<String> classNameFilter) {
    this.classNameFilter = classNameFilter;
  }

  public StackTraceCleaner(Iterable<Class<?>> classesToFilter) {
    this(buildEnhancedClassNameFilter(classesToFilter));
  }

  public Predicate<String> getClassNameFilter() {
    return classNameFilter;
  }

  public void setClassNameFilter(Predicate<String> classNameFilter) {
    this.classNameFilter = classNameFilter;
  }

  public void addClassNameFilter(Predicate<String> orFilter) {
    classNameFilter = classNameFilter.or(orFilter);
  }

  /**
   * builds a predicate that will match the provided classes, as well as any named inner classes declared inside them
   */
  public static Predicate<String> buildClassNameFilter(Class<?>... classesToFilter) {
    return buildClassNameFilter(Arrays.asList(classesToFilter));
  }

  /**
   * builds a predicate that will match the provided classes, as well as any named inner classes declared inside them
   */
  public static Pred<String> buildClassNameFilter(Iterable<Class<?>> classesToFilter) {
    return extend(classesToFilter)
            .flatMap(cls -> extend(cls.getDeclaredClasses()).prepend(cls).map(Class::getName))
            .toSet()::contains;
  }

  /**
   * builds a predicate that will match the provided classes, as well as any named inner classes declared inside them,
   * as well as any "Enhanced" classes (ie, those whose names contain '$$'
   */
  public static Predicate<String> buildEnhancedClassNameFilter(Class<?>... classesToFilter) {
    return buildEnhancedClassNameFilter(Arrays.asList(classesToFilter));
  }

  /**
   * builds a predicate that will match the provided classes, as well as any named inner classes declared inside them,
   * as well as any "Enhanced" classes (ie, those whose names contain '$$'
   */
  public static Predicate<String> buildEnhancedClassNameFilter(Iterable<Class<?>> classesToFilter) {
    return buildClassNameFilter(classesToFilter).or(Predicates.containsPattern("\\$\\$")::apply);
  }


  public <T extends Throwable> T clean(T e) {
    for (Throwable cause : Throwables.getCausalChain(e)) {
      StackTraceElement[] unfiltered = cause.getStackTrace();
      List<StackTraceElement> filteredTrace = Lists.newArrayListWithCapacity(unfiltered.length);
      boolean filteredPrev = false;
      for (StackTraceElement element : unfiltered) {
        boolean filtered = isFiltered(element.getClassName());
        if (!(filteredPrev && filtered)) filteredTrace.add(element);
        filteredPrev = filtered;
      }
      cause.setStackTrace(filteredTrace.toArray(new StackTraceElement[filteredTrace.size()]));
    }
    return e;
  }

  public boolean isFiltered(String className) {
    return classNameFilter.test(className) || className.contains("$$");
  }
}
