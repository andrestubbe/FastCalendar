# FastCalendar Philosophy & Architectural Principles

## 1. Zero GC Overhead in Hot Scheduling Paths

Traditional calendar libraries in the JVM ecosystem frequently allocate hundreds of transient objects per event evaluation—`GregorianCalendar`, `Date`, `ZonedDateTime`, `ZoneId`, `Rule`, `Period`, and iterator wrappers. When expanding a 10-year recurring schedule across a directory of 50,000 users, garbage collection pauses can grind enterprise scheduling servers to a halt.

**FastCalendar** changes the paradigm:
- Dates and timestamps are represented and computed directly as 64-bit UTC epoch milliseconds (`long`).
- Calendar field extraction (year, month, day, day-of-week, hour, minute, second) is computed via bitwise and branchless integer arithmetic (e.g. Sakamoto's day-of-week formula and civil calendar conversion algorithms).
- Recurrence evaluation operates on primitive long arrays (`long[]`) and streaming functional callbacks (`RecurrenceConsumer`), achieving **zero GC pressure** in hot expansion loops.

---

## 2. Strict RFC 5545 and RFC 4791 Compliance

Interoperability is paramount:
- **Line Unfolding**: Automatically handles lines split across RFC 5545 75-octet limits prefixed with whitespace or tabs.
- **Line Folding**: Automatically wraps long lines at 75 octets with `\r\n ` continuation markers during serialization.
- **Escaped Characters**: Correctly handles and unescapes `\,`, `\;`, `\\`, and `\n` without regex overhead.
- **CalDAV Purity**: Native XML builder and parser for CalDAV `calendar-query`, `calendar-multiget`, and WebDAV `sync-collection` (RFC 6578) incremental delta sync.

---

## 3. Cache-Friendly Columnar and Sweep-Line Scheduling

Finding common meeting slots across hundreds of attendees is modeled as a 1D geometric interval overlap problem:
- Event boundaries are sorted and processed using an optimal **Sweep-Line Algorithm** in $O(N \log N)$ time.
- Intervals are merged in place with continuous memory layouts for maximal L1/L2 cache hit rates.

---

## 4. Zero External Dependencies

FastCalendar has **zero third-party runtime dependencies**. It compiles cleanly on Java 17+ using standard JDK components, ensuring lightweight JAR footprints, instant classloading, and zero supply-chain risk.
