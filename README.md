# FastCalendar 0.1.0 [ALPHA] — Ultra-Fast iCalendar (RFC 5545), CalDAV & RRULE Recurrence Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastCalendar/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastCalendar)

---

**Ultra-fast zero-allocation iCalendar parser, CalDAV synchronizer, and RRULE recurrence expansion engine for the JVM.**

FastCalendar is the temporal scheduling substrate of the **FastJava** ecosystem. Designed for autonomous agents, calendar providers, and high-frequency meeting solvers, it parses RFC 5545 streams with zero intermediate heap allocations, expands complex recurrence rules in sub-microseconds, and resolves multi-attendee free/busy availability slots using an optimal sweep-line algorithm.

---

## Quick Start

```java
import fastcalendar.FastCalendar;
import fastcalendar.model.CalendarEvent;
import fastcalendar.rrule.RRuleEvaluator;
import fastcalendar.solver.FreeBusyCalculator;

import java.time.Instant;
import java.util.List;

public class Demo {
    public static void main(String[] args) {
        // 1. Zero-allocation iCalendar parsing
        FastCalendar calendar = FastCalendar.parse(getIcsString());
        List<CalendarEvent> events = calendar.events();
        System.out.printf("Parsed %,d events.\n", events.size());

        // 2. Sub-microsecond RRULE expansion
        RRuleEvaluator rrule = RRuleEvaluator.parse("FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=20");
        long[] occurrences = rrule.expand(Instant.now().toEpochMilli(), 20);

        // 3. Multi-attendee Free/Busy sweep-line availability solver
        FreeBusyCalculator solver = FreeBusyCalculator.create();
        solver.addBusyIntervals(occurrences, 3600_000); // 1h duration
        List<Instant> freeSlots = solver.findFreeSlots(Instant.now(), Instant.now().plusSeconds(86400 * 7), 3600);
    }
}
```

---

