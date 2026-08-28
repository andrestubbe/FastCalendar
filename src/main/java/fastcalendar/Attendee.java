package fastcalendar;

import java.util.Objects;

/**
 * RFC 5545 ATTENDEE property representation.
 */
public final class Attendee {
    private final String calAddress; // e.g. "mailto:user@example.com"
    private final String commonName; // CN parameter
    private final String role;       // ROLE parameter (REQ-PARTICIPANT, OPT-PARTICIPANT, CHAIR)
    private final String partStat;   // PARTSTAT (NEEDS-ACTION, ACCEPTED, DECLINED, TENTATIVE)
    private final String cuType;     // CUTYPE (INDIVIDUAL, GROUP, RESOURCE, ROOM)
    private final boolean rsvp;      // RSVP parameter

    public Attendee(String calAddress, String commonName, String role, String partStat, String cuType, boolean rsvp) {
        this.calAddress = Objects.requireNonNull(calAddress, "calAddress");
        this.commonName = commonName;
        this.role = role != null ? role : "REQ-PARTICIPANT";
        this.partStat = partStat != null ? partStat : "NEEDS-ACTION";
        this.cuType = cuType != null ? cuType : "INDIVIDUAL";
        this.rsvp = rsvp;
    }

    public static Attendee of(String calAddress) {
        return new Attendee(calAddress, null, null, null, null, false);
    }

    public static Attendee of(String calAddress, String commonName) {
        return new Attendee(calAddress, commonName, null, null, null, false);
    }

    public String getCalAddress() { return calAddress; }
    public String getCommonName() { return commonName; }
    public String getRole() { return role; }
    public String getPartStat() { return partStat; }
    public String getCuType() { return cuType; }
    public boolean isRsvp() { return rsvp; }

    @Override
    public String toString() {
        return "Attendee[" + calAddress + (commonName != null ? " (" + commonName + ")" : "") + "]";
    }
}
