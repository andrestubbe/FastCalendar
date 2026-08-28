package fastcalendar.benchmark;

import fastcalendar.FastCalendar;
import fastcalendar.FastDateTime;
import fastcalendar.FreeBusyCalculator;
import fastcalendar.RRule;
import fastcalendar.RRuleEvaluator;
import fastcalendar.VCalendar;
import fastcalendar.VEvent;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class CalendarBenchmark {

    private String sampleIcs;
    private VCalendar sampleCalendar;
    private RRule weeklyRule;
    private long ruleStart;
    private long windowStart;
    private long windowEnd;
    private List<VEvent> indexEvents;

    @Setup
    public void setup() {
        sampleIcs = 
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//FastJava//FastCalendar Benchmark//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:bench-001\r\n" +
            "DTSTART:20260828T090000Z\r\n" +
            "DTEND:20260828T103000Z\r\n" +
            "SUMMARY:High Speed Benchmark Event\r\n" +
            "DESCRIPTION:Testing zero-allocation throughput and line unfolding performance.\r\n" +
            "LOCATION:Bench Room Alpha\r\n" +
            "ORGANIZER;CN=FastJava:mailto:bench@fastjava.org\r\n" +
            "ATTENDEE;CN=Runner 1;ROLE=REQ-PARTICIPANT:mailto:r1@fastjava.org\r\n" +
            "ATTENDEE;CN=Runner 2;ROLE=OPT-PARTICIPANT:mailto:r2@fastjava.org\r\n" +
            "RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR;COUNT=100\r\n" +
            "STATUS:CONFIRMED\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n";

        sampleCalendar = FastCalendar.parse(sampleIcs);

        weeklyRule = RRule.weekly()
            .interval(2)
            .byDays(RRule.WeekDay.MO, RRule.WeekDay.WE, RRule.WeekDay.FR)
            .count(250)
            .build();

        ruleStart = FastDateTime.toEpochMillis(2026, 1, 1, 10, 0, 0);
        windowStart = FastDateTime.toEpochMillis(2026, 1, 1, 0, 0, 0);
        windowEnd = FastDateTime.toEpochMillis(2028, 1, 1, 0, 0, 0);

        indexEvents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            long s = ruleStart + i * 3600_000L;
            indexEvents.add(VEvent.builder()
                .uid("evt-" + i)
                .summary("Event " + i)
                .dtStart(s)
                .dtEnd(s + 1800_000L)
                .build());
        }
    }

    @Benchmark
    public VCalendar benchmarkIcsParse() {
        return FastCalendar.parse(sampleIcs);
    }

    @Benchmark
    public String benchmarkIcsSerialize() {
        return FastCalendar.write(sampleCalendar);
    }

    @Benchmark
    public long[] benchmarkRRuleExpansion() {
        return RRuleEvaluator.expand(ruleStart, weeklyRule, windowStart, windowEnd);
    }

    @Benchmark
    public List<FreeBusyCalculator.TimeSlot> benchmarkFreeBusyCalculation() {
        return FreeBusyCalculator.computeBusyIntervals(indexEvents, windowStart, windowEnd);
    }
}
