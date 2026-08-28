package fastcalendar;

/**
 * RFC 5545 VJOURNAL component representation.
 */
public final class VJournal {
    private final String uid;
    private final String summary;
    private final String description;
    private final long dtStart;

    public VJournal(String uid, String summary, String description, long dtStart) {
        this.uid = uid != null ? uid : java.util.UUID.randomUUID().toString();
        this.summary = summary != null ? summary : "";
        this.description = description;
        this.dtStart = dtStart;
    }

    public String getUid() { return uid; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public long getDtStart() { return dtStart; }

    @Override
    public String toString() {
        return "VJournal[uid=" + uid + ", summary='" + summary + "']";
    }
}
