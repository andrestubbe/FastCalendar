package fastcalendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IcsParserTest {

    @Test
    public void testParseSampleIcs() {
        String ics = "BEGIN:VCALENDAR\r\n" +
                     "PRODID:-//Example Corp.//Calendar 1.0//EN\r\n" +
                     "VERSION:2.0\r\n" +
                     "X-WR-CALNAME:Engineering Sprint\r\n" +
                     "BEGIN:VEVENT\r\n" +
                     "UID:12345-67890-test\r\n" +
                     "DTSTART:20260828T100000Z\r\n" +
                     "DTEND:20260828T113000Z\r\n" +
                     "SUMMARY:FastJava Architecture Sync\r\n" +
                     "DESCRIPTION:Quarterly review of zero-allocation libraries and\\n\r\n" +
                     " SIMD vector optimizers.\r\n" +
                     "LOCATION:Virtual Room Alpha\r\n" +
                     "ORGANIZER;CN=Andre Stubbe:mailto:andre@fastjava.org\r\n" +
                     "ATTENDEE;CN=Developer One;ROLE=REQ-PARTICIPANT:mailto:dev1@fastjava.org\r\n" +
                     "RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=FR\r\n" +
                     "STATUS:CONFIRMED\r\n" +
                     "BEGIN:VALARM\r\n" +
                     "ACTION:DISPLAY\r\n" +
                     "TRIGGER:-PT15M\r\n" +
                     "DESCRIPTION:15 minute reminder\r\n" +
                     "END:VALARM\r\n" +
                     "END:VEVENT\r\n" +
                     "BEGIN:VTODO\r\n" +
                     "UID:todo-99\r\n" +
                     "SUMMARY:Ship FastCalendar 0.1.0\r\n" +
                     "STATUS:IN-PROCESS\r\n" +
                     "END:VTODO\r\n" +
                     "END:VCALENDAR\r\n";

        VCalendar cal = FastCalendar.parse(ics);
        assertNotNull(cal);
        assertEquals("Engineering Sprint", cal.getName());
        assertEquals(1, cal.getEvents().size());
        assertEquals(1, cal.getTodos().size());

        VEvent ev = cal.getEvents().get(0);
        assertEquals("12345-67890-test", ev.getUid());
        assertEquals("FastJava Architecture Sync", ev.getSummary());
        assertTrue(ev.getDescription().contains("Quarterly review of zero-allocation libraries"));
        assertEquals("Virtual Room Alpha", ev.getLocation());
        assertEquals("mailto:andre@fastjava.org", ev.getOrganizer().getCalAddress());
        assertEquals("Andre Stubbe", ev.getOrganizer().getCommonName());
        assertEquals(1, ev.getAttendees().size());
        assertEquals("Developer One", ev.getAttendees().get(0).getCommonName());
        assertNotNull(ev.getRrule());
        assertEquals(RRule.Frequency.WEEKLY, ev.getRrule().getFrequency());
        assertEquals(1, ev.getAlarms().size());
        assertEquals(-900, ev.getAlarms().get(0).getTriggerSeconds());

        // Roundtrip serialization
        String serialized = FastCalendar.write(cal);
        assertTrue(serialized.contains("BEGIN:VCALENDAR"));
        assertTrue(serialized.contains("SUMMARY:FastJava Architecture Sync"));
        assertTrue(serialized.contains("END:VCALENDAR"));

        VCalendar roundtrip = FastCalendar.parse(serialized);
        assertEquals(1, roundtrip.getEvents().size());
        assertEquals(ev.getSummary(), roundtrip.getEvents().get(0).getSummary());
    }
}
