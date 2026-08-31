package fr.stefwashcar.service.graphmailer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/*
 * Injection Spring désactivée avec l'intégration Microsoft Graph Mailer.
 * Décommenter @Service (et son import) lors de la réactivation.
 */
// @Service
public class GraphMailer {
    private final RestClient restClient;
    private final GraphTokenProvider tokenProvider;
    private final String defaultFromUser;

    public GraphMailer(RestClient.Builder builder,
                       GraphTokenProvider tokenProvider,
                       @Value("${app.graph.default-from-user:}") String defaultFromUser) {
        this.restClient = builder.build();
        this.tokenProvider = tokenProvider;
        this.defaultFromUser = defaultFromUser;
    }

    public void sendMail(String fromUser, String to, String subject,
                         String htmlBody, boolean saveToSentItems) {
        String token = tokenProvider.getAccessToken();
        String from = fromUser != null && !fromUser.isBlank() ? fromUser : defaultFromUser;

        if (from == null || from.isBlank()) {
            throw new IllegalStateException("Graph sender user is not configured.");
        }

        Map<String,Object> payload = Map.of(
                "message", Map.of(
                        "subject", subject,
                        "body", Map.of("contentType", "HTML", "content", htmlBody),
                        "toRecipients", List.of(
                                Map.of("emailAddress", Map.of("address", to))
                        )
                ),
                "saveToSentItems", saveToSentItems
        );

        restClient.post()
                .uri("https://graph.microsoft.com/v1.0/users/{from}/sendMail", from)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public void sendMail(String fromUser, String to, String subject, String htmlBody) {
        sendMail(fromUser, to, subject, htmlBody, true);
    }
}
