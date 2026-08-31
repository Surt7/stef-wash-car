package fr.stefwashcar.service.googleapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Injection Spring désactivée avec l'intégration Google Calendar.
 * Décommenter @Service (et son import) lors de la réactivation.
 */
// @Service
public class GoogleCalendarService {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String calendarId;

    public GoogleCalendarService(
            RestClient.Builder restClientBuilder,
            @Value("${app.google.client-id:}") String clientId,
            @Value("${app.google.client-secret:}") String clientSecret,
            @Value("${app.google.refresh-token:}") String refreshToken,
            @Value("${app.google.calendar-id:}") String calendarId
    ) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.calendarId = calendarId;
    }

    private String getAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        try {
            Map<String, Object> data = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Object token = data != null ? data.get("access_token") : null;

            if (token == null || token.toString().isBlank()) {
                throw new IllegalStateException(
                        "Impossible de récupérer le access token Google."
                );
            }

            return token.toString();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Google token error: " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    public List<Map<String, Object>> getFutureEvents() {
        return getFutureEvents(null);
    }

    public List<Map<String, Object>> getFutureEvents(String search) {
        String accessToken = getAccessToken();

        try {
            Map<String, Object> data = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .pathSegment(
                                    "calendar",
                                    "v3",
                                    "calendars",
                                    calendarId,
                                    "events"
                            )
                            .queryParam("singleEvents", true)
                            .queryParam("orderBy", "startTime")
                            .queryParam(
                                    "timeMin",
                                    Instant.now()
                                            .truncatedTo(ChronoUnit.SECONDS)
                                            .toString()
                            )
                            .queryParam("maxResults", 2500)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Object itemsValue = data != null ? data.get("items") : null;

            if (!(itemsValue instanceof List<?> items)) {
                return List.of();
            }

            List<Map<String, Object>> events = items.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> event =
                                (Map<String, Object>) item;
                        return event;
                    })
                    .toList();

            if (search == null || search.isBlank()) {
                return events;
            }

            String needle = search.toLowerCase(Locale.ROOT);

            return events.stream()
                    .filter(event -> {
                        Object summary = event.get("summary");
                        return summary != null
                                && summary.toString()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(needle);
                    })
                    .toList();

        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Google calendar error: " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }
}
