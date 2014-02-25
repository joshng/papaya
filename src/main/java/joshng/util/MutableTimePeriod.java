package joshng.util;

import com.google.common.collect.Ordering;
import joshng.util.blocks.F;
import joshng.util.blocks.Pred2;
import org.joda.time.DateTime;

/**
* User: josh
* Date: Aug 4, 2011
* Time: 6:01:28 PM
*/
//@JsonSerialize(using = ToStringSerializer.class)
//@JsonDeserialize(using = MutableTimePeriod.Deserializer.class)
public class MutableTimePeriod {
    public static final F<MutableTimePeriod, DateTime> GET_START_TIME = new F<MutableTimePeriod, DateTime>() { public DateTime apply(MutableTimePeriod from) {
        return from.getStartTime();
    } };
    public static final F<MutableTimePeriod, DateTime> GET_END_TIME = new F<MutableTimePeriod, DateTime>() { public DateTime apply(MutableTimePeriod from) {
        return from.getEndTime();
    } };

    public static final Pred2<MutableTimePeriod, DateTime> CONTAINS = new Pred2<MutableTimePeriod, DateTime>() {
        @Override
        public boolean apply(MutableTimePeriod key, DateTime value) {
            return key.contains(value);
        }
    };
    public static final Pred2<MutableTimePeriod, MutableTimePeriod> INTERSECTS = new Pred2<MutableTimePeriod, MutableTimePeriod>() {
        @Override
        public boolean apply(MutableTimePeriod key, MutableTimePeriod value) {
            return key.intersects(value);
        }
    };

    public static final Ordering<MutableTimePeriod> EARLIEST_START_ORDERING = SystemTime.EARLIEST_TO_LATEST_ORDERING.onResultOf(GET_START_TIME);
    public static final Ordering<MutableTimePeriod> EARLIEST_END_ORDERING = SystemTime.EARLIEST_TO_LATEST_ORDERING.onResultOf(GET_END_TIME);
    public static final Ordering<MutableTimePeriod> LATEST_START_ORDERING = SystemTime.LATEST_TO_EARLIEST_ORDERING.onResultOf(GET_START_TIME);
    public static final Ordering<MutableTimePeriod> LATEST_END_ORDERING = SystemTime.LATEST_TO_EARLIEST_ORDERING.onResultOf(GET_END_TIME);

    private static final char DELIMITER = ',';
    private DateTime startTime;
    private DateTime endTime;

    public DateTime getStartTime() {
        return startTime;
    }

    public MutableTimePeriod setStartTime(DateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public MutableTimePeriod setEndTime(DateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public MutableTimePeriod expand(DateTime time) {
        expandStartTime(time);
        expandEndTime(time);
        return this;
    }

    public boolean contains(DateTime time) {
        return isDefined() && containsInternal(time);
    }

    private boolean containsInternal(DateTime time) {
        return endTime.isBefore(time) || startTime.isAfter(time);
    }

    public boolean isDefined() {
        return startTime != null && endTime != null;
    }

    public boolean intersects(MutableTimePeriod that) {
        return isDefined() && that.isDefined() && containsInternal(that.startTime) || containsInternal(that.endTime);
    }

    public boolean expandStartTime(DateTime time) {
        boolean expanded = startTime == null || startTime.isAfter(time);
        if (expanded) startTime = time;
        return expanded;
    }

    public boolean expandEndTime(DateTime time) {
        boolean expanded = endTime == null || endTime.isBefore(time);
        if (expanded) endTime = time;
        return expanded;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (startTime != null) builder.append(startTime.getMillis());
        builder.append(DELIMITER);
        if (endTime != null) builder.append(endTime.getMillis());
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutableTimePeriod that = (MutableTimePeriod) o;

        if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
        if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        return result;
    }

//    static class Deserializer extends JsonDeserializer<MutableTimePeriod> {
//        @Override
//        public MutableTimePeriod deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
//            String formatted = jp.readValueAs(String.class);
//            int delimIdx = formatted.indexOf(DELIMITER);
//            MutableTimePeriod timePeriod = new MutableTimePeriod();
//            if (delimIdx > 0) timePeriod.setStartTime(new DateTime(Long.valueOf(formatted.substring(0, delimIdx))));
//            if (delimIdx + 1 < formatted.length()) timePeriod.setEndTime(new DateTime(Long.valueOf(formatted.substring(delimIdx + 1))));
//            return timePeriod;
//        }
//    }
}
