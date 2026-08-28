package fastcalendar;

/**
 * RFC 5545 VALARM component representation.
 */
public final class VAlarm {
    public enum Action {
        AUDIO,
        DISPLAY,
        EMAIL
    }

    private final Action action;
    private final long triggerSeconds; // e.g. -900 for 15 minutes before
    private final String description;
    private final String summary;

    public VAlarm(Action action, long triggerSeconds, String description, String summary) {
        this.action = action != null ? action : Action.DISPLAY;
        this.triggerSeconds = triggerSeconds;
        this.description = description;
        this.summary = summary;
    }

    public static VAlarm display(long triggerSecondsBefore, String message) {
        return new VAlarm(Action.DISPLAY, triggerSecondsBefore, message, null);
    }

    public static VAlarm audio(long triggerSecondsBefore) {
        return new VAlarm(Action.AUDIO, triggerSecondsBefore, null, null);
    }

    public Action getAction() { return action; }
    public long getTriggerSeconds() { return triggerSeconds; }
    public String getDescription() { return description; }
    public String getSummary() { return summary; }

    @Override
    public String toString() {
        return "VAlarm[" + action + ", trigger=" + triggerSeconds + "s, desc=" + description + "]";
    }
}
