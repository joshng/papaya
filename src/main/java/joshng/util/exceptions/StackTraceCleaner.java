package joshng.util.exceptions;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

import java.util.Arrays;
import java.util.List;

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
        classNameFilter = Predicates.or(classNameFilter, orFilter);
    }

    /** builds a predicate that will match the provided classes, as well as any named inner classes declared inside them */
    public static Predicate<String> buildClassNameFilter(Class<?>... classesToFilter) {
        return buildClassNameFilter(Arrays.asList(classesToFilter));
    }

    /** builds a predicate that will match the provided classes, as well as any named inner classes declared inside them */
    public static Predicate<String> buildClassNameFilter(Iterable<Class<?>> classesToFilter) {
        return Predicates.in(ImmutableSet.copyOf(Iterables.concat(Iterables.transform(classesToFilter, new Function<Class<?>, Iterable<String>>() { public Iterable<String> apply(Class<?> from) {
            return Iterables.transform(Arrays.asList(ObjectArrays.concat(from, from.getDeclaredClasses())), new Function<Class<?>, String>() {
                public String apply(Class<?> from) {
                    return from.getName();
                }
            });
        } }))));
    }

    /** builds a predicate that will match the provided classes, as well as any named inner classes declared inside them,
     * as well as any "Enhanced" classes (ie, those whose names contain '$$'
     */
    public static Predicate<String> buildEnhancedClassNameFilter(Class<?>... classesToFilter) {
        return buildEnhancedClassNameFilter(Arrays.asList(classesToFilter));
    }

    /** builds a predicate that will match the provided classes, as well as any named inner classes declared inside them,
     * as well as any "Enhanced" classes (ie, those whose names contain '$$'
     */
    public static Predicate<String> buildEnhancedClassNameFilter(Iterable<Class<?>> classesToFilter) {
        return Predicates.or(buildClassNameFilter(classesToFilter), Predicates.containsPattern("\\$\\$"));
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
        return classNameFilter.apply(className) || className.contains("$$");
    }
}
