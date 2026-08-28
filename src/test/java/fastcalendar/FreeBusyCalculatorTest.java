package fastcalendar;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class FreeBusyCalculatorTest {

    @Test
    public void testMergeIntervals() {
        List<FreeBusyCalculator.TimeSlot> raw = new java.util.ArrayList<>();
        raw.add(new FreeBusyCalculator.TimeSlot(1000, 2000));
        raw.add(new FreeBusyCalculator.TimeSlot(1500, 2500));
        raw.add(new FreeBusyCalculator.TimeSlot(3000, 4000));

        List<FreeBusyCalculator.TimeSlot> merged = FreeBusyCalculator.mergeIntervals(raw);
        assertEquals(2, merged.size());
        assertEquals(1000, merged.get(0).getStart());
        assertEquals(2500, merged.get(0).getEnd());
        assertEquals(3000, merged.get(1).getStart());
        assertEquals(4000, merged.get(1).getEnd());
    }

    @Test
    public void testComputeFreeIntervals() {
        List<FreeBusyCalculator.TimeSlot> busy = new java.util.ArrayList<>();
        busy.add(new FreeBusyCalculator.TimeSlot(1000, 2000));
        busy.add(new FreeBusyCalculator.TimeSlot(3000, 4000));

        List<FreeBusyCalculator.TimeSlot> free = FreeBusyCalculator.computeFreeIntervals(busy, 0, 5000, 500);
        assertEquals(3, free.size());
        assertEquals(0, free.get(0).getStart());
        assertEquals(1000, free.get(0).getEnd());

        assertEquals(2000, free.get(1).getStart());
        assertEquals(3000, free.get(1).getEnd());

        assertEquals(4000, free.get(2).getStart());
        assertEquals(5000, free.get(2).getEnd());
    }
}
