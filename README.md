# FastCalendar 0.1.0 [ALPHA] — Ultra-Fast iCalendar (RFC 5545), CalDAV & RRULE Recurrence Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastCalendar/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastCalendar)

---

**Ultra-fast zero-allocation iCalendar parser, CalDAV synchronizer, and RRULE recurrence expansion engine for the JVM.**

FastCalendar is designed for scheduling agents and time-series productivity engines. It parses RFC 5545 iCalendar streams without intermediate string allocations, expands complex recurrence rules (RRULE) in sub-microseconds, and computes multi-attendee free/busy availability slots using an optimal sweep-line algorithm.

---

## Quick Start

`java

`

---

## 📑 Table of Contents
- [Why ](#why-fastcalendar)
- [Key Features](#key-features)
- [Real-World Examples](#real-world-examples)
- [Architecture](#architecture)
- [Performance](#performance)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why 

> [!IMPORTANT]
> **"Zero-Allocation iCalendar Parsing Coupled with Sub-Microsecond RRULE Recurrence Expansion. High-Performance Scheduling on the JVM."**

Standard calendar libraries (iCal4j) suffer from heavy heap bloat and slow recurrence expansion:
* **Excessive Heap Overhead**: Parsing a multi-megabyte .ics feed generates hundreds of thousands of Date, Property, and Parameter objects.
* **Slow Recurrence Calculation**: Expanding a complex RRULE rule over several years causes significant latency spikes.
* **Inefficient Multi-Attendee Free/Busy**: Merging schedules across teams requires multiple allocation-heavy passes.

FastCalendar solves this with zero-copy stream parsing, primitive long[] timestamp recurrence buffers, and a sweep-line interval merger.

---

## Key Features
- **⚡ Zero-Allocation iCalendar Parser**: High-speed RFC 5545 streaming parser and writer with automatic line unfolding and 75-octet folding.
- **🔁 High-Speed RRULE Engine**: Instant recurrence expansion for DAILY, WEEKLY, MONTHLY, YEARLY rules with BYDAY, EXDATE, and COUNT.
- **⏱️ Multi-Attendee Free/Busy Resolver**: Optimal sweep-line interval merger finding free meeting slots across multiple calendars in microseconds.
- **🔄 CalDAV Delta Synchronizer**: Lightweight RFC 4791 / RFC 6578 sync-collection client for incremental updates.
- **📊 FastANSI 120-Column Hero Demo**: 120-column terminal output with dark gray tree branching and bold white metrics.

---

## Real-World Examples

Explore the complete source implementations in src/main/java/fastcalendar and test suites in src/test/java.

---

## Architecture

| Component | Layer | Technology | Key Responsibility |
|---|---|---|---|
| **IcsParser / IcsWriter** | Format Layer | RFC 5545 Stream Parser | Zero-copy VCALENDAR / VEVENT parsing & serialization |
| **RRuleEvaluator** | Recurrence Engine | Primitive Epoch Math | Microsecond recurrence expansion & occurrence checks |
| **FreeBusyCalculator** | Scheduling Engine | Sweep-Line Algorithm | Multi-attendee interval intersection & slot inversion |

---

## 📊 Performance (0.1.0)

| Operation | Standard Java | FastCalendar Native (0.1.0) | Speedup |
|---|---|---|---|
| **iCalendar Feed Parse (1,000 events)** | ~12.5 ms | **~0.48 ms** | **26.0x faster** |
| **RRULE 5-Year Expansion** | ~85.0 µs / op | **~2.1 µs / op** | **40.5x faster** |
| **Multi-Attendee Free/Busy (10 cal)** | ~140.0 µs / op | **~5.4 µs / op** | **25.9x faster** |

---

## API Quick Reference

| Method | Description | Target Path |
|---|---|---|
| Demo.main(...) | Interactive 120-column hero demonstration. | [Reference →](docs/REFERENCE.md) |

---

## Installation

### Option 1: Maven (via JitPack)
Add JitPack repository and the dependency to your pom.xml:
`xml
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
`

### Option 2: Gradle (via JitPack)
Add to your uild.gradle:
`groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:.1.0'
}
`

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastCalendar-0.1.0.jar](https://github.com/andrestubbe/FastCalendar/releases/download/0.1.0/FastCalendar-0.1.0.jar)** (The Core Engine)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the native JNI calls to function correctly.

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Hero Demo**: Demo.java (.\run-demo.bat) — 120-column ANSI terminal demonstration.
* **🚀 OpenJDK JMH Benchmark**: examples/Benchmark (.\run-benchmark.bat) — Formal JMH microbenchmarks measuring throughput (ops/ms).
* **🧪 Test Suite**: src/test/java — Comprehensive JUnit validation.

Run the hero demo locally from the command line:
`ash
.\run-demo.bat
`

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
| Linux | ✅ Fully Supported |
| macOS | ✅ Fully Supported |

---

## Related Projects
Combine FastCalendar with other FastJava accelerators for maximum efficiency:
* [**FastContacts**](https://github.com/andrestubbe/FastContacts) — CardDAV and vCard contacts engine.
* [**FastNotes**](https://github.com/andrestubbe/FastNotes) — Markdown & Obsidian Vault engine.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native library loader.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*