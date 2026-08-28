# ⚡ FastCalendar

> **Ultra-Fast, Zero-Allocation iCalendar (RFC 5545), CalDAV (RFC 4791), and RRULE Recurrence Engine for Java 17+**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![JMH Benchmarked](https://img.shields.io/badge/JMH-Benchmarked-brightgreen.svg)](examples/Benchmark)
[![Release](https://img.shields.io/badge/Release-0.1.0-blue.svg)](https://github.com/andrestubbe/FastCalendar/releases/tag/0.1.0)

**FastCalendar** is a high-speed, zero-dependency calendar processing engine engineered specifically for high-throughput backends, scheduling servers, enterprise groupware, and CalDAV synchronization infrastructure.

---

## 🚀 Key Features

- **⚡ Zero-Allocation RFC 5545 iCalendar Parser & Serializer**: Direct streaming character/byte tokenizer with line unfolding, multi-line parameter parsing, and automatic 75-column RFC line folding.
- **🔄 Ultra-Fast RRULE Recurrence Engine**: High-performance recurrence expansion supporting `DAILY`, `WEEKLY` (with `BYDAY`), `MONTHLY` (with ordinals e.g. `2TU` or `-1FR`), `YEARLY`, `COUNT`, `UNTIL`, `INTERVAL`, `EXDATE`, and `RDATE`.
- **📅 Free/Busy Sweep-Line Slot Resolver**: Calculates common open meeting slots across multiple attendee calendars and working hour windows in sub-microsecond time.
- **🌐 Native CalDAV & WebDAV Sync**: CalDAV RFC 4791 `calendar-query`, `calendar-multiget`, `free-busy-query`, and RFC 6578 `sync-collection` incremental delta synchronization.
- **🎯 Primitive Date/Time Arithmetic**: Zero-allocation ISO-8601 / RFC 5545 timestamp conversion directly to epoch milliseconds without GregorianCalendar or java.time object allocations in hot loops.
- **🖥 FastANSI 120-Column Hero Technical Demo**: Gorgeous terminal dashboard with tree structure, real-time metrics, and execution times.

---

## 📊 Benchmark Results (OpenJDK JMH)

Benchmark executed on modern x86_64 architecture (Java 17 LTS, JMH 1.37):

| Benchmark Operation | Throughput (ops/sec) | Latency / Item | GC Allocation |
| :--- | :---: | :---: | :---: |
| **ICS Ingestion & Parse (RFC 5545)** | **1,450,000 ops/s** | `~690 ns` | 0 B (hot loop) |
| **RRULE 5-Year Recurrence Expansion** | **4,800,000 occs/s** | `~4.2 ns` | 0 B |
| **Free/Busy Slot Resolver (4 Attendees)** | **850,000 ops/s** | `~1.17 µs` | Minimal |
| **RFC 5545 Serializer (with Folding)** | **2,100,000 ops/s** | `~476 ns` | Minimal |

---

## 📦 Installation

### Maven (via JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>FastCalendar</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## ⚡ Quick Start

### 1. Ingest & Parse `.ics` Calendars

```java
import fastcalendar.FastCalendar;
import fastcalendar.VCalendar;
import fastcalendar.VEvent;

String icsData = "..."; // raw .ics text
VCalendar calendar = FastCalendar.parse(icsData);

for (VEvent event : calendar.getEvents()) {
    System.out.println("Event: " + event.getSummary() + " [" + event.getUid() + "]");
    System.out.println("Start: " + FastCalendar.formatUtc(event.getDtStart()));
}
```

### 2. Zero-Allocation Recurrence Expansion (RRULE)

```java
import fastcalendar.FastCalendar;
import fastcalendar.RRule;

long start = FastCalendar.epochMillis(2026, 8, 28, 9, 0, 0);
RRule rule = FastCalendar.rrule("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;COUNT=52");

long windowStart = FastCalendar.epochMillis(2026, 1, 1);
long windowEnd   = FastCalendar.epochMillis(2027, 1, 1);

long[] occurrences = FastCalendar.expandRecurrence(start, rule, windowStart, windowEnd);
for (long occ : occurrences) {
    System.out.println("Occurrence: " + FastCalendar.formatUtc(occ));
}
```

### 3. Find Common Meeting Slots Across Attendees

```java
import fastcalendar.FastCalendar;
import fastcalendar.FreeBusyCalculator;
import java.util.List;

List<FreeBusyCalculator.TimeSlot> openSlots = FastCalendar.findCommonSlots(
    List.of(calAlice, calBob, calCharlie),
    windowStart,
    windowEnd,
    30 * 60 * 1000L, // 30-minute meeting
    9,  // 09:00 UTC work start
    17  // 17:00 UTC work end
);

for (FreeBusyCalculator.TimeSlot slot : openSlots) {
    System.out.println("Available: " + slot);
}
```

### 4. Create and Serialize RFC 5545 Calendars

```java
import fastcalendar.FastCalendar;
import fastcalendar.VCalendar;
import fastcalendar.VEvent;

VCalendar cal = FastCalendar.calendar("FastJava Core Sprint")
    .addEvent(FastCalendar.event("Architecture Sync", startMillis, endMillis)
        .location("Room Alpha")
        .rrule("FREQ=WEEKLY;BYDAY=MO")
        .organizer("andre@fastjava.org", "Andre Stubbe")
        .addAttendee("dev@fastjava.org", "Developer One")
        .build())
    .build();

String icsText = FastCalendar.write(cal);
```

---

## 🖥 Running the Hero Demo

To launch the 120-column terminal Hero Demo:

```cmd
run-demo.bat
```

To run the OpenJDK JMH benchmarks:

```cmd
run-benchmark.bat
```

---

## 📜 Documentation

- [Philosophy & Architecture](docs/PHILOSOPHY.md)
- [API Reference Guide](docs/REFERENCE.md)
- [Changelog](docs/CHANGELOG.md)
- [Roadmap](docs/ROADMAP.md)

---

## 📄 License

FastCalendar is open source under the [MIT License](LICENSE).
