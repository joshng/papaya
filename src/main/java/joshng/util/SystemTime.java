package joshng.util;

import com.google.common.collect.Ordering;

import java.time.ZoneId;

public class SystemTime {
  public static final ZoneId PACIFIC_ZONE_ID = ZoneId.of("America/Los_Angeles");
  @SuppressWarnings({"unchecked"})
  public static final Ordering<Comparable> EARLIEST_TO_LATEST_ORDERING = Ordering.natural();
  public static final Ordering<Comparable> LATEST_TO_EARLIEST_ORDERING = EARLIEST_TO_LATEST_ORDERING.reverse();
}
