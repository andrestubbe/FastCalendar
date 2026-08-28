package fastcalendar;

import java.util.Arrays;
import java.util.Objects;

/**
 * RFC 5545 Recurrence Rule (RRULE) data structure and builder.
 * Supports FREQ, INTERVAL, COUNT, UNTIL, BYSECOND, BYMINUTE, BYHOUR, BYDAY,
 * BYMONTHDAY, BYYEARDAY, BYWEEKNO, BYMONTH, BYSETPOS, and WKST.
 */
public final class RRule {

    public enum Frequency {
        SECONDLY,
        MINUTELY,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }

    public enum WeekDay {
        MO(1, "MO"),
        TU(2, "TU"),
        WE(3, "WE"),
        TH(4, "TH"),
        FR(5, "FR"),
        SA(6, "SA"),
        SU(7, "SU");

        private final int isoDay;
        private final String code;

        WeekDay(int isoDay, String code) {
            this.isoDay = isoDay;
            this.code = code;
        }

        public int getIsoDay() {
            return isoDay;
        }

        public String getCode() {
            return code;
        }

        public static WeekDay fromIsoDay(int isoDay) {
            switch (isoDay) {
                case 1: return MO;
                case 2: return TU;
                case 3: return WE;
                case 4: return TH;
                case 5: return FR;
                case 6: return SA;
                case 7: return SU;
                default: return MO;
            }
        }

        public static WeekDay fromCode(String code) {
            if (code == null) return MO;
            String upper = code.trim().toUpperCase();
            switch (upper) {
                case "MO": return MO;
                case "TU": return TU;
                case "WE": return WE;
                case "TH": return TH;
                case "FR": return FR;
                case "SA": return SA;
                case "SU": return SU;
                default: return MO;
            }
        }
    }

    /**
     * Day rule with optional ordinal position (e.g. +1MO for 1st Monday, -1SU for last Sunday, 0 for every Monday).
     */
    public static final class ByDayRule {
        private final int ordinal; // 0 for every, +1, +2, ..., -1, -2
        private final WeekDay day;

        public ByDayRule(int ordinal, WeekDay day) {
            this.ordinal = ordinal;
            this.day = Objects.requireNonNull(day, "day");
        }

        public ByDayRule(WeekDay day) {
            this(0, day);
        }

        public int getOrdinal() {
            return ordinal;
        }

        public WeekDay getDay() {
            return day;
        }

        public static ByDayRule parse(String token) {
            token = token.trim();
            if (token.isEmpty()) return null;
            int len = token.length();
            if (len >= 2) {
                String dayCode = token.substring(len - 2).toUpperCase();
                WeekDay wd = WeekDay.fromCode(dayCode);
                int ord = 0;
                if (len > 2) {
                    ord = Integer.parseInt(token.substring(0, len - 2));
                }
                return new ByDayRule(ord, wd);
            }
            return null;
        }

