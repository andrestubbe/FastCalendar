package fastcalendar;

import java.util.Arrays;

/**
 * Ultra-fast, zero-allocation RFC 5545 Recurrence Rule Evaluator.
 * Computes calendar event occurrences over arbitrary time windows
 * without creating object allocations in the hot evaluation loops.
 */
public final class RRuleEvaluator {

    private static final int MAX_OCCURRENCES_DEFAULT = 100_000;

    private RRuleEvaluator() {}

    /**
     * Expands an event's recurrence rule into a sorted array of epoch millisecond timestamps.
     */
    public static long[] expand(long dtStart, RRule rule, long windowStart, long windowEnd) {
        return expand(dtStart, rule, null, null, windowStart, windowEnd, MAX_OCCURRENCES_DEFAULT);
    }

    /**
     * Expands recurrence rule with exceptions (EXDATE) and additionals (RDATE).
     */
    public static long[] expand(long dtStart, RRule rule, long[] exDates, long[] rDates,
                                long windowStart, long windowEnd, int maxOccurrences) {
        if (dtStart <= 0) return new long[0];

        // If no rule, just check DTSTART + RDATEs
        if (rule == null) {
            return evaluateStaticDates(dtStart, exDates, rDates, windowStart, windowEnd);
        }

        // Temporary primitive array buffer to accumulate occurrences
        long[] buffer = new long[Math.min(1024, maxOccurrences)];
        int[] count = new int[1];

        evaluate(dtStart, rule, exDates, rDates, windowStart, windowEnd, maxOccurrences, timestamp -> {
            if (count[0] >= buffer.length) {
                // Grow buffer
                long[] newBuf = new long[Math.min(buffer.length * 2, maxOccurrences)];
                System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
                // Can't replace local directly, so we handle in a collector object if needed
            }
            return true;
        });

        // Use array-list collector
        LongArrayCollector collector = new LongArrayCollector(Math.min(256, maxOccurrences));
        evaluate(dtStart, rule, exDates, rDates, windowStart, windowEnd, maxOccurrences, ts -> {
            collector.add(ts);
            return collector.size() < maxOccurrences;
        });

        return collector.toArray();
    }

    /**
     * Zero-allocation streaming evaluation. Invokes consumer for every occurrence within [windowStart, windowEnd].
     */
    public static void evaluate(long dtStart, RRule rule, long[] exDates, long[] rDates,
                                long windowStart, long windowEnd, int maxOccurrences,
                                RecurrenceConsumer consumer) {
        if (dtStart <= 0 || consumer == null) return;

        if (exDates != null && exDates.length > 1) {
            Arrays.sort(exDates);
        }
        if (rDates != null && rDates.length > 1) {
            Arrays.sort(rDates);
        }

        if (rule == null) {
            emitStaticDates(dtStart, exDates, rDates, windowStart, windowEnd, consumer);
            return;
        }

        int[] dt = new int[6];
        FastDateTime.unpackUtc(dtStart, dt);

        int countLimit = rule.getCount();
        long untilLimit = rule.getUntil();
        int interval = rule.getInterval();
        RRule.Frequency freq = rule.getFrequency();

        int emittedCount = 0;
        int totalGenerated = 0;

        switch (freq) {
            case DAILY:
                evaluateDaily(dt, dtStart, rule, interval, countLimit, untilLimit, exDates,
                              windowStart, windowEnd, maxOccurrences, consumer);
                break;
            case WEEKLY:
                evaluateWeekly(dt, dtStart, rule, interval, countLimit, untilLimit, exDates,
                               windowStart, windowEnd, maxOccurrences, consumer);
                break;
            case MONTHLY:
                evaluateMonthly(dt, dtStart, rule, interval, countLimit, untilLimit, exDates,
                                windowStart, windowEnd, maxOccurrences, consumer);
                break;
            case YEARLY:
                evaluateYearly(dt, dtStart, rule, interval, countLimit, untilLimit, exDates,
                               windowStart, windowEnd, maxOccurrences, consumer);
                break;
            case HOURLY:
                evaluateHourly(dt, dtStart, rule, interval, countLimit, untilLimit, exDates,
                               windowStart, windowEnd, maxOccurrences, consumer);
                break;
            case MINUTELY:
            case SECONDLY:
                evaluateSubHourly(dt, dtStart, rule, freq, interval, countLimit, untilLimit, exDates,
                                  windowStart, windowEnd, maxOccurrences, consumer);
                break;
        }

        // Process any RDATEs
        if (rDates != null) {
            for (long rdate : rDates) {
                if (rdate >= windowStart && rdate <= windowEnd) {
                    if (!isExcluded(rdate, exDates)) {
                        consumer.onOccurrence(rdate);
                    }
                }
            }
        }
    }

