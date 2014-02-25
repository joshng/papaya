package joshng.util;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import joshng.util.blocks.F;
import joshng.util.collect.Pair;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: Jan 31, 2010
 * Time: 11:23:41 PM
 */
public class StringUtils {
    public static final F<String, String> TO_LOWER = new F<String, String>() {
        public String apply(String input) {
            return input.toLowerCase();
        }
    };
    public static final F<String, String> TO_UPPER = new F<String, String>() {
        public String apply(String input) {
            return input.toUpperCase();
        }
    };
    //private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");
	private static final Pattern LAST_NONWORD_PATTERN = Pattern.compile("\\W\\w*$");
	public static final Charset UTF8_CHARSET = Charsets.UTF_8;
    public static final Ordering<String> CASE_INSENSITIVE_ORDERING = Ordering.from(String.CASE_INSENSITIVE_ORDER);
    public static final Function<byte[],String> UTF8_BYTES_TO_STRING = new Function<byte[], String>() { public String apply(byte[] bytes) {
        return StringUtils.fromUTF8Bytes(bytes);
    } };

    public static byte[] toUTF8Bytes(String str) {
		return str.getBytes(UTF8_CHARSET);
	}

	public static String fromUTF8Bytes(byte[] bytes) {
		return new String(bytes, UTF8_CHARSET);
	}

	public static String toHexString(byte[] bytes) {
        return toHexString(bytes, 0, bytes.length);
	}

    public static String toHexString(byte[] bytes, int offset, int len) {
        StringBuilder hex = new StringBuilder(len * 2);
        for (int i = offset; i < len; i++) {
            hex.append(Character.forDigit((bytes[i] & 0XF0) >> 4, 16));
            hex.append(Character.forDigit((bytes[i] & 0X0F), 16));
        }
        return hex.toString();
    }


    public static F<String, String> trailingSubstringer(final int startOffset) {
        return new F<String, String>() {
            @Override
            public String apply(String input) {
                return input.substring(startOffset);
            }
        };
    }

    public static F<String, String> leadingSubstringer(final int length) {
        return new F<String, String>() {
            @Override
            public String apply(String input) {
                return input.substring(0, length);
            }
        };
    }

    public static F<String, String> appender(final String suffix) {
        return new F<String, String>() {
            @Override public String apply(String input) {
                return input + suffix;
            }
        };
    }

    public static F<String, String> prepender(final String prefix) {
        return new F<String, String>() {
            @Override public String apply(String input) {
                return prefix + input;
            }
        };
    }

    public static <I extends Iterable<?>> F<I, String> joinerOn(char delimiter) {
        return joiner(Joiner.on(delimiter));
    }

    public static <I extends Iterable<?>> F<I, String> joinerOn(String delimiter) {
        return joiner(Joiner.on(delimiter));
    }

    public static <I extends Iterable<?>> F<I, String> joiner(final Joiner joiner) {
        return new F<I, String>() {
            @Override public String apply(I input) {
                return joiner.join(input);
            }
        };
    }

    public static final F<String, Integer> GET_STRING_LENGTH = new F<String, Integer>() {
        @Override
        public Integer apply(String input) {
            return input.length();
        }
    };

    public static String ifNullOrEmpty(String s, String replacement) {
        return Strings.isNullOrEmpty(s) ? replacement : s;
    }

	public static String truncateByCharLength(String string, int maxLength) {
		return string.length() > maxLength ? string.substring(0, maxLength) : string;
	}

//	public static String capitalizeAllWords(String string) {
//		return capitalizeAllWords(string, WORD_SEPARATOR);
//	}
//
//	public static String capitalizeAllWords(String string, Pattern wordSeparator) {
//
//	}

	public static String capitalizeFirstChar(String word) {
		if (word.length() == 0) {
			return word;
		}
		char[] chars = word.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}

    private static final Pattern underscorePattern = Pattern.compile("(_)(.)");
    public static String camelizeUnderscores(String word) {
        // remove all underscores
        Matcher m = underscorePattern.matcher(word);
        while (m.find()) {
            word = m.replaceFirst(m.group(2).toUpperCase());
            m = underscorePattern.matcher(word);
        }

        return word;
    }