        @Override
        public String toString() {
            if (ordinal == 0) return day.getCode();
            return (ordinal > 0 ? "+" + ordinal : String.valueOf(ordinal)) + day.getCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByDayRule that)) return false;
            return ordinal == that.ordinal && day == that.day;
        }

        @Override
        public int hashCode() {
            return 31 * ordinal + day.hashCode();
        }
    }

    private final Frequency freq;
    private final int interval;
    private final int count;
    private final long until; // epoch millis or -1 if none
    private final int[] bySeconds;
    private final int[] byMinutes;
    private final int[] byHours;
    private final ByDayRule[] byDays;
    private final int[] byMonthDays;
    private final int[] byYearDays;
    private final int[] byWeeks;
    private final int[] byMonths;
    private final int[] bySetPositions;
    private final WeekDay wkst;

    public RRule(Frequency freq, int interval, int count, long until,
                 int[] bySeconds, int[] byMinutes, int[] byHours, ByDayRule[] byDays,
                 int[] byMonthDays, int[] byYearDays, int[] byWeeks, int[] byMonths,
                 int[] bySetPositions, WeekDay wkst) {
        this.freq = freq != null ? freq : Frequency.DAILY;
        this.interval = Math.max(1, interval);
        this.count = count;
        this.until = until;
        this.bySeconds = bySeconds;
        this.byMinutes = byMinutes;
        this.byHours = byHours;
        this.byDays = byDays;
        this.byMonthDays = byMonthDays;
        this.byYearDays = byYearDays;
        this.byWeeks = byWeeks;
        this.byMonths = byMonths;
        this.bySetPositions = bySetPositions;
        this.wkst = wkst != null ? wkst : WeekDay.MO;
    }

    public Frequency getFrequency() { return freq; }
    public int getInterval() { return interval; }
    public int getCount() { return count; }
    public long getUntil() { return until; }
    public int[] getBySeconds() { return bySeconds; }
    public int[] getByMinutes() { return byMinutes; }
    public int[] getByHours() { return byHours; }
    public ByDayRule[] getByDays() { return byDays; }
    public int[] getByMonthDays() { return byMonthDays; }
    public int[] getByYearDays() { return byYearDays; }
    public int[] getByWeeks() { return byWeeks; }
    public int[] getByMonths() { return byMonths; }
    public int[] getBySetPositions() { return bySetPositions; }
    public WeekDay getWkst() { return wkst; }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder daily() {
        return new Builder().frequency(Frequency.DAILY);
    }

    public static Builder weekly() {
        return new Builder().frequency(Frequency.WEEKLY);
    }

    public static Builder monthly() {
        return new Builder().frequency(Frequency.MONTHLY);
    }

    public static Builder yearly() {
        return new Builder().frequency(Frequency.YEARLY);
    }

    /**
     * Parses RFC 5545 RRULE string (e.g. "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;COUNT=10").
     */
    public static RRule parse(CharSequence rruleStr) {
        if (rruleStr == null) return null;
        String s = rruleStr.toString().trim();
        if (s.startsWith("RRULE:")) {
            s = s.substring(6).trim();
        }

        Builder b = new Builder();
        String[] parts = s.split(";");
        for (String part : parts) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = part.substring(0, eq).trim().toUpperCase();
            String val = part.substring(eq + 1).trim();

            switch (key) {
                case "FREQ":
                    b.frequency(Frequency.valueOf(val.toUpperCase()));
                    break;
                case "INTERVAL":
                    b.interval(Integer.parseInt(val));
                    break;
                case "COUNT":
                    b.count(Integer.parseInt(val));
                    break;
                case "UNTIL":
                    b.until(FastDateTime.parseIcsDateTime(val));
                    break;
                case "BYSECOND":
                    b.bySeconds(parseIntArray(val));
                    break;
                case "BYMINUTE":
                    b.byMinutes(parseIntArray(val));
                    break;
                case "BYHOUR":
                    b.byHours(parseIntArray(val));
                    break;
                case "BYDAY":
                    b.byDays(parseByDayRules(val));
                    break;
                case "BYMONTHDAY":
                    b.byMonthDays(parseIntArray(val));
                    break;
                case "BYYEARDAY":
                    b.byYearDays(parseIntArray(val));
                    break;
                case "BYWEEKNO":
                    b.byWeeks(parseIntArray(val));
                    break;
                case "BYMONTH":
                    b.byMonths(parseIntArray(val));
                    break;
                case "BYSETPOS":
                    b.bySetPositions(parseIntArray(val));
                    break;
                case "WKST":
                    b.wkst(WeekDay.fromCode(val));
                    break;
            }
        }
        return b.build();
    }

    private static int[] parseIntArray(String str) {
        String[] tokens = str.split(",");
        int[] arr = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            arr[i] = Integer.parseInt(tokens[i].trim());
        }
        return arr;
    }

    private static ByDayRule[] parseByDayRules(String str) {
        String[] tokens = str.split(",");
        ByDayRule[] rules = new ByDayRule[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            rules[i] = ByDayRule.parse(tokens[i]);
        }
        return rules;
    }

    /**
     * Formats to RFC 5545 RRULE string format.
     */
    public String toIcsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FREQ=").append(freq.name());
        if (interval > 1) {
            sb.append(";INTERVAL=").append(interval);
        }
        if (count > 0) {
            sb.append(";COUNT=").append(count);
        }
        if (until > 0) {
            sb.append(";UNTIL=").append(FastDateTime.formatUtc(until));
        }
        if (bySeconds != null && bySeconds.length > 0) {
            sb.append(";BYSECOND=").append(joinInts(bySeconds));
        }
        if (byMinutes != null && byMinutes.length > 0) {
            sb.append(";BYMINUTE=").append(joinInts(byMinutes));
        }
        if (byHours != null && byHours.length > 0) {
            sb.append(";BYHOUR=").append(joinInts(byHours));
        }
        if (byDays != null && byDays.length > 0) {
            sb.append(";BYDAY=");
            for (int i = 0; i < byDays.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(byDays[i].toString());
            }
        }
        if (byMonthDays != null && byMonthDays.length > 0) {
            sb.append(";BYMONTHDAY=").append(joinInts(byMonthDays));
        }
        if (byYearDays != null && byYearDays.length > 0) {
            sb.append(";BYYEARDAY=").append(joinInts(byYearDays));
        }
        if (byWeeks != null && byWeeks.length > 0) {
            sb.append(";BYWEEKNO=").append(joinInts(byWeeks));
        }
        if (byMonths != null && byMonths.length > 0) {
            sb.append(";BYMONTH=").append(joinInts(byMonths));
        }
        if (bySetPositions != null && bySetPositions.length > 0) {
            sb.append(";BYSETPOS=").append(joinInts(bySetPositions));
        }
        if (wkst != WeekDay.MO) {
            sb.append(";WKST=").append(wkst.getCode());
        }
        return sb.toString();
    }

    private static String joinInts(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toIcsString();
    }

    public static final class Builder {
        private Frequency freq = Frequency.DAILY;
        private int interval = 1;
        private int count = -1;
        private long until = -1L;
        private int[] bySeconds;
        private int[] byMinutes;
        private int[] byHours;
        private ByDayRule[] byDays;
        private int[] byMonthDays;
        private int[] byYearDays;
        private int[] byWeeks;
        private int[] byMonths;
        private int[] bySetPositions;
        private WeekDay wkst = WeekDay.MO;

        public Builder frequency(Frequency freq) { this.freq = freq; return this; }
        public Builder interval(int interval) { this.interval = interval; return this; }
        public Builder count(int count) { this.count = count; return this; }
        public Builder until(long untilEpochMillis) { this.until = untilEpochMillis; return this; }
        public Builder bySeconds(int... seconds) { this.bySeconds = seconds; return this; }
        public Builder byMinutes(int... minutes) { this.byMinutes = minutes; return this; }
        public Builder byHours(int... hours) { this.byHours = hours; return this; }
        public Builder byDays(ByDayRule... days) { this.byDays = days; return this; }
        public Builder byDays(WeekDay... days) {
            ByDayRule[] rules = new ByDayRule[days.length];
            for (int i = 0; i < days.length; i++) {
                rules[i] = new ByDayRule(days[i]);
            }
            this.byDays = rules;
            return this;
        }
        public Builder byMonthDays(int... monthDays) { this.byMonthDays = monthDays; return this; }
        public Builder byYearDays(int... yearDays) { this.byYearDays = yearDays; return this; }
        public Builder byWeeks(int... weeks) { this.byWeeks = weeks; return this; }
        public Builder byMonths(int... months) { this.byMonths = months; return this; }
        public Builder bySetPositions(int... setPositions) { this.bySetPositions = setPositions; return this; }
        public Builder wkst(WeekDay wkst) { this.wkst = wkst; return this; }

        public RRule build() {
            return new RRule(freq, interval, count, until, bySeconds, byMinutes, byHours,
                             byDays, byMonthDays, byYearDays, byWeeks, byMonths, bySetPositions, wkst);
        }
    }
}