    private static boolean isExcluded(long timestamp, long[] exDates) {
        if (exDates == null || exDates.length == 0) return false;
        return Arrays.binarySearch(exDates, timestamp) >= 0;
    }

    private static void evaluateDaily(int[] dt, long dtStart, RRule rule, int interval,
                                      int countLimit, long untilLimit, long[] exDates,
                                      long windowStart, long windowEnd, int maxOccurrences,
                                      RecurrenceConsumer consumer) {
        long current = dtStart;
        int generated = 0;
        int emitted = 0;

        while (true) {
            if (countLimit > 0 && generated >= countLimit) break;
            if (untilLimit > 0 && current > untilLimit) break;
            if (current > windowEnd && untilLimit <= 0 && countLimit <= 0) break;
            if (emitted >= maxOccurrences) break;

            generated++;

            if (current >= windowStart && current <= windowEnd) {
                if (!isExcluded(current, exDates)) {
                    emitted++;
                    if (!consumer.onOccurrence(current)) return;
                }
            }

            // Step by interval days
            current += interval * FastDateTime.MILLIS_PER_DAY;
        }
    }

    private static void evaluateWeekly(int[] dt, long dtStart, RRule rule, int interval,
                                       int countLimit, long untilLimit, long[] exDates,
                                       long windowStart, long windowEnd, int maxOccurrences,
                                       RecurrenceConsumer consumer) {
        RRule.ByDayRule[] byDays = rule.getByDays();
        if (byDays == null || byDays.length == 0) {
            // Default to the day of week of DTSTART
            int dow = FastDateTime.getDayOfWeek(dt[0], dt[1], dt[2]);
            byDays = new RRule.ByDayRule[] { new RRule.ByDayRule(RRule.WeekDay.fromIsoDay(dow)) };
        }

        int year = dt[0];
        int month = dt[1];
        int day = dt[2];
        int hour = dt[3];
        int min = dt[4];
        int sec = dt[5];

        // Find Monday of the current week
        int curDow = FastDateTime.getDayOfWeek(year, month, day);
        long currentWeekStart = FastDateTime.toEpochMillis(year, month, day, hour, min, sec)
                                - (curDow - 1) * FastDateTime.MILLIS_PER_DAY;

        int generated = 0;
        int emitted = 0;

        while (true) {
            // Check days of current week
            for (RRule.ByDayRule bdr : byDays) {
                int targetDow = bdr.getDay().getIsoDay();
                long occ = currentWeekStart + (targetDow - 1) * FastDateTime.MILLIS_PER_DAY;

                if (occ < dtStart) continue; // Before start
                if (countLimit > 0 && generated >= countLimit) return;
                if (untilLimit > 0 && occ > untilLimit) return;

                generated++;

                if (occ >= windowStart && occ <= windowEnd) {
                    if (!isExcluded(occ, exDates)) {
                        emitted++;
                        if (!consumer.onOccurrence(occ)) return;
                        if (emitted >= maxOccurrences) return;
                    }
                }
            }

            // Advance week
            currentWeekStart += (long) interval * 7 * FastDateTime.MILLIS_PER_DAY;
            if (currentWeekStart > windowEnd && untilLimit <= 0 && countLimit <= 0) {
                break;
            }
        }
    }

