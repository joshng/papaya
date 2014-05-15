package joshng.util;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

/**
 * User: josh
 * Date: 11/15/12
 * Time: 3:12 PM
 */
public interface WallClock {
  WallClock SYSTEM_CLOCK = new WallClock() {
    @Override
    public ReadableInstant now() {
      return new DateTime();
    }
  };

  ReadableInstant now();
}
