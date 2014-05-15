package joshng.util.collect;

import com.google.common.collect.Ordering;
import joshng.util.MutableTimePeriod;
import org.joda.time.DateTime;

import static joshng.util.collect.Functional.extend;
import static joshng.util.collect.Functional.funPairs;

/**
 * User: josh
 * Date: 4/29/12
 * Time: 6:03 PM
 */
public class TimePeriodMap<K> extends ComputingMap<K, MutableTimePeriod> {
  @Override
  protected MutableTimePeriod computeDefaultValue(K key) {
    return new MutableTimePeriod();
  }

  public MutableTimePeriod expand(K key, DateTime time) {
    return get(key).expand(time);
  }

  /**
   * If the size of this map is greater than {@code maxSize}, removes the items that appear
   * first according to the given {@code ordering} (ie, the "minimum" entries) until {@code size() == maxSize}.
   * <p>
   * <p>The {@link MutableTimePeriod} class has several utility {@link Ordering} constants that are appropriate
   * for this application.
   *
   * @param maxSize  the maximum number of entries to leave in the map
   * @param ordering an {@link Ordering} to identify the entries to remove; the "minimum" entries will be dropped.
   * @see MutableTimePeriod#EARLIEST_START_ORDERING
   * @see MutableTimePeriod#LATEST_END_ORDERING
   */

  public void limitSize(int maxSize, Ordering<MutableTimePeriod> ordering) {
    int itemsToRemove = size() - maxSize;
    if (itemsToRemove > 0) {
      if (itemsToRemove == 1) {
        remove(funPairs(this).minByValues(ordering).getOrThrow().getKey());
      } else {
        keySet().removeAll(funPairs(this).sortByValues(ordering).limit(itemsToRemove).keys().toList());
      }
    }
  }

  public Maybe<DateTime> getEarliestRecordedTime() {
    return extend(values())
            .map(MutableTimePeriod.GET_START_TIME)
            .min(Ordering.natural());
  }
}