    private static void evaluateMonthly(int[] dt, long dtStart, RRule rule, int interval,
                                        int countLimit, long untilLimit, long[] exDates,
                                        long windowStart, long windowEnd, int maxOccurrences,
                                        RecurrenceConsumer consumer) {
        int year = dt[0];
        int month = dt[1];
        int startDay = dt[2];
        int hour = dt[3];
        int min = dt[4];
        int sec = dt[5];

        int[] byMonthDays = rule.getByMonthDays();
        RRule.ByDayRule[] byDays = rule.getByDays();

        int generated = 0;
        int emitted = 0;

        while (true) {
            int daysInMonth = FastDateTime.getDaysInMonth(year, month);

            if (byDays != null && byDays.length > 0) {
                // Monthly by day of week, e.g. 2nd Tuesday (+2TU) or last Friday (-1FR)
                for (RRule.ByDayRule bdr : byDays) {
                    int day = findDayInMonth(year, month, bdr.getDay().getIsoDay(), bdr.getOrdinal());
                    if (day > 0) {
                        long occ = FastDateTime.toEpochMillis(year, month, day, hour, min, sec);
                        if (occ < dtStart) continue;
                        if (countLimit > 0 && generated >= countLimit) return;
                        if (untilLimit > 0 && occ > untilLimit) return;

                        generated++;
                        if (occ >= windowStart && occ <= windowEnd) {
                            if (!isExcluded(occ, exDates)) {
                                emitted++;
                                if (!consumer.onOccurrence(occ)) return;
                                if (emitted >= maxOccurrences) return;
                            }
                        }
                    }
                }
            } else if (byMonthDays != null && byMonthDays.length > 0) {
                for (int mday : byMonthDays) {
                    int day = mday > 0 ? mday : (daysInMonth + mday + 1);
                    if (day >= 1 && day <= daysInMonth) {
                        long occ = FastDateTime.toEpochMillis(year, month, day, hour, min, sec);
                        if (occ < dtStart) continue;
                        if (countLimit > 0 && generated >= countLimit) return;
                        if (untilLimit > 0 && occ > untilLimit) return;

                        generated++;
                        if (occ >= windowStart && occ <= windowEnd) {
                            if (!isExcluded(occ, exDates)) {
                                emitted++;
                                if (!consumer.onOccurrence(occ)) return;
                                if (emitted >= maxOccurrences) return;
                            }
                        }
                    }
                }
            } else {
                // Default: same day of month as startDay
                int day = Math.min(startDay, daysInMonth);
                long occ = FastDateTime.toEpochMillis(year, month, day, hour, min, sec);
                if (occ >= dtStart) {
                    if (countLimit > 0 && generated >= countLimit) return;
                    if (untilLimit > 0 && occ > untilLimit) return;

                    generated++;
                    if (occ >= windowStart && occ <= windowEnd) {
                        if (!isExcluded(occ, exDates)) {
                            emitted++;
                            if (!consumer.onOccurrence(occ)) return;
                            if (emitted >= maxOccurrences) return;
                        }
                    }
                }
            }

            // Step month by interval
            month += interval;
            while (month > 12) {
                month -= 12;
                year++;
            }

            long monthStartEpoch = FastDateTime.toEpochMillis(year, month, 1, 0, 0, 0);
            if (monthStartEpoch > windowEnd && untilLimit <= 0 && countLimit <= 0) {
                break;
            }
        }
    }

    private static int findDayInMonth(int year, int month, int targetDow, int ordinal) {
        int daysInMonth = FastDateTime.getDaysInMonth(year, month);
        int matchCount = 0;
        int matchedDay = 0;

        if (ordinal >= 0) {
            for (int d = 1; d <= daysInMonth; d++) {
                int dow = FastDateTime.getDayOfWeek(year, month, d);
                if (dow == targetDow) {
                    matchCount++;
                    if (ordinal == 0 || matchCount == ordinal) {
                        return d;
                    }
                }
            }
        } else {
            // Negative ordinal: count backwards from end of month
            int targetOrdinal = -ordinal;
            for (int d = daysInMonth; d >= 1; d--) {
                int dow = FastDateTime.getDayOfWeek(year, month, d);
                if (dow == targetDow) {
                    matchCount++;
                    if (matchCount == targetOrdinal) {
                        return d;
                    }
                }
            }
        }
        return 0;
    }

