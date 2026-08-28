# Changelog

All notable changes to **FastCalendar** will be documented in this file.

## [0.1.0] - 2026-08-28

### Initial Release
- **RFC 5545 Parsing Engine**: Streaming parser for VCALENDAR, VEVENT, VTODO, VJOURNAL, VFREEBUSY, and VALARM with unfolded line support.
- **Zero-Allocation RRULE Recurrence Engine**: High-speed recurrence expansion supporting DAILY, WEEKLY, MONTHLY, YEARLY, BYDAY ordinals, COUNT, UNTIL, INTERVAL, EXDATE, and RDATE.
- **Sweep-Line Free/Busy Resolver**: Multi-calendar available meeting slot finder respecting working hours.
- **CalDAV (RFC 4791) & RFC 6578 Sync Engine**: Native XML generation and multistatus delta synchronization.
- **FastDateTime**: Branchless and zero-allocation epoch millis conversions and ISO-8601 formatting.
- **FastANSI 120-Column Hero Technical Demo**: Comprehensive terminal dashboard.
- **OpenJDK JMH Benchmark Suite**: Comprehensive benchmark tests in `examples/Benchmark`.
