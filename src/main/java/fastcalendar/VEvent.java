package fastcalendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RFC 5545 VEVENT component representation.
 * Represents a calendar event with high-speed field access and recurrence rules.
 */
public final class VEvent {

    public enum Status {
        TENTATIVE,
        CONFIRMED,
        CANCELLED
    }

    public enum Transp {
        OPAQUE,      // Busy time
        TRANSPARENT  // Free time
    }

    private final String uid;
    private final String summary;
    private final String description;
    private final String location;
    private final long dtStart;
    private final long dtEnd;
    private final long durationMillis;
    private final boolean allDay;
    private final String tzid;
    private final RRule rrule;
    private final long[] exDates;
    private final long[] rDates;
    private final Status status;
    private final Transp transp;
    private final Organizer organizer;
    private final List<Attendee> attendees;
    private final List<VAlarm> alarms;
    private final List<String> categories;
    private final long created;
    private final long lastModified;
    private final int sequence;
    private final String url;
    private final Map<String, String> customProperties;

    private VEvent(Builder b) {
        this.uid = b.uid != null ? b.uid : java.util.UUID.randomUUID().toString();
        this.summary = b.summary != null ? b.summary : "";
        this.description = b.description;
        this.location = b.location;
        this.dtStart = b.dtStart;
        this.allDay = b.allDay;
        this.tzid = b.tzid;
        this.rrule = b.rrule;
        this.exDates = b.exDates;
        this.rDates = b.rDates;
        this.status = b.status != null ? b.status : Status.CONFIRMED;
        this.transp = b.transp != null ? b.transp : Transp.OPAQUE;
        this.organizer = b.organizer;
        this.attendees = b.attendees != null ? Collections.unmodifiableList(new ArrayList<>(b.attendees)) : Collections.emptyList();
        this.alarms = b.alarms != null ? Collections.unmodifiableList(new ArrayList<>(b.alarms)) : Collections.emptyList();
        this.categories = b.categories != null ? Collections.unmodifiableList(new ArrayList<>(b.categories)) : Collections.emptyList();
        this.created = b.created;
        this.lastModified = b.lastModified;
        this.sequence = b.sequence;
        this.url = b.url;
        this.customProperties = b.customProperties != null ? Collections.unmodifiableMap(new HashMap<>(b.customProperties)) : Collections.emptyMap();

        if (b.dtEnd > 0) {
            this.dtEnd = b.dtEnd;
            this.durationMillis = b.dtEnd - b.dtStart;
        } else if (b.durationMillis > 0) {
            this.durationMillis = b.durationMillis;
            this.dtEnd = b.dtStart + b.durationMillis;
        } else {
            // Default 1 hour duration if not allDay, else 1 day
            this.durationMillis = b.allDay ? FastDateTime.MILLIS_PER_DAY : FastDateTime.MILLIS_PER_HOUR;
            this.dtEnd = b.dtStart + this.durationMillis;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUid() { return uid; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public long getDtStart() { return dtStart; }
    public long getDtEnd() { return dtEnd; }
    public long getDurationMillis() { return durationMillis; }
    public boolean isAllDay() { return allDay; }
    public String getTzid() { return tzid; }
    public RRule getRrule() { return rrule; }
    public boolean isRecurring() { return rrule != null || (rDates != null && rDates.length > 0); }
    public long[] getExDates() { return exDates; }
    public long[] getRDates() { return rDates; }
    public Status getStatus() { return status; }
    public Transp getTransp() { return transp; }
    public Organizer getOrganizer() { return organizer; }
    public List<Attendee> getAttendees() { return attendees; }
    public List<VAlarm> getAlarms() { return alarms; }
    public List<String> getCategories() { return categories; }
    public long getCreated() { return created; }
    public long getLastModified() { return lastModified; }
    public int getSequence() { return sequence; }
    public String getUrl() { return url; }
    public Map<String, String> getCustomProperties() { return customProperties; }

    /**
     * Expands all occurrences of this event within the specified time range [windowStart, windowEnd].
     */
    public long[] expandOccurrences(long windowStart, long windowEnd) {
        return RRuleEvaluator.expand(dtStart, rrule, exDates, rDates, windowStart, windowEnd, 10_000);
    }

    /**
     * Checks if this event is busy (opaque) and overlaps with the given time window.
     */
    public boolean overlaps(long rangeStart, long rangeEnd) {
        if (transp != Transp.OPAQUE || status == Status.CANCELLED) return false;
        if (!isRecurring()) {
            return dtStart < rangeEnd && dtEnd > rangeStart;
        }
        // Recurring event check
        long[] occs = expandOccurrences(rangeStart - durationMillis, rangeEnd);
        for (long occStart : occs) {
            long occEnd = occStart + durationMillis;
            if (occStart < rangeEnd && occEnd > rangeStart) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "VEvent[uid=" + uid + ", summary='" + summary + "', start=" + FastDateTime.formatUtc(dtStart) +
               ", end=" + FastDateTime.formatUtc(dtEnd) + (rrule != null ? ", rrule=" + rrule : "") + "]";
    }

    public static final class Builder {
        private String uid;
        private String summary;
        private String description;
        private String location;
        private long dtStart = -1L;
        private long dtEnd = -1L;
        private long durationMillis = -1L;
        private boolean allDay = false;
        private String tzid;
        private RRule rrule;
        private long[] exDates;
        private long[] rDates;
        private Status status = Status.CONFIRMED;
        private Transp transp = Transp.OPAQUE;
        private Organizer organizer;
        private List<Attendee> attendees;
        private List<VAlarm> alarms;
        private List<String> categories;
        private long created = -1L;
        private long lastModified = -1L;
        private int sequence = 0;
        private String url;
        private Map<String, String> customProperties;

        public Builder uid(String uid) { this.uid = uid; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder dtStart(long dtStart) { this.dtStart = dtStart; return this; }
        public Builder dtEnd(long dtEnd) { this.dtEnd = dtEnd; return this; }
        public Builder durationMillis(long durationMillis) { this.durationMillis = durationMillis; return this; }
        public Builder allDay(boolean allDay) { this.allDay = allDay; return this; }
        public Builder tzid(String tzid) { this.tzid = tzid; return this; }
        public Builder rrule(RRule rrule) { this.rrule = rrule; return this; }
        public Builder rrule(String rruleText) { this.rrule = RRule.parse(rruleText); return this; }
        public Builder exDates(long... exDates) { this.exDates = exDates; return this; }
        public Builder rDates(long... rDates) { this.rDates = rDates; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder transp(Transp transp) { this.transp = transp; return this; }
        public Builder organizer(Organizer organizer) { this.organizer = organizer; return this; }
        public Builder organizer(String email, String commonName) { this.organizer = Organizer.of(email, commonName); return this; }
        
        public Builder addAttendee(Attendee attendee) {
            if (this.attendees == null) this.attendees = new ArrayList<>(4);
            this.attendees.add(attendee);
            return this;
        }

        public Builder addAttendee(String email, String commonName) {
            return addAttendee(Attendee.of(email, commonName));
        }

        public Builder addAlarm(VAlarm alarm) {
            if (this.alarms == null) this.alarms = new ArrayList<>(2);
            this.alarms.add(alarm);
            return this;
        }

        public Builder addCategory(String category) {
            if (this.categories == null) this.categories = new ArrayList<>(2);
            this.categories.add(category);
            return this;
        }

        public Builder created(long created) { this.created = created; return this; }
        public Builder lastModified(long lastModified) { this.lastModified = lastModified; return this; }
        public Builder sequence(int sequence) { this.sequence = sequence; return this; }
        public Builder url(String url) { this.url = url; return this; }

        public Builder customProperty(String key, String value) {
            if (this.customProperties == null) this.customProperties = new HashMap<>(4);
            this.customProperties.put(key, value);
            return this;
        }

        public VEvent build() {
            if (dtStart <= 0) {
                dtStart = System.currentTimeMillis();
            }
            return new VEvent(this);
        }
    }
}
