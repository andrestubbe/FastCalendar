package fastcalendar;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CalDavTest {

    @Test
    public void testBuildCalendarQueryXml() {
        long start = FastDateTime.toEpochMillis(2026, 8, 1, 0, 0, 0);
        long end = FastDateTime.toEpochMillis(2026, 8, 31, 23, 59, 59);

        String xml = CalDavXml.buildCalendarQueryXml(start, end);
        assertNotNull(xml);
        assertTrue(xml.contains("<C:calendar-query"));
        assertTrue(xml.contains("20260801T000000Z"));
        assertTrue(xml.contains("20260831T235959Z"));
    }

    @Test
    public void testParseMultistatus() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                     "<d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n" +
                     "  <d:response>\n" +
                     "    <d:href>/calendars/user/work/event1.ics</d:href>\n" +
                     "    <d:propstat>\n" +
                     "      <d:prop>\n" +
                     "        <d:getetag>\"etag-12345\"</d:getetag>\n" +
                     "        <c:calendar-data>BEGIN:VCALENDAR\r\n" +
                     "VERSION:2.0\r\n" +
                     "PRODID:-//Test//EN\r\n" +
                     "BEGIN:VEVENT\r\n" +
                     "UID:evt-1\r\n" +
                     "DTSTART:20260828T120000Z\r\n" +
                     "DTEND:20260828T130000Z\r\n" +
                     "SUMMARY:Client Review\r\n" +
                     "END:VEVENT\r\n" +
                     "END:VCALENDAR</c:calendar-data>\n" +
                     "      </d:prop>\n" +
                     "      <d:status>HTTP/1.1 200 OK</d:status>\n" +
                     "    </d:propstat>\n" +
                     "  </d:response>\n" +
                     "</d:multistatus>";

        List<CalDavXml.ResourceResponse> responses = CalDavXml.parseMultistatus(xml);
        assertEquals(1, responses.size());
        CalDavXml.ResourceResponse res = responses.get(0);
        assertEquals("/calendars/user/work/event1.ics", res.getHref());
        assertEquals("etag-12345", res.getEtag());
        assertTrue(res.isSuccess());

        VCalendar parsed = res.parseCalendar();
        assertNotNull(parsed);
        assertEquals(1, parsed.getEvents().size());
        assertEquals("Client Review", parsed.getEvents().get(0).getSummary());
    }
}
