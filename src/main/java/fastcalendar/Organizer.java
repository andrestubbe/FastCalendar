package fastcalendar;

import java.util.Objects;

/**
 * RFC 5545 ORGANIZER property representation.
 */
public final class Organizer {
    private final String calAddress; // e.g. "mailto:boss@example.com"
    private final String commonName; // CN parameter
    private final String sentBy;     // SENT-BY parameter

    public Organizer(String calAddress, String commonName, String sentBy) {
        this.calAddress = Objects.requireNonNull(calAddress, "calAddress");
        this.commonName = commonName;
        this.sentBy = sentBy;
    }

    public static Organizer of(String calAddress) {
        return new Organizer(calAddress, null, null);
    }

    public static Organizer of(String calAddress, String commonName) {
        return new Organizer(calAddress, commonName, null);
    }

    public String getCalAddress() { return calAddress; }
    public String getCommonName() { return commonName; }
    public String getSentBy() { return sentBy; }

    @Override
    public String toString() {
        return "Organizer[" + calAddress + (commonName != null ? " (" + commonName + ")" : "") + "]";
    }
}
