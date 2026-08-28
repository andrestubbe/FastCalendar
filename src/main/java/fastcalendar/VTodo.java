package fastcalendar;

import java.util.Objects;

/**
 * RFC 5545 VTODO component representation.
 */
public final class VTodo {
    public enum Status {
        NEEDS_ACTION,
        IN_PROCESS,
        COMPLETED,
        CANCELLED
    }

    private final String uid;
    private final String summary;
    private final String description;
    private final long due;
    private final long completed;
    private final Status status;
    private final int priority; // 0..9

    public VTodo(String uid, String summary, String description, long due, long completed, Status status, int priority) {
        this.uid = uid != null ? uid : java.util.UUID.randomUUID().toString();
        this.summary = summary != null ? summary : "";
        this.description = description;
        this.due = due;
        this.completed = completed;
        this.status = status != null ? status : Status.NEEDS_ACTION;
        this.priority = priority;
    }

    public String getUid() { return uid; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public long getDue() { return due; }
    public long getCompleted() { return completed; }
    public Status getStatus() { return status; }
    public int getPriority() { return priority; }

    @Override
    public String toString() {
        return "VTodo[uid=" + uid + ", summary='" + summary + "', status=" + status + "]";
    }
}
