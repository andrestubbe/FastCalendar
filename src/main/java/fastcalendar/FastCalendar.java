package fastcalendar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * FastCalendar - High-Performance, Zero-Allocation iCalendar (RFC 5545),
 * CalDAV (RFC 4791), and RRULE Recurrence Engine for the FastJava Ecosystem.
 */
public final class FastCalendar {

    private FastCalendar() {}

    /**
     * Parses an RFC 5545 iCalendar from a String.
     */
    public static VCalendar parse(String icsContent) {
        return IcsParser.parse(icsContent);
    }

    /**
     * Parses an RFC 5545 iCalendar from a byte array.
     */
    public static VCalendar parse(byte[] icsBytes) {
        return IcsParser.parse(icsBytes);
    }

    /**
     * Parses an RFC 5545 iCalendar from a file Path.
     */
    public static VCalendar parse(Path path) throws IOException {
        return IcsParser.parse(path);
    }

    /**
     * Parses an RFC 5545 iCalendar from a File.
     */
    public static VCalendar parse(File file) throws IOException {
        return IcsParser.parse(file);
    }

    /**
     * Parses an RFC 5545 iCalendar from an InputStream.
     */
    public static VCalendar parse(InputStream is) {
        return IcsParser.parse(is);
    }

    /**
     * Serializes a VCalendar object to an RFC 5545 formatted .ics String.
     */
    public static String write(VCalendar calendar) {
        return IcsWriter.writeToString(calendar);
    }

    /**
     * Serializes a VCalendar object to an RFC 5545 formatted file.
     */
    public static void write(VCalendar calendar, Path path) throws IOException {
        IcsWriter.writeToFile(calendar, path);
    }

    /**
     * Creates a new fluent VCalendar builder.
     */
    public static VCalendar.Builder calendar() {
        return VCalendar.builder();
    }

    /**
     * Creates a named calendar builder.
     */
    public static VCalendar.Builder calendar(String name) {
        return VCalendar.builder().name(name);
    }

    /**
     * Creates a new fluent VEvent builder.
     */
    public static VEvent.Builder event() {
        return VEvent.builder();
    }

    /**
     * Creates an event with summary and start/end time.
     */
    public static VEvent.Builder event(String summary, long startEpochMillis, long endEpochMillis) {
        return VEvent.builder()
            .summary(summary)
            .dtStart(startEpochMillis)
            .dtEnd(endEpochMillis);
    }

    /**
     * Parses an RFC 5545 RRULE string.
     */
    public static RRule rrule(String rruleText) {
        return RRule.parse(rruleText);
    }

    /**
     * Creates a new RRULE builder.
     */
    public static RRule.Builder rruleBuilder() {
        return RRule.builder();
    }

    /**
     * Creates a new empty CalendarIndex.
     */
    public static CalendarIndex index() {
        return new CalendarIndex();
    }

    /**
     * Creates a CalendarIndex populated with events.
     */
    public static CalendarIndex index(Collection<VEvent> events) {
        CalendarIndex idx = new CalendarIndex();
        idx.addAll(events);
        return idx;
    }

    /**
     * Expands an event's recurrence rule into a sorted array of epoch millisecond timestamps.
     */
    public static long[] expandRecurrence(long dtStart, RRule rule, long windowStart, long windowEnd) {
        return RRuleEvaluator.expand(dtStart, rule, windowStart, windowEnd);
    }

    /**
     * Finds common available meeting slots across multiple calendars within working hours.
     */
    public static List<FreeBusyCalculator.TimeSlot> findCommonSlots(List<VCalendar> calendars,
                                                                   long windowStart,
                                                                   long windowEnd,
                                                                   long meetingDurationMillis,
                                                                   int workStartHour,
                                                                   int workEndHour) {
        return FreeBusyCalculator.findCommonMeetingSlots(calendars, windowStart, windowEnd,
                                                         meetingDurationMillis, workStartHour, workEndHour);
    }

    /**
     * Fast epoch millisecond converter.
     */
    public static long epochMillis(int year, int month, int day, int hour, int minute, int second) {
        return FastDateTime.toEpochMillis(year, month, day, hour, minute, second);
    }

    /**
     * Fast epoch millisecond converter for date at UTC midnight.
     */
    public static long epochMillis(int year, int month, int day) {
        return FastDateTime.toEpochMillis(year, month, day, 0, 0, 0);
    }

    /**
     * Formats epoch millisecond to RFC 5545 UTC timestamp.
     */
    public static String formatUtc(long epochMillis) {
        return FastDateTime.formatUtc(epochMillis);
    }

    /**
     * Formats epoch millisecond to RFC 5545 Date string.
     */
    public static String formatDate(long epochMillis) {
        return FastDateTime.formatDateOnly(epochMillis);
    }

    /**
     * Parses RFC 5545 date or datetime timestamp to epoch milliseconds.
     */
    public static long parseDateTime(String timestamp) {
        return FastDateTime.parseIcsDateTime(timestamp);
    }

    /**
     * Creates a new CalDavClient with Basic authentication.
     */
    public static CalDavClient calDavClient(String username, String password) {
        return CalDavClient.builder().basicAuth(username, password).build();
    }
}
