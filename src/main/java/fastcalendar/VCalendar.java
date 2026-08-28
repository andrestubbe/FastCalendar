package fastcalendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 5545 VCALENDAR top-level calendar container.
 * Holds events, todos, journals, freebusy components, and calendar-level properties.
 */
public final class VCalendar {

    private final String prodId;
    private final String version;
    private final String calScale;
    private final String method;
    private final String name; // X-WR-CALNAME
    private final String description; // X-WR-CALDESC
    private final String timezone; // X-WR-TIMEZONE
    private final List<VEvent> events;
    private final List<VTodo> todos;
    private final List<VJournal> journals;
    private final List<VFreeBusy> freeBusyList;
    private final Map<String, String> customProperties;

    private VCalendar(Builder b) {
        this.prodId = b.prodId != null ? b.prodId : "-//FastJava//FastCalendar 0.1.0//EN";
        this.version = b.version != null ? b.version : "2.0";
        this.calScale = b.calScale != null ? b.calScale : "GREGORIAN";
        this.method = b.method;
        this.name = b.name;
        this.description = b.description;
        this.timezone = b.timezone;
        this.events = b.events != null ? Collections.unmodifiableList(new ArrayList<>(b.events)) : Collections.emptyList();
        this.todos = b.todos != null ? Collections.unmodifiableList(new ArrayList<>(b.todos)) : Collections.emptyList();
        this.journals = b.journals != null ? Collections.unmodifiableList(new ArrayList<>(b.journals)) : Collections.emptyList();
        this.freeBusyList = b.freeBusyList != null ? Collections.unmodifiableList(new ArrayList<>(b.freeBusyList)) : Collections.emptyList();
        this.customProperties = b.customProperties != null ? Collections.unmodifiableMap(new HashMap<>(b.customProperties)) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProdId() { return prodId; }
    public String getVersion() { return version; }
    public String getCalScale() { return calScale; }
    public String getMethod() { return method; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTimezone() { return timezone; }
    public List<VEvent> getEvents() { return events; }
    public List<VTodo> getTodos() { return todos; }
    public List<VJournal> getJournals() { return journals; }
    public List<VFreeBusy> getFreeBusyList() { return freeBusyList; }
    public Map<String, String> getCustomProperties() { return customProperties; }

    /**
     * Builds a fast query index from this calendar's events.
     */
    public CalendarIndex createIndex() {
        CalendarIndex index = new CalendarIndex();
        index.addAll(events);
        return index;
    }

    /**
     * Serializes this calendar into an RFC 5545 formatted .ics string.
     */
    public String toIcsString() {
        return IcsWriter.writeToString(this);
    }

    @Override
    public String toString() {
        return "VCalendar[name='" + name + "', events=" + events.size() + ", todos=" + todos.size() + "]";
    }

    public static final class Builder {
        private String prodId;
        private String version = "2.0";
        private String calScale = "GREGORIAN";
        private String method;
        private String name;
        private String description;
        private String timezone;
        private List<VEvent> events;
        private List<VTodo> todos;
        private List<VJournal> journals;
        private List<VFreeBusy> freeBusyList;
        private Map<String, String> customProperties;

        public Builder prodId(String prodId) { this.prodId = prodId; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder calScale(String calScale) { this.calScale = calScale; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }

        public Builder addEvent(VEvent event) {
            if (this.events == null) this.events = new ArrayList<>();
            this.events.add(event);
            return this;
        }

        public Builder addEvents(Iterable<VEvent> events) {
            if (events != null) {
                for (VEvent e : events) addEvent(e);
            }
            return this;
        }

        public Builder addTodo(VTodo todo) {
            if (this.todos == null) this.todos = new ArrayList<>();
            this.todos.add(todo);
            return this;
        }

        public Builder addJournal(VJournal journal) {
            if (this.journals == null) this.journals = new ArrayList<>();
            this.journals.add(journal);
            return this;
        }

        public Builder addFreeBusy(VFreeBusy freeBusy) {
            if (this.freeBusyList == null) this.freeBusyList = new ArrayList<>();
            this.freeBusyList.add(freeBusy);
            return this;
        }

        public Builder customProperty(String key, String value) {
            if (this.customProperties == null) this.customProperties = new HashMap<>();
            this.customProperties.put(key, value);
            return this;
        }

        public VCalendar build() {
            return new VCalendar(this);
        }
    }
}
