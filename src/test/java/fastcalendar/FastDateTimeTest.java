package fastcalendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FastDateTimeTest {

    @Test
    public void testLeapYear() {
        assertTrue(FastDateTime.isLeapYear(2024));
        assertTrue(FastDateTime.isLeapYear(2000));
        assertFalse(FastDateTime.isLeapYear(2025));
        assertFalse(FastDateTime.isLeapYear(2026));
        assertFalse(FastDateTime.isLeapYear(1900));
        assertTrue(FastDateTime.isLeapYear(2028));
    }

    @Test
    public void testDaysInMonth() {
        assertEquals(31, FastDateTime.getDaysInMonth(2026, 1));
        assertEquals(28, FastDateTime.getDaysInMonth(2026, 2));
        assertEquals(29, FastDateTime.getDaysInMonth(2024, 2));
        assertEquals(31, FastDateTime.getDaysInMonth(2026, 8));
        assertEquals(30, FastDateTime.getDaysInMonth(2026, 9));
    }

    @Test
    public void testDayOfWeek() {
        // 2026-08-28 is Friday (5)
        assertEquals(5, FastDateTime.getDayOfWeek(2026, 8, 28));
        // 2026-08-29 is Saturday (6)
        assertEquals(6, FastDateTime.getDayOfWeek(2026, 8, 29));
        // 2026-08-30 is Sunday (7)
        assertEquals(7, FastDateTime.getDayOfWeek(2026, 8, 30));
        // 2026-08-31 is Monday (1)
        assertEquals(1, FastDateTime.getDayOfWeek(2026, 8, 31));
    }

    @Test
    public void testParseAndFormatUtc() {
        String icsUtc = "20260828T143000Z";
        long millis = FastDateTime.parseIcsDateTime(icsUtc);
        assertTrue(millis > 0);

        String formatted = FastDateTime.formatUtc(millis);
        assertEquals(icsUtc, formatted);
    }

    @Test
    public void testDateOnly() {
        String icsDate = "20260828";
        long millis = FastDateTime.parseIcsDateTime(icsDate);
        assertTrue(millis > 0);

        String formatted = FastDateTime.formatDateOnly(millis);
        assertEquals(icsDate, formatted);
    }

    @Test
    public void testUnpackUtc() {
        long millis = FastDateTime.toEpochMillis(2026, 8, 28, 16, 45, 30);
        int[] out = new int[6];
        FastDateTime.unpackUtc(millis, out);
        assertEquals(2026, out[0]);
        assertEquals(8, out[1]);
        assertEquals(28, out[2]);
        assertEquals(16, out[3]);
        assertEquals(45, out[4]);
        assertEquals(30, out[5]);
    }
}