    public static String safeSubstring(String input, int startIdx, int endIdx) {
        int len = input.length();
        return input.substring(Math.min(startIdx, len), Math.min(endIdx, len));
    }

    public static String truncateByCharLengthWithEllipsis(String string, int maxLength) {
        return string.length() <= maxLength ? string : truncateByCharLength(string, maxLength - 3) + "...";
    }

    public static String truncateByUTF8ByteLength(String string, int maxBytes) {
        return truncateByByteLength(string, maxBytes, Charsets.UTF_8);
    }

    public static String truncateByByteLength(String string, int maxBytes, Charset charset) {
        CharsetEncoder ce = charset.newEncoder();
        if (string.length() * ce.maxBytesPerChar() <= maxBytes) return string;

        CharBuffer chars = CharBuffer.wrap(string);
        ByteBuffer bytes = ByteBuffer.allocate(maxBytes);
        CoderResult result = ce.encode(chars, bytes, true);
        return result.isOverflow() ? new String(bytes.array(), 0, bytes.position(), charset) : string;
    }

    public static String truncateOnWordBoundaryWithEllipsis(String string, int maxLength) {
        if (string.length() <= maxLength) {
            return string;
        }
        Matcher matcher = LAST_NONWORD_PATTERN.matcher(string);
        matcher.region(0, maxLength - 2);
        int truncateIdx = matcher.find() ? matcher.start() : maxLength - 3;

        return string.substring(0, truncateIdx) + "...";
    }

    /**
     * Splits the given string at the first occurrence of the given {@code separator}.<br/><br/>
     * The separator character is OMITTED from the resulting strings.
     * @param separator the char to find
     * @param str the string to split
     * @return Pair.of(str.substring(0, sepIdx), str.substring(sepIdx + 1))
     * @throws IllegalArgumentException if the string does not contain the requested separator
     */
    public static Pair<String, String> splitOnFirst(char separator, String str) {
        int sepIdx = str.indexOf(separator);
        checkArgument(sepIdx >= 0, "Separator not found", separator, str);
        return splitAroundIndex(sepIdx, str);
    }

    /**
     * Splits the given string at the last occurrence of the given {@code separator}.<br/><br/>
     * The separator character is OMITTED from the resulting strings.
     * @param separator the char to find
     * @param str the string to split
     * @return Pair.of(str.substring(0, sepIdx), str.substring(sepIdx + 1))
     * @throws IllegalArgumentException if the string does not contain the requested separator
     */
    public static Pair<String, String> splitOnLast(char separator, String str) {
        int sepIdx = str.lastIndexOf(separator);
        checkArgument(sepIdx >= 0, "Separator not found", separator, str);
        return splitAroundIndex(sepIdx, str);
    }

    /**
     * Splits the given string "around" the given {@code index}.<br/><br/>
     * The character at the index is OMITTED from the resulting strings.
     *
     * @param index the index to split around
     * @param str the string to split
     * @return Pair.of(str.substring(0, index), str.substring(index + 1))
     * @throws IndexOutOfBoundsException if the index is outside the range of the given string
     */
    public static Pair<String, String> splitAroundIndex(int index, String str) {
        return Pair.of(str.substring(0, index), str.substring(index + 1));
    }

    /**
     * @param index index to split the input string
     * @param str string to split
     * @return Pair.of(str.substring(0, index), str.substring(index)) // INCLUDES the char at the index
     */
    public static Pair<String, String> splitAtIndex(int index, String str) {
        return Pair.of(str.substring(0, index), str.substring(index));
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These
     * are matched by position - the first {@code %s} gets {@code args[0]}, etc.
     * If there are more arguments than placeholders, the unmatched arguments will
     * be appended to the end of the formatted message in square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s}
     *     placeholders.
     * @param args the arguments to be substituted into the message
     *     template. Arguments are converted to strings using
     *     {@link String#valueOf(Object)}. Arguments can be null.
     */
    // [copied from com.google.common.base.Preconditions.format]
    public static String format(String template,
                                @Nullable Object... args) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(
                template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template.substring(templateStart, placeholderStart));
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template.substring(templateStart));

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append("]");
        }

        return builder.toString();
    }
}
