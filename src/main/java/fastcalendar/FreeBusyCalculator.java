package fastcalendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * High-Speed Free/Busy slot calculation and multi-attendee meeting scheduler.
 * Implements interval merging and sweep-line algorithms for optimal slot matching.
 */
public final class FreeBusyCalculator {

    public static final class TimeSlot {
        private final long start;
        private final long end;

        public TimeSlot(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() { return start; }
        public long getEnd() { return end; }
        public long getDurationMillis() { return end - start; }

        @Override
        public String toString() {
            return FastDateTime.formatUtc(start) + " -> " + FastDateTime.formatUtc(end);
        }
    }

    private FreeBusyCalculator() {}

    /**
     * Extracts and merges all busy intervals from a collection of events within a time window.
     */
    public static List<TimeSlot> computeBusyIntervals(Iterable<VEvent> events, long windowStart, long windowEnd) {
        List<TimeSlot> rawIntervals = new ArrayList<>();

        for (VEvent event : events) {
            if (event.getTransp() != VEvent.Transp.OPAQUE || event.getStatus() == VEvent.Status.CANCELLED) {
                continue;
            }

            if (!event.isRecurring()) {
                if (event.getDtStart() < windowEnd && event.getDtEnd() > windowStart) {
                    rawIntervals.add(new TimeSlot(
                        Math.max(windowStart, event.getDtStart()),
                        Math.min(windowEnd, event.getDtEnd())
                    ));
                }
            } else {
                long duration = event.getDurationMillis();
                long[] occs = event.expandOccurrences(windowStart - duration, windowEnd);
                for (long occStart : occs) {
                    long occEnd = occStart + duration;
                    if (occStart < windowEnd && occEnd > windowStart) {
                        rawIntervals.add(new TimeSlot(
                            Math.max(windowStart, occStart),
                            Math.min(windowEnd, occEnd)
                        ));
                    }
                }
            }
        }

        return mergeIntervals(rawIntervals);
    }

    /**
     * Merges overlapping or adjacent time intervals in O(N log N) time.
     */
    public static List<TimeSlot> mergeIntervals(List<TimeSlot> intervals) {
        if (intervals == null || intervals.isEmpty()) return Collections.emptyList();
        if (intervals.size() == 1) return intervals;

        intervals.sort(Comparator.comparingLong(TimeSlot::getStart));

        List<TimeSlot> merged = new ArrayList<>(intervals.size());
        TimeSlot current = intervals.get(0);
        long curStart = current.getStart();
        long curEnd = current.getEnd();

        for (int i = 1; i < intervals.size(); i++) {
            TimeSlot next = intervals.get(i);
            if (next.getStart() <= curEnd) {
                curEnd = Math.max(curEnd, next.getEnd());
            } else {
                merged.add(new TimeSlot(curStart, curEnd));
                curStart = next.getStart();
                curEnd = next.getEnd();
            }
        }
        merged.add(new TimeSlot(curStart, curEnd));

        return merged;
    }

    /**
     * Inverts merged busy intervals into free time slots within the given window.
     */
    public static List<TimeSlot> computeFreeIntervals(List<TimeSlot> busyIntervals, long windowStart, long windowEnd, long minDurationMillis) {
        List<TimeSlot> freeSlots = new ArrayList<>();
        long current = windowStart;

        for (TimeSlot busy : busyIntervals) {
            if (busy.getStart() > current) {
                long freeDuration = busy.getStart() - current;
                if (freeDuration >= minDurationMillis) {
                    freeSlots.add(new TimeSlot(current, busy.getStart()));
                }
            }
            current = Math.max(current, busy.getEnd());
        }

        if (current < windowEnd) {
            long remaining = windowEnd - current;
            if (remaining >= minDurationMillis) {
                freeSlots.add(new TimeSlot(current, windowEnd));
            }
        }

        return freeSlots;
    }

    /**
     * Finds common available meeting slots across multiple attendee calendars, restricted to working hours.
     */
    public static List<TimeSlot> findCommonMeetingSlots(List<VCalendar> calendars,
                                                        long windowStart,
                                                        long windowEnd,
                                                        long meetingDurationMillis,
                                                        int workStartHour,
                                                        int workEndHour) {
        List<VEvent> allEvents = new ArrayList<>();
        if (calendars != null) {
            for (VCalendar cal : calendars) {
                allEvents.addAll(cal.getEvents());
            }
        }

        // Compute merged busy intervals
        List<TimeSlot> busy = computeBusyIntervals(allEvents, windowStart, windowEnd);
        List<TimeSlot> free = computeFreeIntervals(busy, windowStart, windowEnd, meetingDurationMillis);

        // Filter and chunk by working hours
        List<TimeSlot> result = new ArrayList<>();
        int[] dt = new int[6];

        for (TimeSlot slot : free) {
            long slotStart = slot.getStart();
            long slotEnd = slot.getEnd();

            // Iterate by days in range
            long dayStart = (slotStart / FastDateTime.MILLIS_PER_DAY) * FastDateTime.MILLIS_PER_DAY;
            while (dayStart < slotEnd) {
                FastDateTime.unpackUtc(dayStart, dt);
                int dow = FastDateTime.getDayOfWeek(dt[0], dt[1], dt[2]);

                // Exclude weekends (Saturday = 6, Sunday = 7)
                if (dow <= 5) {
                    long workStartMillis = FastDateTime.toEpochMillis(dt[0], dt[1], dt[2], workStartHour, 0, 0);
                    long workEndMillis = FastDateTime.toEpochMillis(dt[0], dt[1], dt[2], workEndHour, 0, 0);

                    long effectiveStart = Math.max(slotStart, workStartMillis);
                    long effectiveEnd = Math.min(slotEnd, workEndMillis);

                    if (effectiveEnd - effectiveStart >= meetingDurationMillis) {
                        // Create valid meeting slots of requested duration
                        long currSlot = effectiveStart;
                        while (currSlot + meetingDurationMillis <= effectiveEnd) {
                            result.add(new TimeSlot(currSlot, currSlot + meetingDurationMillis));
                            currSlot += meetingDurationMillis;
                        }
                    }
                }
                dayStart += FastDateTime.MILLIS_PER_DAY;
            }
        }

        return result;
    }

    /**
     * Generates a standard RFC 5545 VFREEBUSY component from a calendar's busy intervals.
     */
    public static VFreeBusy generateFreeBusy(VCalendar calendar, long start, long end, Organizer organizer) {
        List<TimeSlot> busy = computeBusyIntervals(calendar.getEvents(), start, end);
        List<VFreeBusy.Period> periods = new ArrayList<>(busy.size());
        for (TimeSlot b : busy) {
            periods.add(new VFreeBusy.Period(b.getStart(), b.getEnd(), "BUSY"));
        }
        return new VFreeBusy(java.util.UUID.randomUUID().toString(), start, end, organizer, Collections.emptyList(), periods);
    }
}