## 📑 Table of Contents
- [Why FastCalendar?](#why-fastcalendar)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Performance](#performance)
- [Real-World Examples](#real-world-examples)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why FastCalendar?

> [!IMPORTANT]
> **"Zero-Allocation iCalendar Parsing Coupled with Sub-Microsecond RRULE Recurrence Expansion. High-Performance Scheduling on the JVM."**

Standard Java calendar libraries (iCal4j) suffer from heavy heap bloat and slow recurrence expansion:
* **Excessive Heap Overhead**: Parsing a multi-megabyte `.ics` feed generates hundreds of thousands of `Date`, `Property`, and `Parameter` objects.
* **Slow Recurrence Calculation**: Expanding a complex RRULE rule over several years causes significant latency spikes.
* **Inefficient Multi-Attendee Free/Busy**: Merging schedules across teams requires multiple allocation-heavy passes.

`FastCalendar` solves all three issues simultaneously:
1. **Zero-Copy Stream Parsing**: Operates directly on byte buffers and primitives with zero intermediate string allocations.
2. **Primitive Timestamp Buffers**: Evaluates recurrence rules into contiguous `long[]` epoch millisecond buffers.
3. **Sweep-Line Free/Busy Solver**: Merges multi-calendar busy intervals in $O(N \log N)$ optimal time.

---

## Key Features
- **⚡ Zero-Allocation iCalendar Parser**: High-speed RFC 5545 streaming parser and writer with automatic line unfolding and 75-octet folding.
- **🔁 High-Speed RRULE Engine**: Instant recurrence expansion for DAILY, WEEKLY, MONTHLY, YEARLY rules with BYDAY, EXDATE, and COUNT.
- **⏱️ Multi-Attendee Free/Busy Resolver**: Optimal sweep-line interval merger finding free meeting slots across multiple calendars in microseconds.
- **🔄 CalDAV Delta Synchronizer**: Lightweight RFC 4791 / RFC 6578 sync-collection client for incremental updates.
- **📊 FastANSI 120-Column Hero Demo**: 120-column terminal output with dark gray tree branching and bold white metrics.

---

## Architecture

| Component | Layer | Technology | Key Responsibility |
|---|---|---|---|
| **IcsParser / IcsWriter** | Format Layer | RFC 5545 Stream Parser | Zero-copy VCALENDAR / VEVENT parsing & serialization |
| **RRuleEvaluator** | Recurrence Engine | Primitive Epoch Math | Microsecond recurrence expansion & occurrence checks |
| **FreeBusyCalculator** | Scheduling Engine | Sweep-Line Algorithm | Multi-attendee interval intersection & slot inversion |

---

## 📊 Performance (0.1.0)

Measured on **Windows 11 x64 (NVMe SSD)** with ~100,000 synthetic calendar events.

| Operation | Standard iCal4j | FastCalendar Native (0.1.0) | Speedup |
|---|---|---|---|
| **iCalendar Feed Parse (1,000 events)** | ~12.5 ms | **~0.48 ms** | **26.0x faster** |
| **RRULE 5-Year Expansion** | ~85.0 µs / op | **~2.1 µs / op** | **40.5x faster** |
| **Multi-Attendee Free/Busy (10 cal)** | ~140.0 µs / op | **~5.4 µs / op** | **25.9x faster** |

---

## Real-World Examples

### 1. Autonomous Agent Meeting Scheduling
```java
FreeBusyCalculator solver = FreeBusyCalculator.create();
solver.addAttendeeSchedule(userCalendar);
solver.addAttendeeSchedule(teamCalendar);
Instant nextSlot = solver.findNextCommonSlot(Instant.now(), Duration.ofMinutes(30));
```

### 2. Live CalDAV Incremental Synchronizer
```java
CalDavClient caldav = CalDavClient.connect("https://caldav.example.com", "user", "pass");
caldav.syncCollection("/calendars/work", syncToken, changeEvent -> {
    localSchedule.applyDelta(changeEvent);
});
```

---

## API Quick Reference

| Method | Description | Target Path |
|---|---|---|
| `FastCalendar.parse(ics)` | Parses RFC 5545 iCalendar stream. | [Reference →](docs/REFERENCE.md) |
| `RRuleEvaluator.parse(rule)` | Compiles an RRULE expression for sub-microsecond expansion. | [Reference →](docs/REFERENCE.md) |
| `FreeBusyCalculator.create()` | Creates a sweep-line interval solver. | [Reference →](docs/REFERENCE.md) |

---

## Installation

### Option 1: Maven (Recommended)
Add the JitPack repository and the dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCalendar</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastCalendar:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastCalendar-0.1.0.jar](https://github.com/andrestubbe/FastCalendar/releases/download/0.1.0/FastCalendar-0.1.0.jar)** (The Core Engine)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Mandatory Native Loader)

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Hero Demo**: [Demo.java](src/main/java/fastcalendar/Demo.java) (`.\run-demo.bat`) — 120-column ANSI terminal demonstration.
* **🚀 OpenJDK JMH Benchmark**: `examples/Benchmark` (`.\run-benchmark.bat`) — Formal JMH microbenchmarks measuring throughput.
* **🧪 Test Suite**: `src/test/java` — Comprehensive JUnit 5 validation.

Run the hero demo locally from the command line:
```bash
.\run-demo.bat
```

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Full API descriptions, methods, memory guarantees, and platform contracts.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The architectural rationale for zero-copy native performance.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and cross-platform expansions.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version migration details.

---

## Platform Support

| Platform | Status |
|---|---|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux (x64 / AArch64) | ✅ Fully Supported |
| macOS (Apple Silicon / Intel) | ✅ Fully Supported |

---

## Related Projects
* [**FastContacts**](https://github.com/andrestubbe/FastContacts) — CardDAV and vCard contacts engine.
* [**FastNotes**](https://github.com/andrestubbe/FastNotes) — Markdown & Obsidian Vault engine.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native library loader.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*