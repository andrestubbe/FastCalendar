package fastcalendar;

/**
 * Functional callback interface for zero-allocation stream iteration over recurrence occurrences.
 */
@FunctionalInterface
public interface RecurrenceConsumer {
    /**
     * Called for each generated occurrence timestamp in epoch milliseconds.
     *
     * @param occurrenceStartMillis Start timestamp in UTC epoch milliseconds.
     * @return true to continue receiving occurrences, false to abort iteration early.
     */
    boolean onOccurrence(long occurrenceStartMillis);
}
