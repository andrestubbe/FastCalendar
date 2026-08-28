# FastCalendar API Reference

## Class Overview

| Class | Package | Description |
| :--- | :--- | :--- |
| `FastCalendar` | `fastcalendar` | Primary facade providing static factories for parsing, building, indexing, and serializing. |
| `FastDateTime` | `fastcalendar` | High-speed primitive civil date arithmetic, Sakamoto day-of-week, and epoch millis conversion. |
| `IcsParser` | `fastcalendar` | Streaming RFC 5545 `.ics` parser with line unfolding and parameter extraction. |
| `IcsWriter` | `fastcalendar` | RFC 5545 serializer with automatic 75-octet line folding and character escaping. |
| `RRule` | `fastcalendar` | Recurrence rule model supporting FREQ, INTERVAL, COUNT, UNTIL, BYDAY, BYMONTH, etc. |
| `RRuleEvaluator` | `fastcalendar` | Zero-allocation recurrence occurrence generator with EXDATE and RDATE support. |
| `CalendarIndex` | `fastcalendar` | Temporal event index for range queries, point-in-time lookups, and conflict checks. |
| `FreeBusyCalculator` | `fastcalendar` | Multi-attendee sweep-line meeting slot resolver and interval merger. |
| `CalDavClient` | `fastcalendar` | HTTP client for CalDAV (RFC 4791) queries, delta-sync (RFC 6578), PUT, and DELETE. |
| `CalDavXml` | `fastcalendar` | Lightweight zero-dependency XML builder and multistatus response parser. |
| `VCalendar` | `fastcalendar` | Root RFC 5545 calendar container. |
| `VEvent` | `fastcalendar` | RFC 5545 event model. |
| `VTodo` | `fastcalendar` | RFC 5545 to-do / task model. |
| `VJournal` | `fastcalendar` | RFC 5545 journal entry model. |
| `VFreeBusy` | `fastcalendar` | RFC 5545 free/busy component model. |
| `VAlarm` | `fastcalendar` | RFC 5545 alarm / reminder component. |
| `Attendee` | `fastcalendar` | RFC 5545 attendee representation. |
| `Organizer` | `fastcalendar` | RFC 5545 organizer representation. |

---

## Core Methods

### `FastCalendar`
```java
// Parsing
public static VCalendar parse(String icsContent);
public static VCalendar parse(byte[] icsBytes);
public static VCalendar parse(Path path) throws IOException;
public static VCalendar parse(InputStream is);

// Serialization
public static String write(VCalendar calendar);
public static void write(VCalendar calendar, Path path) throws IOException;

// Builders
public static VCalendar.Builder calendar();
public static VCalendar.Builder calendar(String name);
public static VEvent.Builder event();
public static VEvent.Builder event(String summary, long startMillis, long endMillis);
public static RRule rrule(String rruleText);

// Evaluation & Scheduling
public static long[] expandRecurrence(long dtStart, RRule rule, long windowStart, long windowEnd);
public static List<TimeSlot> findCommonSlots(List<VCalendar> calendars, long windowStart, long windowEnd, long meetingDurationMillis, int workStartHour, int workEndHour);

// Date Helpers
public static long epochMillis(int year, int month, int day, int hour, int minute, int second);
public static String formatUtc(long epochMillis);
public static String formatDate(long epochMillis);
```
