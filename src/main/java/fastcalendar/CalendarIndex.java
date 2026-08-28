package fastcalendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Ultra-fast temporal event index and interval scheduler.
 * Supports point-in-time and range queries across millions of recurring and non-recurring events.
 */
public final class CalendarIndex {

    private final List<VEvent> nonRecurring = new ArrayList<>();
    private final List<VEvent> recurring = new ArrayList<>();

    public CalendarIndex() {}

    public void add(VEvent event) {
        if (event == null) return;
        if (event.isRecurring()) {
            recurring.add(event);
        } else {
            nonRecurring.add(event);
        }
    }

    public void addAll(Collection<VEvent> events) {
        if (events == null) return;
        for (VEvent e : events) {
            add(e);
        }
    }

    public int size() {
        return nonRecurring.size() + recurring.size();
    }

    public void clear() {
        nonRecurring.clear();
        recurring.clear();
    }

    /**
     * Checks if any confirmed, opaque event conflicts with [rangeStart, rangeEnd].
     */
    public boolean hasConflict(long rangeStart, long rangeEnd) {
        for (int i = 0; i < nonRecurring.size(); i++) {
            VEvent e = nonRecurring.get(i);
            if (e.getTransp() == VEvent.Transp.OPAQUE && e.getStatus() != VEvent.Status.CANCELLED) {
                if (e.getDtStart() < rangeEnd && e.getDtEnd() > rangeStart) {
                    return true;
                }
            }
        }
        for (int i = 0; i < recurring.size(); i++) {
            VEvent e = recurring.get(i);
            if (e.overlaps(rangeStart, rangeEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves all events that have any occurrence overlapping [rangeStart, rangeEnd].
     */
    public List<VEvent> getEventsBetween(long rangeStart, long rangeEnd) {
        List<VEvent> results = new ArrayList<>();

        for (int i = 0; i < nonRecurring.size(); i++) {
            VEvent e = nonRecurring.get(i);
            if (e.getDtStart() < rangeEnd && e.getDtEnd() > rangeStart) {
                results.add(e);
            }
        }

        for (int i = 0; i < recurring.size(); i++) {
            VEvent e = recurring.get(i);
            if (e.overlaps(rangeStart, rangeEnd)) {
                results.add(e);
            }
        }

        return results;
    }

    /**
     * Retrieves all events active at a specific instant.
     */
    public List<VEvent> getEventsAt(long timestamp) {
        return getEventsBetween(timestamp, timestamp + 1);
    }

    /**
     * Finds conflicting events for a proposed time window [rangeStart, rangeEnd].
     */
    public List<VEvent> findConflicts(long rangeStart, long rangeEnd) {
        List<VEvent> conflicts = new ArrayList<>();
        for (int i = 0; i < nonRecurring.size(); i++) {
            VEvent e = nonRecurring.get(i);
            if (e.getTransp() == VEvent.Transp.OPAQUE && e.getStatus() != VEvent.Status.CANCELLED) {
                if (e.getDtStart() < rangeEnd && e.getDtEnd() > rangeStart) {
                    conflicts.add(e);
                }
            }
        }
        for (int i = 0; i < recurring.size(); i++) {
            VEvent e = recurring.get(i);
            if (e.overlaps(rangeStart, rangeEnd)) {
                conflicts.add(e);
            }
        }
        return conflicts;
    }

    /**
     * Searches for the next available slot of duration slotDurationMillis between searchFrom and searchUntil.
     * Returns start epoch millis of the available slot, or -1 if no slot available.
     */
    public long nextAvailableSlot(long searchFrom, long slotDurationMillis, long searchUntil, long stepMillis) {
        long current = searchFrom;
        while (current + slotDurationMillis <= searchUntil) {
            long slotEnd = current + slotDurationMillis;
            if (!hasConflict(current, slotEnd)) {
                return current;
            }
            current += Math.max(1, stepMillis);
        }
        return -1L;
    }
}
