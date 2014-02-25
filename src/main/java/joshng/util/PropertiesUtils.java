package joshng.util;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import joshng.util.blocks.F;
import joshng.util.collect.FunPairs;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import static joshng.util.collect.Functional.extend;
import static joshng.util.collect.Functional.funPairs;

public class PropertiesUtils {
    public static Properties load(Iterable<URL> urls) throws IOException {
        Properties properties = new Properties();
        for (URL url : urls) {
//            properties = new java.util.Properties(properties);
            properties.load(url.openStream());
        }
        return properties;
    }

    @SuppressWarnings({"unchecked"})
    public static FunPairs<String,String> propertyPairs(final Properties properties) {
        return extend(Iterators.forEnumeration((Enumeration<String>)properties.propertyNames()))
                .toList() // make a copy to allow repeated iteration
                .asKeysTo(new F<String, String>() {
                    @Override
                    public String apply(String input) {
                        return properties.getProperty(input);
                    }
                });
    }


    public static void dumpToLog(Logger log, String from, Properties properties) {
        dumpToLog(log, from, propertyPairs(properties));
    }

    public static void dumpToLog(Logger log, String from, Iterable<Map.Entry<String, String>> properties) {
        if (log.isInfoEnabled()) log.info("Configuration from {}:\n{}", from, format(properties));
    }

    public static String format(Iterable<? extends Map.Entry<String, String>> properties) {
        StringBuilder builder = new StringBuilder();
        try {
            append(properties, builder);
        } catch (IOException e) {
            // can't happen
            throw Throwables.propagate(e);
        }
        return builder.toString();
    }

    public static void append(Iterable<? extends Map.Entry<String, String>> properties, Appendable builder) throws IOException {
        FunPairs<String, String> sortedPairs = funPairs(properties).sortByKeys(Ordering.natural());
        int maxKeyLength = sortedPairs.keys().map(StringUtils.GET_STRING_LENGTH).max(Ordering.natural()).getOrElse(0);

        for (Map.Entry<String, String> entry : sortedPairs) {
            String name = entry.getKey();
            builder.append("  ").append(Strings.padEnd(name, maxKeyLength, ' ')).append(" = ");
            String value = entry.getValue();
            if (value.length() > 0) {
                // filter password properties
                String lowercase = name.toLowerCase();
                if (lowercase.contains("password") || lowercase.contains("secret")) value = "<omitted>";
            }
            builder.append(value).append("\n");
        }
    }
}
