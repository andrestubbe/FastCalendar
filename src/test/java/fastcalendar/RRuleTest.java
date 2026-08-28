package fastcalendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RRuleTest {

    @Test
    public void testParseDailyRule() {
        RRule rule = RRule.parse("FREQ=DAILY;INTERVAL=2;COUNT=5");
        assertNotNull(rule);
        assertEquals(RRule.Frequency.DAILY, rule.getFrequency());
        assertEquals(2, rule.getInterval());
        assertEquals(5, rule.getCount());
        assertEquals("FREQ=DAILY;INTERVAL=2;COUNT=5", rule.toIcsString());
    }

    @Test
    public void testParseWeeklyRule() {
        RRule rule = RRule.parse("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR");
        assertNotNull(rule);
        assertEquals(RRule.Frequency.WEEKLY, rule.getFrequency());
        assertEquals(1, rule.getInterval());
        assertEquals(3, rule.getByDays().length);
        assertEquals(RRule.WeekDay.MO, rule.getByDays()[0].getDay());
        assertEquals(RRule.WeekDay.WE, rule.getByDays()[1].getDay());
        assertEquals(RRule.WeekDay.FR, rule.getByDays()[2].getDay());
    }

    @Test
    public void testParseMonthlyWithOrdinal() {
        RRule rule = RRule.parse("FREQ=MONTHLY;BYDAY=2TU");
        assertNotNull(rule);
        assertEquals(RRule.Frequency.MONTHLY, rule.getFrequency());
        assertEquals(1, rule.getByDays().length);
        assertEquals(2, rule.getByDays()[0].getOrdinal());
        assertEquals(RRule.WeekDay.TU, rule.getByDays()[0].getDay());
    }

    @Test
    public void testParseNegativeOrdinal() {
        RRule rule = RRule.parse("FREQ=MONTHLY;BYDAY=-1FR");
        assertNotNull(rule);
        assertEquals(-1, rule.getByDays()[0].getOrdinal());
        assertEquals(RRule.WeekDay.FR, rule.getByDays()[0].getDay());
    }
}
