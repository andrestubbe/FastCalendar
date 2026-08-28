package fastcalendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight, zero-dependency CalDAV (RFC 4791) and WebDAV (RFC 4918) XML builder and parser.
 */
public final class CalDavXml {

    private CalDavXml() {}

    /**
     * CalDAV response resource container representing a calendar object.
     */
    public static final class ResourceResponse {
        private final String href;
        private final String etag;
        private final String status;
        private final String icsData;

        public ResourceResponse(String href, String etag, String status, String icsData) {
            this.href = href;
            this.etag = cleanEtag(etag);
            this.status = status;
            this.icsData = icsData;
        }

        private static String cleanEtag(String raw) {
            if (raw == null) return null;
            raw = raw.trim();
            if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                return raw.substring(1, raw.length() - 1);
            }
            return raw;
        }

        public String getHref() { return href; }
        public String getEtag() { return etag; }
        public String getStatus() { return status; }
        public String getIcsData() { return icsData; }

        public boolean isSuccess() {
            return status != null && (status.contains("200") || status.contains("201"));
        }

        public VCalendar parseCalendar() {
            return icsData != null ? IcsParser.parse(icsData) : null;
        }

        @Override
        public String toString() {
            return "ResourceResponse[href=" + href + ", etag=" + etag + "]";
        }
    }

    /**
     * Generates a CalDAV `calendar-query` REPORT XML payload with a time-range filter.
     */
    public static String buildCalendarQueryXml(long startMillis, long endMillis) {
        String startStr = FastDateTime.formatUtc(startMillis);
        String endStr = FastDateTime.formatUtc(endMillis);

        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
               "<C:calendar-query xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
               "  <D:prop>\n" +
               "    <D:getetag/>\n" +
               "    <C:calendar-data/>\n" +
               "  </D:prop>\n" +
               "  <C:filter>\n" +
               "    <C:comp-filter name=\"VCALENDAR\">\n" +
               "      <C:comp-filter name=\"VEVENT\">\n" +
               "        <C:time-range start=\"" + startStr + "\" end=\"" + endStr + "\"/>\n" +
               "      </C:comp-filter>\n" +
               "    </C:comp-filter>\n" +
               "  </C:filter>\n" +
               "</C:calendar-query>";
    }

    /**
     * Generates a CalDAV `calendar-multiget` REPORT XML payload for batch fetching.
     */
    public static String buildCalendarMultigetXml(Iterable<String> hrefs) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
          .append("<C:calendar-multiget xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n")
          .append("  <D:prop>\n")
          .append("    <D:getetag/>\n")
          .append("    <C:calendar-data/>\n")
          .append("  </D:prop>\n");
        if (hrefs != null) {
            for (String href : hrefs) {
                sb.append("  <D:href>").append(href).append("</D:href>\n");
            }
        }
        sb.append("</C:calendar-multiget>");
        return sb.toString();
    }

    /**
     * Generates a WebDAV `sync-collection` REPORT XML payload for incremental delta sync (RFC 6578).
     */
    public static String buildSyncCollectionXml(String syncToken, int limit) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
          .append("<D:sync-collection xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n")
          .append("  <D:sync-token>").append(syncToken != null ? syncToken : "").append("</D:sync-token>\n")
          .append("  <D:sync-level>1</D:sync-level>\n");
        if (limit > 0) {
            sb.append("  <D:limit><D:nresults>").append(limit).append("</D:nresults></D:limit>\n");
        }
        sb.append("  <D:prop>\n")
          .append("    <D:getetag/>\n")
          .append("    <C:calendar-data/>\n")
          .append("  </D:prop>\n")
          .append("</D:sync-collection>");
        return sb.toString();
    }

    /**
     * Generates a CalDAV `free-busy-query` REPORT XML payload.
     */
    public static String buildFreeBusyQueryXml(long startMillis, long endMillis) {
        String startStr = FastDateTime.formatUtc(startMillis);
        String endStr = FastDateTime.formatUtc(endMillis);

        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
               "<C:free-busy-query xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
               "  <C:time-range start=\"" + startStr + "\" end=\"" + endStr + "\"/>\n" +
               "</C:free-busy-query>";
    }

    /**
     * Parses a WebDAV / CalDAV `multistatus` XML response into ResourceResponse items.
     */
    public static List<ResourceResponse> parseMultistatus(String xml) {
        if (xml == null || xml.isEmpty()) return Collections.emptyList();

        List<ResourceResponse> responses = new ArrayList<>();
        int searchPos = 0;

        while (true) {
            int respStart = indexOfIgnoreCase(xml, "<response", searchPos);
            if (respStart < 0) {
                respStart = indexOfIgnoreCase(xml, "<d:response", searchPos);
            }
            if (respStart < 0) break;

            int respEnd = indexOfIgnoreCase(xml, "</response>", respStart);
            if (respEnd < 0) {
                respEnd = indexOfIgnoreCase(xml, "</d:response>", respStart);
            }
            if (respEnd < 0) break;

            String respChunk = xml.substring(respStart, respEnd);
            searchPos = respEnd + 11;

            String href = extractTagContent(respChunk, "href");
            String etag = extractTagContent(respChunk, "getetag");
            String status = extractTagContent(respChunk, "status");
            String ics = extractTagContent(respChunk, "calendar-data");

            responses.add(new ResourceResponse(href, etag, status, ics));
        }

        return responses;
    }

    /**
     * Extracts sync-token from a multistatus sync response.
     */
    public static String extractSyncToken(String xml) {
        if (xml == null) return null;
        return extractTagContent(xml, "sync-token");
    }

    private static String extractTagContent(String xml, String tagName) {
        String openTag1 = "<" + tagName + ">";
        String openTag2 = "<d:" + tagName + ">";
        String openTag3 = "<c:" + tagName + ">";
        String openTag4 = "<cal:" + tagName + ">";

        int start = indexOfIgnoreCase(xml, openTag1, 0);
        int tagLen = openTag1.length();
        if (start < 0) {
            start = indexOfIgnoreCase(xml, openTag2, 0);
            tagLen = openTag2.length();
        }
        if (start < 0) {
            start = indexOfIgnoreCase(xml, openTag3, 0);
            tagLen = openTag3.length();
        }
        if (start < 0) {
            start = indexOfIgnoreCase(xml, openTag4, 0);
            tagLen = openTag4.length();
        }
        if (start < 0) {
            // Check for tags with attributes e.g. <cal:calendar-data xmlns="...">
            int prefixPos = indexOfIgnoreCase(xml, "<" + tagName + " ", 0);
            if (prefixPos < 0) prefixPos = indexOfIgnoreCase(xml, "<d:" + tagName + " ", 0);
            if (prefixPos < 0) prefixPos = indexOfIgnoreCase(xml, "<c:" + tagName + " ", 0);
            if (prefixPos < 0) prefixPos = indexOfIgnoreCase(xml, "<cal:" + tagName + " ", 0);

            if (prefixPos >= 0) {
                int closingBracket = xml.indexOf('>', prefixPos);
                if (closingBracket > 0) {
                    start = prefixPos;
                    tagLen = (closingBracket - prefixPos) + 1;
                }
            }
        }

        if (start < 0) return null;

        int contentStart = start + tagLen;
        String closeTag1 = "</" + tagName + ">";
        String closeTag2 = "</d:" + tagName + ">";
        String closeTag3 = "</c:" + tagName + ">";
        String closeTag4 = "</cal:" + tagName + ">";

        int end = indexOfIgnoreCase(xml, closeTag1, contentStart);
        if (end < 0) end = indexOfIgnoreCase(xml, closeTag2, contentStart);
        if (end < 0) end = indexOfIgnoreCase(xml, closeTag3, contentStart);
        if (end < 0) end = indexOfIgnoreCase(xml, closeTag4, contentStart);

        if (end < 0) return null;

        String content = xml.substring(contentStart, end).trim();
        // Check CDATA
        if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
            content = content.substring(9, content.length() - 3);
        }
        return content;
    }

    private static int indexOfIgnoreCase(String src, String target, int fromIndex) {
        if (src == null || target == null) return -1;
        int targetLen = target.length();
        int max = src.length() - targetLen;

        for (int i = fromIndex; i <= max; i++) {
            if (src.regionMatches(true, i, target, 0, targetLen)) {
                return i;
            }
        }
        return -1;
    }
}
