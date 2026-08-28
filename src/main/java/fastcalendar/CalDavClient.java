package fastcalendar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * High-Speed, Asynchronous & Synchronous CalDAV (RFC 4791) client.
 * Built on Java 11+ java.net.http.HttpClient with zero external dependencies.
 */
public final class CalDavClient {

    public static final class SyncResult {
        private final String nextSyncToken;
        private final List<CalDavXml.ResourceResponse> updatedResources;
        private final List<String> deletedHrefs;

        public SyncResult(String nextSyncToken, List<CalDavXml.ResourceResponse> updatedResources, List<String> deletedHrefs) {
            this.nextSyncToken = nextSyncToken;
            this.updatedResources = updatedResources != null ? updatedResources : Collections.emptyList();
            this.deletedHrefs = deletedHrefs != null ? deletedHrefs : Collections.emptyList();
        }

        public String getNextSyncToken() { return nextSyncToken; }
        public List<CalDavXml.ResourceResponse> getUpdatedResources() { return updatedResources; }
        public List<String> getDeletedHrefs() { return deletedHrefs; }
    }

    private final HttpClient httpClient;
    private final String authHeader;
    private final Duration timeout;

    public CalDavClient(HttpClient httpClient, String authHeader, Duration timeout) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.authHeader = authHeader;
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Queries calendar events within a specified time window using CalDAV REPORT.
     */
    public List<VEvent> queryEvents(String calendarUrl, long startMillis, long endMillis) throws IOException, InterruptedException {
        String xmlPayload = CalDavXml.buildCalendarQueryXml(startMillis, endMillis);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(calendarUrl))
            .header("Content-Type", "application/xml; charset=utf-8")
            .header("Depth", "1")
            .timeout(timeout)
            .method("REPORT", HttpRequest.BodyPublishers.ofString(xmlPayload, StandardCharsets.UTF_8));

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 207 && response.statusCode() != 200) {
            throw new IOException("CalDAV query failed with HTTP status " + response.statusCode() + ": " + response.body());
        }

        List<CalDavXml.ResourceResponse> resources = CalDavXml.parseMultistatus(response.body());
        List<VEvent> events = new ArrayList<>();
        for (CalDavXml.ResourceResponse r : resources) {
            if (r.getIcsData() != null && !r.getIcsData().isEmpty()) {
                VCalendar cal = r.parseCalendar();
                if (cal != null) {
                    events.addAll(cal.getEvents());
                }
            }
        }
        return events;
    }

    /**
     * Synchronizes calendar incrementally via WebDAV sync-collection (RFC 6578).
     */
    public SyncResult syncEvents(String calendarUrl, String syncToken) throws IOException, InterruptedException {
        String xmlPayload = CalDavXml.buildSyncCollectionXml(syncToken, 500);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(calendarUrl))
            .header("Content-Type", "application/xml; charset=utf-8")
            .header("Depth", "1")
            .timeout(timeout)
            .method("REPORT", HttpRequest.BodyPublishers.ofString(xmlPayload, StandardCharsets.UTF_8));

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 207 && response.statusCode() != 200) {
            throw new IOException("CalDAV sync failed with HTTP status " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();
        String nextSyncToken = CalDavXml.extractSyncToken(body);
        List<CalDavXml.ResourceResponse> resources = CalDavXml.parseMultistatus(body);

        List<CalDavXml.ResourceResponse> updated = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        for (CalDavXml.ResourceResponse res : resources) {
            if (res.getStatus() != null && res.getStatus().contains("404")) {
                deleted.add(res.getHref());
            } else if (res.isSuccess()) {
                updated.add(res);
            }
        }

        return new SyncResult(nextSyncToken, updated, deleted);
    }

    /**
     * Uploads/creates an event on the CalDAV server via HTTP PUT.
     */
    public boolean putEvent(String eventUrl, VEvent event, String ifMatchEtag) throws IOException, InterruptedException {
        VCalendar singleCal = VCalendar.builder().addEvent(event).build();
        String icsContent = singleCal.toIcsString();

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(eventUrl))
            .header("Content-Type", "text/calendar; charset=utf-8")
            .timeout(timeout)
            .PUT(HttpRequest.BodyPublishers.ofString(icsContent, StandardCharsets.UTF_8));

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }
        if (ifMatchEtag != null) {
            reqBuilder.header("If-Match", "\"" + ifMatchEtag + "\"");
        }

        HttpResponse<Void> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
        int status = response.statusCode();
        return status == 200 || status == 201 || status == 204;
    }

    /**
     * Deletes an event on the CalDAV server via HTTP DELETE.
     */
    public boolean deleteEvent(String eventUrl, String ifMatchEtag) throws IOException, InterruptedException {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(eventUrl))
            .timeout(timeout)
            .DELETE();

        if (authHeader != null) {
            reqBuilder.header("Authorization", authHeader);
        }
        if (ifMatchEtag != null) {
            reqBuilder.header("If-Match", "\"" + ifMatchEtag + "\"");
        }

        HttpResponse<Void> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
        int status = response.statusCode();
        return status == 200 || status == 204;
    }

    public static final class Builder {
        private HttpClient httpClient;
        private String authHeader;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder basicAuth(String username, String password) {
            String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            this.authHeader = "Basic " + token;
            return this;
        }

        public Builder bearerAuth(String token) {
            this.authHeader = "Bearer " + token;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public CalDavClient build() {
            return new CalDavClient(httpClient, authHeader, timeout);
        }
    }
}
