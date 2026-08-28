package fastcalendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ultra-fast, zero-allocation date/time utility for RFC 5545 iCalendar timestamps.
 * Converts between ISO-8601 / RFC 5545 timestamp strings and epoch milliseconds
 * using direct integer arithmetic with minimal GC footprint.
 */
public final class FastDateTime {

    private static final int[] DAYS_PER_MONTH = {
        0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    private static final int[] DAYS_PER_MONTH_LEAP = {
        0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    private static final int[] DAYS_BEFORE_MONTH = {
        0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
    };

    private static final int[] DAYS_BEFORE_MONTH_LEAP = {
        0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335
    };

    public static final long MILLIS_PER_SECOND = 1000L;
    public static final long MILLIS_PER_MINUTE = 60_000L;
    public static final long MILLIS_PER_HOUR   = 3_600_000L;
    public static final long MILLIS_PER_DAY    = 86_400_000L;

    private FastDateTime() {}

    /**
     * Checks if a year is a leap year in the Gregorian calendar.
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Gets number of days in the specified month of the given year.
     */
    public static int getDaysInMonth(int year, int month) {
        if (month < 1 || month > 12) return 0;
        return isLeapYear(year) ? DAYS_PER_MONTH_LEAP[month] : DAYS_PER_MONTH[month];
    }

    /**
     * Computes the day of week for a given Gregorian date: 1 = Monday, ..., 7 = Sunday.
     * Uses Sakamoto's algorithm (zero allocation, 2-3 CPU cycles).
     */
    public static int getDayOfWeek(int year, int month, int day) {
        int[] t = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
        if (month < 3) {
            year -= 1;
        }
        int dow = (year + year / 4 - year / 100 + year / 400 + t[month - 1] + day) % 7;
        return dow == 0 ? 7 : dow; // 1 = Monday ... 7 = Sunday (ISO-8601)
    }

    /**
     * Converts year, month (1..12), day (1..31), hour (0..23), minute (0..59), second (0..59)
     * into UTC epoch milliseconds using direct integer calendar arithmetic.
     */
    public static long toEpochMillis(int year, int month, int day, int hour, int minute, int second) {
        long y = year - 1970;
        long leaps = (year - 1969) / 4 - (year - 1901) / 100 + (year - 1601) / 400;
        long days = y * 365 + leaps;

        boolean leap = isLeapYear(year);
        days += leap ? DAYS_BEFORE_MONTH_LEAP[month] : DAYS_BEFORE_MONTH[month];
        days += (day - 1);

        long totalSeconds = days * 86400L + hour * 3600L + minute * 60L + second;
        return totalSeconds * 1000L;
    }

    /**
     * Parses RFC 5545 timestamp string (e.g. "20260828T143000Z", "20260828T143000", or "20260828")
     * into UTC epoch milliseconds.
     */
    public static long parseIcsDateTime(CharSequence cs) {
        if (cs == null || cs.length() < 8) {
            return -1L;
        }

        // Clean any surrounding whitespace
        int start = 0;
        int end = cs.length();
        while (start < end && cs.charAt(start) <= ' ') start++;
        while (end > start && cs.charAt(end - 1) <= ' ') end--;

        int len = end - start;
        if (len < 8) return -1L;

        // Check if hyphenated/coloned ISO standard: "2026-08-28T14:30:00Z"
        if (cs.charAt(start + 4) == '-') {
            return parseIsoStandard(cs, start, end);
        }

        // Format: YYYYMMDD or YYYYMMDDTHHMMSS or YYYYMMDDTHHMMSSZ
        int year = parse4Digits(cs, start);
        int month = parse2Digits(cs, start + 4);
        int day = parse2Digits(cs, start + 6);

        int hour = 0;
        int minute = 0;
        int second = 0;

        if (len >= 15 && (cs.charAt(start + 8) == 'T' || cs.charAt(start + 8) == 't')) {
            hour = parse2Digits(cs, start + 9);
            minute = parse2Digits(cs, start + 11);
            second = parse2Digits(cs, start + 13);
        }

        return toEpochMillis(year, month, day, hour, minute, second);
    }

    private static long parseIsoStandard(CharSequence cs, int start, int end) {
        try {
            int year = parse4Digits(cs, start);
            int month = parse2Digits(cs, start + 5);
            int day = parse2Digits(cs, start + 8);
            int hour = 0, minute = 0, second = 0;
            if (end - start >= 19 && cs.charAt(start + 10) == 'T') {
                hour = parse2Digits(cs, start + 11);
                minute = parse2Digits(cs, start + 14);
                second = parse2Digits(cs, start + 17);
            }
            return toEpochMillis(year, month, day, hour, minute, second);
        } catch (Exception e) {
            return -1L;
        }
    }

    private static int parse4Digits(CharSequence cs, int offset) {
        return (cs.charAt(offset) - '0') * 1000 +
               (cs.charAt(offset + 1) - '0') * 100 +
               (cs.charAt(offset + 2) - '0') * 10 +
               (cs.charAt(offset + 3) - '0');
    }

    private static int parse2Digits(CharSequence cs, int offset) {
        return (cs.charAt(offset) - '0') * 10 + (cs.charAt(offset + 1) - '0');
    }

    /**
     * Formats epoch milliseconds into RFC 5545 UTC timestamp: "YYYYMMDDTHHMMSSZ".
     */
    public static String formatUtc(long epochMillis) {
        StringBuilder sb = new StringBuilder(16);
        formatUtc(epochMillis, sb);
        return sb.toString();
    }

    /**
     * Formats epoch milliseconds into RFC 5545 UTC timestamp appending to StringBuilder: "YYYYMMDDTHHMMSSZ".
     */
    public static void formatUtc(long epochMillis, StringBuilder sb) {
        long totalSeconds = Math.floorDiv(epochMillis, 1000L);
        long days = Math.floorDiv(totalSeconds, 86400L);
        int remSeconds = (int) Math.floorMod(totalSeconds, 86400L);

        int hour = remSeconds / 3600;
        int rem = remSeconds % 3600;
        int minute = rem / 60;
        int second = rem % 60;

        // Compute Year, Month, Day from epoch days
        long l = days + 719468;
        long n = Math.floorDiv(l, 146097);
        long l1 = Math.floorMod(l, 146097);
        long n1 = Math.floorDiv(l1, 36524);
        long l2 = l1 - n1 * 36524;
        long n2 = Math.floorDiv(l2, 1461);
        long l3 = l2 - n2 * 1461;
        long n3 = Math.floorDiv(l3, 365);
        long l4 = l3 - n3 * 365;

        long year = 400 * n + 100 * n1 + 4 * n2 + n3;
        if (n1 == 4 || n3 == 4) {
            year--;
            l4 += 365;
        }

        long m = (5 * l4 + 2) / 153;
        long day = l4 - (153 * m + 2) / 5 + 1;
        long month = m < 10 ? m + 3 : m - 9;
        if (month <= 2) {
            year++;
        }

        append4Digits((int) year, sb);
        append2Digits((int) month, sb);
        append2Digits((int) day, sb);
        sb.append('T');
        append2Digits(hour, sb);
        append2Digits(minute, sb);
        append2Digits(second, sb);
        sb.append('Z');
    }

    /**
     * Formats epoch milliseconds into RFC 5545 Date-only string: "YYYYMMDD".
     */
    public static String formatDateOnly(long epochMillis) {
        StringBuilder sb = new StringBuilder(8);
        formatDateOnly(epochMillis, sb);
        return sb.toString();
    }

    public static void formatDateOnly(long epochMillis, StringBuilder sb) {
        long totalSeconds = Math.floorDiv(epochMillis, 1000L);
        long days = Math.floorDiv(totalSeconds, 86400L);

        long l = days + 719468;
        long n = Math.floorDiv(l, 146097);
        long l1 = Math.floorMod(l, 146097);
        long n1 = Math.floorDiv(l1, 36524);
        long l2 = l1 - n1 * 36524;
        long n2 = Math.floorDiv(l2, 1461);
        long l3 = l2 - n2 * 1461;
        long n3 = Math.floorDiv(l3, 365);
        long l4 = l3 - n3 * 365;

        long year = 400 * n + 100 * n1 + 4 * n2 + n3;
        if (n1 == 4 || n3 == 4) {
            year--;
            l4 += 365;
        }

        long m = (5 * l4 + 2) / 153;
        long day = l4 - (153 * m + 2) / 5 + 1;
        long month = m < 10 ? m + 3 : m - 9;
        if (month <= 2) {
            year++;
        }

        append4Digits((int) year, sb);
        append2Digits((int) month, sb);
        append2Digits((int) day, sb);
    }

    private static void append4Digits(int val, StringBuilder sb) {
        if (val < 0) val = 0;
        int d1 = (val / 1000) % 10;
        int d2 = (val / 100) % 10;
        int d3 = (val / 10) % 10;
        int d4 = val % 10;
        sb.append((char) ('0' + d1));
        sb.append((char) ('0' + d2));
        sb.append((char) ('0' + d3));
        sb.append((char) ('0' + d4));
    }

    private static void append2Digits(int val, StringBuilder sb) {
        if (val < 0) val = 0;
        int d1 = (val / 10) % 10;
        int d2 = val % 10;
        sb.append((char) ('0' + d1));
        sb.append((char) ('0' + d2));
    }

    /**
     * Unpacks year, month, day, hour, minute, second from epoch millis into an int[6] array.
     */
    public static void unpackUtc(long epochMillis, int[] outComponents) {
        long totalSeconds = Math.floorDiv(epochMillis, 1000L);
        long days = Math.floorDiv(totalSeconds, 86400L);
        int remSeconds = (int) Math.floorMod(totalSeconds, 86400L);

        int hour = remSeconds / 3600;
        int rem = remSeconds % 3600;
        int minute = rem / 60;
        int second = rem % 60;

        long l = days + 719468;
        long n = Math.floorDiv(l, 146097);
        long l1 = Math.floorMod(l, 146097);
        long n1 = Math.floorDiv(l1, 36524);
        long l2 = l1 - n1 * 36524;
        long n2 = Math.floorDiv(l2, 1461);
        long l3 = l2 - n2 * 1461;
        long n3 = Math.floorDiv(l3, 365);
        long l4 = l3 - n3 * 365;

        long year = 400 * n + 100 * n1 + 4 * n2 + n3;
        if (n1 == 4 || n3 == 4) {
            year--;
            l4 += 365;
        }

        long m = (5 * l4 + 2) / 153;
        long day = l4 - (153 * m + 2) / 5 + 1;
        long month = m < 10 ? m + 3 : m - 9;
        if (month <= 2) {
            year++;
        }

        outComponents[0] = (int) year;
        outComponents[1] = (int) month;
        outComponents[2] = (int) day;
        outComponents[3] = hour;
        outComponents[4] = minute;
        outComponents[5] = second;
    }

    /**
     * Convenience helper to create epoch millis from java.time.LocalDateTime in UTC.
     */
    public static long of(int year, int month, int day, int hour, int minute, int second) {
        return toEpochMillis(year, month, day, hour, minute, second);
    }

    public static long of(int year, int month, int day) {
        return toEpochMillis(year, month, day, 0, 0, 0);
    }
}