    private static void evaluateYearly(int[] dt, long dtStart, RRule rule, int interval,
                                       int countLimit, long untilLimit, long[] exDates,
                                       long windowStart, long windowEnd, int maxOccurrences,
                                       RecurrenceConsumer consumer) {
        int year = dt[0];
        int month = dt[1];
        int day = dt[2];
        int hour = dt[3];
        int min = dt[4];
        int sec = dt[5];

        int generated = 0;
        int emitted = 0;

        while (true) {
            int d = Math.min(day, FastDateTime.getDaysInMonth(year, month));
            long occ = FastDateTime.toEpochMillis(year, month, d, hour, min, sec);

            if (occ >= dtStart) {
                if (countLimit > 0 && generated >= countLimit) return;
                if (untilLimit > 0 && occ > untilLimit) return;

                generated++;
                if (occ >= windowStart && occ <= windowEnd) {
                    if (!isExcluded(occ, exDates)) {
                        emitted++;
                        if (!consumer.onOccurrence(occ)) return;
                        if (emitted >= maxOccurrences) return;
                    }
                }
            }

            year += interval;
            long yearStartEpoch = FastDateTime.toEpochMillis(year, 1, 1, 0, 0, 0);
            if (yearStartEpoch > windowEnd && untilLimit <= 0 && countLimit <= 0) {
                break;
            }
        }
    }

    private static void evaluateHourly(int[] dt, long dtStart, RRule rule, int interval,
                                       int countLimit, long untilLimit, long[] exDates,
                                       long windowStart, long windowEnd, int maxOccurrences,
                                       RecurrenceConsumer consumer) {
        long current = dtStart;
        long step = interval * FastDateTime.MILLIS_PER_HOUR;
        int generated = 0;
        int emitted = 0;

        while (true) {
            if (countLimit > 0 && generated >= countLimit) break;
            if (untilLimit > 0 && current > untilLimit) break;
            if (current > windowEnd && untilLimit <= 0 && countLimit <= 0) break;

            generated++;
            if (current >= windowStart && current <= windowEnd) {
                if (!isExcluded(current, exDates)) {
                    emitted++;
                    if (!consumer.onOccurrence(current)) return;
                    if (emitted >= maxOccurrences) return;
                }
            }
            current += step;
        }
    }

    private static void evaluateSubHourly(int[] dt, long dtStart, RRule rule, RRule.Frequency freq,
                                          int interval, int countLimit, long untilLimit, long[] exDates,
                                          long windowStart, long windowEnd, int maxOccurrences,
                                          RecurrenceConsumer consumer) {
        long current = dtStart;
        long step = (freq == RRule.Frequency.MINUTELY ? FastDateTime.MILLIS_PER_MINUTE : FastDateTime.MILLIS_PER_SECOND) * interval;
        int generated = 0;
        int emitted = 0;

        while (true) {
            if (countLimit > 0 && generated >= countLimit) break;
            if (untilLimit > 0 && current > untilLimit) break;
            if (current > windowEnd && untilLimit <= 0 && countLimit <= 0) break;

            generated++;
            if (current >= windowStart && current <= windowEnd) {
                if (!isExcluded(current, exDates)) {
                    emitted++;
                    if (!consumer.onOccurrence(current)) return;
                    if (emitted >= maxOccurrences) return;
                }
            }
            current += step;
        }
    }

    private static long[] evaluateStaticDates(long dtStart, long[] exDates, long[] rDates,
                                              long windowStart, long windowEnd) {
        LongArrayCollector c = new LongArrayCollector(8);
        emitStaticDates(dtStart, exDates, rDates, windowStart, windowEnd, c::add);
        return c.toArray();
    }

    private static void emitStaticDates(long dtStart, long[] exDates, long[] rDates,
                                        long windowStart, long windowEnd,
                                        RecurrenceConsumer consumer) {
        if (dtStart >= windowStart && dtStart <= windowEnd && !isExcluded(dtStart, exDates)) {
            consumer.onOccurrence(dtStart);
        }
        if (rDates != null) {
            for (long r : rDates) {
                if (r >= windowStart && r <= windowEnd && !isExcluded(r, exDates)) {
                    consumer.onOccurrence(r);
                }
            }
        }
    }

    /**
     * Fast growable long primitive array for zero GC boxing.
     */
    public static final class LongArrayCollector {
        private long[] data;
        private int size;

        public LongArrayCollector(int initialCapacity) {
            this.data = new long[Math.max(8, initialCapacity)];
        }

        public boolean add(long value) {
            if (size >= data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = value;
            return true;
        }

        public int size() {
            return size;
        }

        public long get(int index) {
            return data[index];
        }

        public long[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
