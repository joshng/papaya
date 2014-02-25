package joshng.util;

import joshng.util.blocks.F;
import joshng.util.collect.ImmutableMaybeMap;

import java.io.Serializable;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: Apr 15, 2011
 * Time: 6:58:21 PM
 */
public class Identifier implements Serializable {
    protected final String identifier;

    public Identifier(String identifier) {
        checkNotNull(identifier, "identifier");
        this.identifier = identifier;
    }

    public static String generateRandomIdentifier() {
        return UUID.randomUUID().toString();
    }

    private static final Pattern HYPHEN = Pattern.compile("-");
    public static String generate32HexIdentifier() {
        return HYPHEN.matcher(generateRandomIdentifier()).replaceAll("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identifier that = (Identifier) o;

        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier;
    }

    public static class SealedRegistry<I extends Identifier> extends ImmutableMaybeMap<String, I> {

        public SealedRegistry(Iterable<I> entries) {
            super(extend(entries).asValuesFrom(F.toStringer()));
        }

        public boolean isValid(String key) {
            return containsKey(key);
        }
    }
}
