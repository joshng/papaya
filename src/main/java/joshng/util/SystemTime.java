package joshng.util;

import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SystemTime implements WallClock {
    public static final DateTimeZone PACIFIC_TIMEZONE = DateTimeZone.forID("America/Los_Angeles");
    @SuppressWarnings({"unchecked"})
    public static final Ordering<Comparable> EARLIEST_TO_LATEST_ORDERING = Ordering.natural();
    public static final Ordering<Comparable> LATEST_TO_EARLIEST_ORDERING = EARLIEST_TO_LATEST_ORDERING.reverse();
    public static final DateTime ZERO = new DateTime(0).withZone(DateTimeZone.UTC);
    public static final DateTime MAX_VALUE = new DateTime(Long.MAX_VALUE).withZone(DateTimeZone.UTC);
    public static final DateTimeFormatter HUMAN_FRIENDLY_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z");

    @Deprecated // don't use this; @Inject SystemTime instead
    public SystemTime() {
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public DateTime now() {
        return new DateTime();
    }

    public DateTime plus(ReadableDuration duration) {
        return now().plus(duration);
    }

    public DateTime plus(ReadablePeriod duration) {
        return now().plus(duration);
    }

    public DateTime minus(ReadableDuration duration) {
        return now().minus(duration);
    }

    public DateTime minus(ReadablePeriod duration) {
        return now().minus(duration);
    }

    public Duration durationUntil(ReadableInstant endTime) {
        return new Duration(currentTimeMillis(), endTime.getMillis());
    }

    public Duration durationSince(ReadableInstant startTime) {
        return new Duration(startTime.getMillis(), currentTimeMillis());
    }
}
