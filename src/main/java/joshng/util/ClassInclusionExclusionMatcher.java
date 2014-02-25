package joshng.util;

import javax.annotation.Nullable;

/**
 * User: josh
 * Date: Apr 27, 2011
 * Time: 5:54:55 PM
 */
public class ClassInclusionExclusionMatcher {
    private final SubclassMatcher inclusionMatcher;
    private final SubclassMatcher exclusionMatcher;

    public ClassInclusionExclusionMatcher(Class<?>[] includedClasses, Class<?>[] excludedClasses) {
        inclusionMatcher = constructMatcher(includedClasses);
        exclusionMatcher = constructMatcher(excludedClasses);
    }

    public boolean matches(Class<?> subclass) {
        return !isExcluded(subclass) && isIncluded(subclass);
    }

    private boolean isIncluded(Class<?> subclass) {
        return inclusionMatcher == null || inclusionMatcher.matches(subclass);
    }

    private boolean isExcluded(Class<?> subclass) {
        return exclusionMatcher != null && exclusionMatcher.matches(subclass);
    }

    @Nullable
    private SubclassMatcher constructMatcher(Class<?>[] includedClasses) {
        if (includedClasses == null) return null;
        if (includedClasses.length == 1) {
            return new SingleSuperclassSubclassMatcher(includedClasses[0]);
        } else {
            return new MultiSuperclassSubclassMatcher(includedClasses);
        }
    }

    public static abstract class SubclassMatcher {
        abstract boolean matches(Class<?> viewClass);
    }

    public static class SingleSuperclassSubclassMatcher extends SubclassMatcher {
        private final Class<?> superclass;

        public SingleSuperclassSubclassMatcher(Class<?> superclass) {
            this.superclass = superclass;
        }

        public boolean matches(Class<?> subclass) {
            return superclass.isAssignableFrom(subclass);
        }
    }

    public static class MultiSuperclassSubclassMatcher extends SubclassMatcher {
        private final Class<?>[] configuredSuperclasses;

        public MultiSuperclassSubclassMatcher(Class<?>[] configuredSuperclasses) {
            this.configuredSuperclasses = configuredSuperclasses;
        }

        public boolean matches(Class<?> subclass) {
            for (Class<?> configuredView : configuredSuperclasses) {
                if (configuredView.isAssignableFrom(subclass)) return true;
            }

            return false;
        }
    }
}
