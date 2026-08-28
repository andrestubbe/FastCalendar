package fastcalendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RFC 5545 VFREEBUSY component representation.
 */
public final class VFreeBusy {

    public static final class Period {
        private final long start;
        private final long end;
        private final String fbType; // BUSY, BUSY-UNAVAILABLE, BUSY-TENTATIVE

        public Period(long start, long end, String fbType) {
            this.start = start;
            this.end = end;
            this.fbType = fbType != null ? fbType : "BUSY";
        }

        public long getStart() { return start; }
        public long getEnd() { return end; }
        public String getFbType() { return fbType; }

        @Override
        public String toString() {
            return FastDateTime.formatUtc(start) + "/" + FastDateTime.formatUtc(end);
        }
    }

    private final String uid;
    private final long dtStart;
    private final long dtEnd;
    private final Organizer organizer;
    private final List<Attendee> attendees;
    private final List<Period> periods;

    public VFreeBusy(String uid, long dtStart, long dtEnd, Organizer organizer,
                     List<Attendee> attendees, List<Period> periods) {
        this.uid = uid != null ? uid : java.util.UUID.randomUUID().toString();
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.organizer = organizer;
        this.attendees = attendees != null ? Collections.unmodifiableList(new ArrayList<>(attendees)) : Collections.emptyList();
        this.periods = periods != null ? Collections.unmodifiableList(new ArrayList<>(periods)) : Collections.emptyList();
    }

    public String getUid() { return uid; }
    public long getDtStart() { return dtStart; }
    public long getDtEnd() { return dtEnd; }
    public Organizer getOrganizer() { return organizer; }
    public List<Attendee> getAttendees() { return attendees; }
    public List<Period> getPeriods() { return periods; }

    @Override
    public String toString() {
        return "VFreeBusy[periods=" + periods.size() + "]";
    }
}
