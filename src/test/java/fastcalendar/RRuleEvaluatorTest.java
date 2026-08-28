package fastcalendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RRuleEvaluatorTest {

    @Test
    public void testDailyExpansion() {
        long dtStart = FastDateTime.toEpochMillis(2026, 8, 1, 9, 0, 0);
        RRule rule = RRule.daily().interval(1).count(5).build();

        long windowStart = FastDateTime.toEpochMillis(2026, 8, 1, 0, 0, 0);
        long windowEnd = FastDateTime.toEpochMillis(2026, 8, 10, 0, 0, 0);

        long[] occs = RRuleEvaluator.expand(dtStart, rule, windowStart, windowEnd);
        assertEquals(5, occs.length);
        assertEquals("20260801T090000Z", FastDateTime.formatUtc(occs[0]));
        assertEquals("20260802T090000Z", FastDateTime.formatUtc(occs[1]));
        assertEquals("20260803T090000Z", FastDateTime.formatUtc(occs[2]));
        assertEquals("20260804T090000Z", FastDateTime.formatUtc(occs[3]));
        assertEquals("20260805T090000Z", FastDateTime.formatUtc(occs[4]));
    }

    @Test
    public void testWeeklyExpansionWithExDate() {
        long dtStart = FastDateTime.toEpochMillis(2026, 8, 3, 10, 0, 0); // Monday Aug 3, 2026
        RRule rule = RRule.weekly().byDays(RRule.WeekDay.MO, RRule.WeekDay.WE).count(4).build();

        long exDate = FastDateTime.toEpochMillis(2026, 8, 5, 10, 0, 0); // Exclude Wednesday Aug 5
        long windowStart = FastDateTime.toEpochMillis(2026, 8, 1, 0, 0, 0);
        long windowEnd = FastDateTime.toEpochMillis(2026, 8, 20, 0, 0, 0);

        long[] occs = RRuleEvaluator.expand(dtStart, rule, new long[]{exDate}, null, windowStart, windowEnd, 100);
        assertEquals(3, occs.length);
        assertEquals("20260803T100000Z", FastDateTime.formatUtc(occs[0])); // Mon Aug 3
        assertEquals("20260810T100000Z", FastDateTime.formatUtc(occs[1])); // Mon Aug 10
        assertEquals("20260812T100000Z", FastDateTime.formatUtc(occs[2])); // Wed Aug 12
    }

    @Test
    public void testMonthlySecondTuesday() {
        long dtStart = FastDateTime.toEpochMillis(2026, 1, 1, 14, 0, 0);
        RRule rule = RRule.monthly().byDays(new RRule.ByDayRule(2, RRule.WeekDay.TU)).count(3).build();

        long windowStart = FastDateTime.toEpochMillis(2026, 1, 1, 0, 0, 0);
        long windowEnd = FastDateTime.toEpochMillis(2026, 4, 1, 0, 0, 0);

        long[] occs = RRuleEvaluator.expand(dtStart, rule, windowStart, windowEnd);
        assertEquals(3, occs.length);
        assertEquals("20260113T140000Z", FastDateTime.formatUtc(occs[0])); // 2nd Tue Jan 2026
        assertEquals("20260210T140000Z", FastDateTime.formatUtc(occs[1])); // 2nd Tue Feb 2026
        assertEquals("20260310T140000Z", FastDateTime.formatUtc(occs[2])); // 2nd Tue Mar 2026
    }
}
