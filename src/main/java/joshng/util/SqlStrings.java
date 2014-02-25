package joshng.util;


import javax.annotation.Nullable;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * User: josh
 * Date: 2/16/12
 * Time: 2:30 PM
 */
public class SqlStrings {
    private static final Pattern UNESCAPED_WILDCARD_PATTERN = Pattern.compile("(^|[^\\\\])[%_]");
    public static boolean containsWildcards(String sql) {
        return UNESCAPED_WILDCARD_PATTERN.matcher(sql).find();
    }

    private static final Pattern WILDCARD_PATTERN = Pattern.compile("[%_]");
    public static String escapeWildcards(String sql) {
        return WILDCARD_PATTERN.matcher(sql).replaceAll("\\\\$0");
    }

    @Nullable
    public static byte[] getBlobBytes(String columnName, ResultSet rs) throws SQLException {
        Blob blob = rs.getBlob(columnName);
        return blob == null ? null : blob.getBytes(1, (int) blob.length());
    }
}
