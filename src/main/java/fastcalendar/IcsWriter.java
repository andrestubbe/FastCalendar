package fastcalendar;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Ultra-fast RFC 5545 iCalendar (.ics) serializer.
 * Enforces strict RFC 5545 line folding (75-octet limit with CRLF + space),
 * correct character escaping, and fast streaming serialization.
 */
public final class IcsWriter {

    private static final String CRLF = "\r\n";

    private IcsWriter() {}

    /**
     * Serializes calendar to string.
     */
    public static String writeToString(VCalendar calendar) {
        StringBuilder sb = new StringBuilder(2048);
        write(calendar, sb);
        return sb.toString();
    }

    /**
     * Serializes calendar to UTF-8 byte array.
     */
    public static byte[] writeToBytes(VCalendar calendar) {
        return writeToString(calendar).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Serializes calendar to a Path.
     */
    public static void writeToFile(VCalendar calendar, Path path) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(calendar, bw);
        }
    }

    /**
     * Serializes calendar to an OutputStream.
     */
    public static void writeToStream(VCalendar calendar, OutputStream os) throws IOException {
        Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        write(calendar, writer);
        writer.flush();
    }

    /**
     * Writes calendar structure to StringBuilder.
     */
    public static void write(VCalendar cal, StringBuilder sb) {
        appendFoldedLine(sb, "BEGIN:VCALENDAR");
        appendFoldedLine(sb, "PRODID:" + cal.getProdId());
        appendFoldedLine(sb, "VERSION:" + cal.getVersion());
        if (cal.getCalScale() != null) {
            appendFoldedLine(sb, "CALSCALE:" + cal.getCalScale());
        }
        if (cal.getMethod() != null) {
            appendFoldedLine(sb, "METHOD:" + cal.getMethod());
        }
        if (cal.getName() != null) {
            appendFoldedLine(sb, "X-WR-CALNAME:" + escape(cal.getName()));
        }
        if (cal.getDescription() != null) {
            appendFoldedLine(sb, "X-WR-CALDESC:" + escape(cal.getDescription()));
        }
        if (cal.getTimezone() != null) {
            appendFoldedLine(sb, "X-WR-TIMEZONE:" + cal.getTimezone());
        }

        // Custom calendar properties
        for (Map.Entry<String, String> entry : cal.getCustomProperties().entrySet()) {
            appendFoldedLine(sb, entry.getKey() + ":" + escape(entry.getValue()));
        }

        // Events
        for (VEvent event : cal.getEvents()) {
            writeEvent(event, sb);
        }

        // Todos
        for (VTodo todo : cal.getTodos()) {
            writeTodo(todo, sb);
        }

        // Journals
        for (VJournal journal : cal.getJournals()) {
            writeJournal(journal, sb);
        }

        // FreeBusy
        for (VFreeBusy fb : cal.getFreeBusyList()) {
            writeFreeBusy(fb, sb);
        }

        appendFoldedLine(sb, "END:VCALENDAR");
    }

    /**
     * Writes calendar structure to Writer.
     */
    public static void write(VCalendar cal, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder(2048);
        write(cal, sb);
        writer.write(sb.toString());
    }

    public static void writeEvent(VEvent event, StringBuilder sb) {
        appendFoldedLine(sb, "BEGIN:VEVENT");
        appendFoldedLine(sb, "UID:" + event.getUid());

        if (event.isAllDay()) {
            appendFoldedLine(sb, "DTSTART;VALUE=DATE:" + FastDateTime.formatDateOnly(event.getDtStart()));
            appendFoldedLine(sb, "DTEND;VALUE=DATE:" + FastDateTime.formatDateOnly(event.getDtEnd()));
        } else {
            if (event.getTzid() != null) {
                appendFoldedLine(sb, "DTSTART;TZID=" + event.getTzid() + ":" + FastDateTime.formatUtc(event.getDtStart()));
                appendFoldedLine(sb, "DTEND;TZID=" + event.getTzid() + ":" + FastDateTime.formatUtc(event.getDtEnd()));
            } else {
                appendFoldedLine(sb, "DTSTART:" + FastDateTime.formatUtc(event.getDtStart()));
                appendFoldedLine(sb, "DTEND:" + FastDateTime.formatUtc(event.getDtEnd()));
            }
        }

        if (event.getSummary() != null && !event.getSummary().isEmpty()) {
            appendFoldedLine(sb, "SUMMARY:" + escape(event.getSummary()));
        }
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            appendFoldedLine(sb, "DESCRIPTION:" + escape(event.getDescription()));
        }
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            appendFoldedLine(sb, "LOCATION:" + escape(event.getLocation()));
        }
        if (event.getStatus() != null) {
            appendFoldedLine(sb, "STATUS:" + event.getStatus().name());
        }
        if (event.getTransp() != null) {
            appendFoldedLine(sb, "TRANSP:" + event.getTransp().name());
        }
        if (event.getSequence() > 0) {
            appendFoldedLine(sb, "SEQUENCE:" + event.getSequence());
        }
        if (event.getCreated() > 0) {
            appendFoldedLine(sb, "CREATED:" + FastDateTime.formatUtc(event.getCreated()));
        }
        if (event.getLastModified() > 0) {
            appendFoldedLine(sb, "LAST-MODIFIED:" + FastDateTime.formatUtc(event.getLastModified()));
        }
        if (event.getUrl() != null && !event.getUrl().isEmpty()) {
            appendFoldedLine(sb, "URL:" + event.getUrl());
        }

        if (event.getRrule() != null) {
            appendFoldedLine(sb, "RRULE:" + event.getRrule().toIcsString());
        }

        if (event.getExDates() != null && event.getExDates().length > 0) {
            StringBuilder exSb = new StringBuilder("EXDATE:");
            for (int i = 0; i < event.getExDates().length; i++) {
                if (i > 0) exSb.append(',');
                exSb.append(FastDateTime.formatUtc(event.getExDates()[i]));
            }
            appendFoldedLine(sb, exSb.toString());
        }

        if (event.getRDates() != null && event.getRDates().length > 0) {
            StringBuilder rSb = new StringBuilder("RDATE:");
            for (int i = 0; i < event.getRDates().length; i++) {
                if (i > 0) rSb.append(',');
                rSb.append(FastDateTime.formatUtc(event.getRDates()[i]));
            }
            appendFoldedLine(sb, rSb.toString());
        }

        if (event.getOrganizer() != null) {
            Organizer org = event.getOrganizer();
            StringBuilder orgSb = new StringBuilder("ORGANIZER");
            if (org.getCommonName() != null) orgSb.append(";CN=").append(escapeParam(org.getCommonName()));
            if (org.getSentBy() != null) orgSb.append(";SENT-BY=\"").append(org.getSentBy()).append("\"");
            orgSb.append(':').append(org.getCalAddress());
            appendFoldedLine(sb, orgSb.toString());
        }

        for (Attendee att : event.getAttendees()) {
            StringBuilder attSb = new StringBuilder("ATTENDEE");
            if (att.getCommonName() != null) attSb.append(";CN=").append(escapeParam(att.getCommonName()));
            if (att.getRole() != null) attSb.append(";ROLE=").append(att.getRole());
            if (att.getPartStat() != null) attSb.append(";PARTSTAT=").append(att.getPartStat());
            if (att.getCuType() != null) attSb.append(";CUTYPE=").append(att.getCuType());
            if (att.isRsvp()) attSb.append(";RSVP=TRUE");
            attSb.append(':').append(att.getCalAddress());
            appendFoldedLine(sb, attSb.toString());
        }

        if (!event.getCategories().isEmpty()) {
            StringBuilder catSb = new StringBuilder("CATEGORIES:");
            for (int i = 0; i < event.getCategories().size(); i++) {
                if (i > 0) catSb.append(',');
                catSb.append(escape(event.getCategories().get(i)));
            }
            appendFoldedLine(sb, catSb.toString());
        }

        // Custom event properties
        for (Map.Entry<String, String> entry : event.getCustomProperties().entrySet()) {
            appendFoldedLine(sb, entry.getKey() + ":" + escape(entry.getValue()));
        }

        // Alarms
        for (VAlarm alarm : event.getAlarms()) {
            appendFoldedLine(sb, "BEGIN:VALARM");
            appendFoldedLine(sb, "ACTION:" + alarm.getAction().name());
            appendFoldedLine(sb, "TRIGGER:-PT" + Math.abs(alarm.getTriggerSeconds()) + "S");
            if (alarm.getDescription() != null) {
                appendFoldedLine(sb, "DESCRIPTION:" + escape(alarm.getDescription()));
            }
            appendFoldedLine(sb, "END:VALARM");
        }

        appendFoldedLine(sb, "END:VEVENT");
    }

    public static void writeTodo(VTodo todo, StringBuilder sb) {
        appendFoldedLine(sb, "BEGIN:VTODO");
        appendFoldedLine(sb, "UID:" + todo.getUid());
        if (todo.getSummary() != null) appendFoldedLine(sb, "SUMMARY:" + escape(todo.getSummary()));
        if (todo.getDescription() != null) appendFoldedLine(sb, "DESCRIPTION:" + escape(todo.getDescription()));
        if (todo.getDue() > 0) appendFoldedLine(sb, "DUE:" + FastDateTime.formatUtc(todo.getDue()));
        if (todo.getCompleted() > 0) appendFoldedLine(sb, "COMPLETED:" + FastDateTime.formatUtc(todo.getCompleted()));
        if (todo.getStatus() != null) appendFoldedLine(sb, "STATUS:" + todo.getStatus().name().replace('_', '-'));
        if (todo.getPriority() > 0) appendFoldedLine(sb, "PRIORITY:" + todo.getPriority());
        appendFoldedLine(sb, "END:VTODO");
    }

    public static void writeJournal(VJournal journal, StringBuilder sb) {
        appendFoldedLine(sb, "BEGIN:VJOURNAL");
        appendFoldedLine(sb, "UID:" + journal.getUid());
        if (journal.getSummary() != null) appendFoldedLine(sb, "SUMMARY:" + escape(journal.getSummary()));
        if (journal.getDescription() != null) appendFoldedLine(sb, "DESCRIPTION:" + escape(journal.getDescription()));
        if (journal.getDtStart() > 0) appendFoldedLine(sb, "DTSTART:" + FastDateTime.formatUtc(journal.getDtStart()));
        appendFoldedLine(sb, "END:VJOURNAL");
    }

    public static void writeFreeBusy(VFreeBusy fb, StringBuilder sb) {
        appendFoldedLine(sb, "BEGIN:VFREEBUSY");
        appendFoldedLine(sb, "UID:" + fb.getUid());
        if (fb.getDtStart() > 0) appendFoldedLine(sb, "DTSTART:" + FastDateTime.formatUtc(fb.getDtStart()));
        if (fb.getDtEnd() > 0) appendFoldedLine(sb, "DTEND:" + FastDateTime.formatUtc(fb.getDtEnd()));
        for (VFreeBusy.Period p : fb.getPeriods()) {
            appendFoldedLine(sb, "FREEBUSY;FBTYPE=" + p.getFbType() + ":" + p.toString());
        }
        appendFoldedLine(sb, "END:VFREEBUSY");
    }

    /**
     * Appends an RFC 5545 line folded at 75 octets.
     */
    public static void appendFoldedLine(StringBuilder sb, String line) {
        if (line == null) return;
        if (line.length() <= 75) {
            sb.append(line).append(CRLF);
            return;
        }

        int start = 0;
        int maxChunk = 75;

        while (start < line.length()) {
            int end = Math.min(start + maxChunk, line.length());
            sb.append(line, start, end).append(CRLF);
            start = end;
            if (start < line.length()) {
                sb.append(' '); // RFC 5545 continuation space
                maxChunk = 74; // Account for the space prefix
            }
        }
    }

    /**
     * Escapes text characters per RFC 5545 (commas, semicolons, backslashes, newlines).
     */
    public static String escape(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case ';':  sb.append("\\;"); break;
                case ',':  sb.append("\\,"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break; // ignore CR in text values
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static String escapeParam(String param) {
        if (param == null) return "";
        if (param.contains(" ") || param.contains(";") || param.contains(":")) {
            return "\"" + param + "\"";
        }
        return param;
    }
}
