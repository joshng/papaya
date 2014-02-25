package joshng.util;

import java.util.EnumSet;

import static com.google.common.base.Preconditions.*;

/**
 * User: josh
 * Date: 2/26/13
 * Time: 12:14 PM
 */
public class EnumBitfieldCodec<E extends Enum<E>> {
    private final Class<E> enumClass;
    private E[] enumConstants;

    public static <E extends Enum<E>> EnumBitfieldCodec<E> newCodec(Class<E> enumClass) {
        return new EnumBitfieldCodec<E>(enumClass);
    }

    public EnumBitfieldCodec(Class<E> enumClass) {
        this.enumClass = enumClass;
        enumConstants = enumClass.getEnumConstants();
        checkArgument(enumConstants.length <= 64, "Too many enum values (max 64 to fit in a single long)", enumClass);
    }

    public EnumSet<E> decode(long bits) {
        EnumSet<E> result = EnumSet.noneOf(enumClass);
        while (bits != 0) {
            result.add(enumConstants[Long.numberOfTrailingZeros(bits)]);
            bits ^= Long.lowestOneBit(bits);
        }
        return result;
    }

    public long encode(Iterable<E> flags) {
        long result = 0;
        for (E flag : flags) {
            result |= (1 << flag.ordinal());
        }
        return result;
    }
}
