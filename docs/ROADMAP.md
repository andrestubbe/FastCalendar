# FastCalendar Roadmap

## v0.2.0 - Scheduled Enhancements
- [ ] **iMIP (RFC 6047) Email Invitation Protocol**: Built-in MIME multipart generator for sending meeting invites, updates, and cancellations via SMTP.
- [ ] **VTIMEZONE Native Olson DB**: Built-in fast embedded timezone transition table for localized recurrences across daylight saving shifts without JDK timezone lookups.
- [ ] **VCard Integration**: Interop layer with FastCard / RFC 6350 contacts.

## v0.3.0 - Enterprise Infrastructure
- [ ] **Lock-Free Memory-Mapped Calendar Store**: Shared-memory persistence engine for multi-process calendar synchronization.
- [ ] **GraalVM Native Image Support**: Sub-millisecond startup for microservice workers and AWS Lambda / serverless calendar webhook handlers.
- [ ] **CalDAV Server Reference Engine**: Ultra-fast standalone CalDAV embedded HTTP server backend.
