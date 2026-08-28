package fastcalendar;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CalendarIndexTest {

    @Test
    public void testConflictDetection() {
        CalendarIndex index = new CalendarIndex();

        long start = FastDateTime.toEpochMillis(2026, 8, 28, 14, 0, 0);
        long end = FastDateTime.toEpochMillis(2026, 8, 28, 15, 0, 0);

        VEvent ev = VEvent.builder()
            .uid("event-1")
            .summary("Team Meeting")
            .dtStart(start)
            .dtEnd(end)
            .build();

        index.add(ev);

        // Overlapping window
        long qStart = FastDateTime.toEpochMillis(2026, 8, 28, 14, 30, 0);
        long qEnd = FastDateTime.toEpochMillis(2026, 8, 28, 15, 30, 0);
        assertTrue(index.hasConflict(qStart, qEnd));

        // Non-overlapping window
        long freeStart = FastDateTime.toEpochMillis(2026, 8, 28, 15, 0, 0);
        long freeEnd = FastDateTime.toEpochMillis(2026, 8, 28, 16, 0, 0);
        assertFalse(index.hasConflict(freeStart, freeEnd));

        // Next available slot
        long searchFrom = FastDateTime.toEpochMillis(2026, 8, 28, 14, 0, 0);
        long searchUntil = FastDateTime.toEpochMillis(2026, 8, 28, 18, 0, 0);
        long slotDuration = 30 * 60 * 1000L;
        long nextSlot = index.nextAvailableSlot(searchFrom, slotDuration, searchUntil, 15 * 60 * 1000L);
        assertEquals(end, nextSlot);
    }
}
