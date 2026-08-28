package fastcalendar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ultra-Fast RFC 5545 iCalendar (.ics) streaming parser.
 * Features line unfolding, fast zero-copy parameter extraction, and component construction.
 */
public final class IcsParser {

    private IcsParser() {}

    /**
     * Parses an RFC 5545 iCalendar from a String.
     */
    public static VCalendar parse(String icsContent) {
        if (icsContent == null || icsContent.isEmpty()) {
            return VCalendar.builder().build();
        }
        return parse(new StringReader(icsContent));
    }

    /**
     * Parses an RFC 5545 iCalendar from a byte array.
     */
    public static VCalendar parse(byte[] icsBytes) {
        if (icsBytes == null || icsBytes.length == 0) {
            return VCalendar.builder().build();
        }
        return parse(new ByteArrayInputStream(icsBytes));
    }

    /**
     * Parses an RFC 5545 iCalendar from a Path.
     */
    public static VCalendar parse(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    /**
     * Parses an RFC 5545 iCalendar from a File.
     */
    public static VCalendar parse(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return parse(is);
        }
    }

    /**
     * Parses an RFC 5545 iCalendar from an InputStream.
     */
    public static VCalendar parse(InputStream is) {
        return parse(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Parses an RFC 5545 iCalendar from a Reader.
     */
    public static VCalendar parse(java.io.Reader reader) {
        VCalendar.Builder calBuilder = VCalendar.builder();
        VEvent.Builder eventBuilder = null;
        VTodo.Status todoStatus = VTodo.Status.NEEDS_ACTION;
        String todoUid = null, todoSummary = null, todoDesc = null;
        long todoDue = -1L, todoCompleted = -1L;
        int todoPriority = 0;
        VAlarm.Action currentAlarmAction = null;
        long currentAlarmTrigger = 0;
        String currentAlarmDesc = null;

        String currentComponent = null; // "VCALENDAR", "VEVENT", "VTODO", "VALARM", etc.

        try (BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader, 16384)) {
            String unfoldedLine = null;
            String rawLine;

            while (true) {
                rawLine = br.readLine();
                if (rawLine == null) {
                    if (unfoldedLine != null) {
                        processLine(unfoldedLine, calBuilder, eventBuilder);
                    }
                    break;
                }

                // Check for line continuation (RFC 5545 Section 3.1: line beginning with space or tab)
                if (!rawLine.isEmpty() && (rawLine.charAt(0) == ' ' || rawLine.charAt(0) == '\t')) {
                    if (unfoldedLine != null) {
                        unfoldedLine += rawLine.substring(1);
                    }
                    continue;
                }

                if (unfoldedLine != null) {
                    // Process previous completed unfolded line
                    String trimmed = unfoldedLine.trim();
                    if (!trimmed.isEmpty()) {
                        if (trimmed.startsWith("BEGIN:")) {
                            String comp = trimmed.substring(6).trim().toUpperCase();
                            if ("VEVENT".equals(comp)) {
                                eventBuilder = VEvent.builder();
                                currentComponent = "VEVENT";
                            } else if ("VALARM".equals(comp)) {
                                currentComponent = "VALARM";
                                currentAlarmAction = VAlarm.Action.DISPLAY;
                                currentAlarmTrigger = -900;
                                currentAlarmDesc = null;
                            } else if ("VTODO".equals(comp)) {
                                currentComponent = "VTODO";
                                todoUid = null;
                                todoSummary = null;
                                todoDesc = null;
                                todoDue = -1L;
                                todoCompleted = -1L;
                                todoStatus = VTodo.Status.NEEDS_ACTION;
                                todoPriority = 0;
                            } else if ("VCALENDAR".equals(comp)) {
                                currentComponent = "VCALENDAR";
                            }
                        } else if (trimmed.startsWith("END:")) {
                            String comp = trimmed.substring(4).trim().toUpperCase();
                            if ("VEVENT".equals(comp) && eventBuilder != null) {
                                calBuilder.addEvent(eventBuilder.build());
                                eventBuilder = null;
                                currentComponent = "VCALENDAR";
                            } else if ("VALARM".equals(comp) && eventBuilder != null) {
                                eventBuilder.addAlarm(new VAlarm(currentAlarmAction, currentAlarmTrigger, currentAlarmDesc, null));
                                currentComponent = "VEVENT";
                            } else if ("VTODO".equals(comp)) {
                                calBuilder.addTodo(new VTodo(todoUid, todoSummary, todoDesc, todoDue, todoCompleted, todoStatus, todoPriority));
                                currentComponent = "VCALENDAR";
                            }
                        } else {
                            if ("VALARM".equals(currentComponent)) {
                                int colon = trimmed.indexOf(':');
                                if (colon > 0) {
                                    String propName = trimmed.substring(0, colon).toUpperCase();
                                    String propVal = trimmed.substring(colon + 1);
                                    if (propName.startsWith("ACTION")) {
                                        try { currentAlarmAction = VAlarm.Action.valueOf(propVal.trim().toUpperCase()); } catch (Exception ignored) {}
                                    } else if (propName.startsWith("TRIGGER")) {
                                        currentAlarmTrigger = parseTriggerSeconds(propVal);
                                    } else if (propName.startsWith("DESCRIPTION")) {
                                        currentAlarmDesc = unescape(propVal);
                                    }
                                }
                            } else if ("VTODO".equals(currentComponent)) {
                                int colon = trimmed.indexOf(':');
                                if (colon > 0) {
                                    String propName = trimmed.substring(0, colon).toUpperCase();
                                    String propVal = trimmed.substring(colon + 1);
                                    if (propName.startsWith("UID")) todoUid = propVal.trim();
                                    else if (propName.startsWith("SUMMARY")) todoSummary = unescape(propVal);
                                    else if (propName.startsWith("DESCRIPTION")) todoDesc = unescape(propVal);
                                    else if (propName.startsWith("DUE")) todoDue = FastDateTime.parseIcsDateTime(propVal);
                                    else if (propName.startsWith("COMPLETED")) todoCompleted = FastDateTime.parseIcsDateTime(propVal);
                                    else if (propName.startsWith("STATUS")) {
                                        try { todoStatus = VTodo.Status.valueOf(propVal.trim().replace('-', '_').toUpperCase()); } catch (Exception ignored) {}
                                    } else if (propName.startsWith("PRIORITY")) {
                                        try { todoPriority = Integer.parseInt(propVal.trim()); } catch (Exception ignored) {}
                                    }
                                }
                            } else if (eventBuilder != null) {
                                parseEventProperty(trimmed, eventBuilder);
                            } else {
                                parseCalendarProperty(trimmed, calBuilder);
                            }
                        }
                    }
                }
                unfoldedLine = rawLine;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ICS", e);
        }

        return calBuilder.build();
    }

    private static void processLine(String line, VCalendar.Builder calBuilder, VEvent.Builder eventBuilder) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;
        if (eventBuilder != null && !trimmed.startsWith("END:")) {
            parseEventProperty(trimmed, eventBuilder);
        } else if (!trimmed.startsWith("BEGIN:") && !trimmed.startsWith("END:")) {
            parseCalendarProperty(trimmed, calBuilder);
        }
    }

    private static void parseCalendarProperty(String line, VCalendar.Builder calBuilder) {
        int colon = line.indexOf(':');
        if (colon <= 0) return;
        String namePart = line.substring(0, colon);
        String value = line.substring(colon + 1);

        String propName = namePart.split(";")[0].trim().toUpperCase();

        switch (propName) {
            case "PRODID":
                calBuilder.prodId(value.trim());
                break;
            case "VERSION":
                calBuilder.version(value.trim());
                break;
            case "CALSCALE":
                calBuilder.calScale(value.trim());
                break;
            case "METHOD":
                calBuilder.method(value.trim());
                break;
            case "X-WR-CALNAME":
                calBuilder.name(unescape(value));
                break;
            case "X-WR-CALDESC":
                calBuilder.description(unescape(value));
                break;
            case "X-WR-TIMEZONE":
                calBuilder.timezone(value.trim());
                break;
            default:
                if (propName.startsWith("X-")) {
                    calBuilder.customProperty(propName, unescape(value));
                }
                break;
        }
    }

    private static void parseEventProperty(String line, VEvent.Builder b) {
        int colon = line.indexOf(':');
        if (colon <= 0) return;

        String nameAndParams = line.substring(0, colon);
        String rawValue = line.substring(colon + 1);

        String[] parts = nameAndParams.split(";");
        String propName = parts[0].trim().toUpperCase();
        Map<String, String> params = parseParams(parts);

        switch (propName) {
            case "UID":
                b.uid(rawValue.trim());
                break;
            case "SUMMARY":
                b.summary(unescape(rawValue));
                break;
            case "DESCRIPTION":
                b.description(unescape(rawValue));
                break;
            case "LOCATION":
                b.location(unescape(rawValue));
                break;
            case "DTSTART":
                if ("DATE".equalsIgnoreCase(params.get("VALUE"))) {
                    b.allDay(true);
                }
                if (params.containsKey("TZID")) {
                    b.tzid(params.get("TZID"));
                }
                b.dtStart(FastDateTime.parseIcsDateTime(rawValue));
                break;
            case "DTEND":
                b.dtEnd(FastDateTime.parseIcsDateTime(rawValue));
                break;
            case "DURATION":
                b.durationMillis(parseDurationMillis(rawValue));
                break;
            case "RRULE":
                b.rrule(RRule.parse(rawValue));
                break;
            case "EXDATE":
                b.exDates(parseDateList(rawValue));
                break;
            case "RDATE":
                b.rDates(parseDateList(rawValue));
                break;
            case "ORGANIZER":
                b.organizer(new Organizer(rawValue.trim(), params.get("CN"), params.get("SENT-BY")));
                break;
            case "ATTENDEE":
                b.addAttendee(new Attendee(rawValue.trim(), params.get("CN"), params.get("ROLE"),
                              params.get("PARTSTAT"), params.get("CUTYPE"), "TRUE".equalsIgnoreCase(params.get("RSVP"))));
                break;
            case "STATUS":
                try {
                    b.status(VEvent.Status.valueOf(rawValue.trim().toUpperCase()));
                } catch (Exception ignored) {}
                break;
            case "TRANSP":
                try {
                    b.transp(VEvent.Transp.valueOf(rawValue.trim().toUpperCase()));
                } catch (Exception ignored) {}
                break;
            case "CATEGORIES":
                String[] cats = rawValue.split(",");
                for (String cat : cats) {
                    b.addCategory(unescape(cat.trim()));
                }
                break;
            case "CREATED":
                b.created(FastDateTime.parseIcsDateTime(rawValue));
                break;
            case "LAST-MODIFIED":
                b.lastModified(FastDateTime.parseIcsDateTime(rawValue));
                break;
            case "SEQUENCE":
                try { b.sequence(Integer.parseInt(rawValue.trim())); } catch (Exception ignored) {}
                break;
            case "URL":
                b.url(rawValue.trim());
                break;
            default:
                if (propName.startsWith("X-")) {
                    b.customProperty(propName, unescape(rawValue));
                }
                break;
        }
    }

    private static Map<String, String> parseParams(String[] parts) {
        if (parts.length <= 1) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>(parts.length);
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) {
                String k = parts[i].substring(0, eq).trim().toUpperCase();
                String v = parts[i].substring(eq + 1).trim();
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                    v = v.substring(1, v.length() - 1);
                }
                map.put(k, v);
            }
        }
        return map;
    }

    private static long[] parseDateList(String val) {
        String[] tokens = val.split(",");
        long[] res = new long[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            res[i] = FastDateTime.parseIcsDateTime(tokens[i]);
        }
        return res;
    }

    /**
     * Parses ISO-8601 duration (e.g. "PT1H30M", "P1D", "-PT15M") into milliseconds.
     */
    public static long parseDurationMillis(String d) {
        if (d == null || d.isEmpty()) return 0;
        String s = d.trim().toUpperCase();
        boolean negative = s.startsWith("-");
        if (negative || s.startsWith("+")) s = s.substring(1);

        if (!s.startsWith("P")) return 0;
        s = s.substring(1); // strip P

        long millis = 0;
        int num = 0;
        boolean timeMode = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                num = num * 10 + (c - '0');
            } else if (c == 'T') {
                timeMode = true;
                num = 0;
            } else if (c == 'W') {
                millis += num * 7L * 86400_000L;
                num = 0;
            } else if (c == 'D') {
                millis += num * 86400_000L;
                num = 0;
            } else if (c == 'H') {
                millis += num * 3600_000L;
                num = 0;
            } else if (c == 'M') {
                if (timeMode) {
                    millis += num * 60_000L;
                } else {
                    // Month approximation in duration (30 days)
                    millis += num * 30L * 86400_000L;
                }
                num = 0;
            } else if (c == 'S') {
                millis += num * 1000L;
                num = 0;
            }
        }

        return negative ? -millis : millis;
    }

    private static long parseTriggerSeconds(String s) {
        long millis = parseDurationMillis(s);
        return millis / 1000L;
    }

    /**
     * Unescapes RFC 5545 escaped characters (\,, \;, \\, \n, \N).
     */
    public static String unescape(String text) {
        if (text == null || text.indexOf('\\') < 0) return text;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == 'n' || next == 'N') {
                    sb.append('\n');
                    i++;
                } else if (next == ',' || next == ';' || next == '\\') {
                    sb.append(next);
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
