package fr.stefwashcar.service.booking;

import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.EmailLog;
import fr.stefwashcar.model.Shop;
import fr.stefwashcar.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppointmentConfirmationMailer {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRANCE);

    private final JavaMailSender mailer;
    private final EmailLogRepository emailLogs;
    private final String publicBaseUrl;
    private final String mailFrom;
    private final boolean enabled;

    public AppointmentConfirmationMailer(
            ObjectProvider<JavaMailSender> mailerProvider,
            EmailLogRepository emailLogs,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.mail.from:booking@lesphotosdemai.fr}") String mailFrom,
            @Value("${app.mail.enabled:true}") boolean enabled) {
        this.mailer = mailerProvider.getIfAvailable();
        this.emailLogs = emailLogs;
        this.publicBaseUrl = stripSlash(publicBaseUrl);
        this.mailFrom = mailFrom;
        this.enabled = enabled;
    }

    public void send(Appointment appointment) {
        if (!enabled) {
            return;
        }

        var user = appointment.getUser();
        var service = appointment.getService();

        ZoneId zone;
        try {
            zone = ZoneId.of(service.getTimezone() != null ? service.getTimezone() : "Europe/Paris");
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        String formattedStart = appointment.getStartAtUtc().atZone(zone).format(DATE_FORMAT);
        String cancelUrl = publicBaseUrl + "/appointments/cancel/"
                + URLEncoder.encode(appointment.getCancelToken(), StandardCharsets.UTF_8)
                + "/confirm";

        String firstName = value(user.getFirstname());
        String lastName = value(user.getLastname());
        String serviceName = value(service.getName());
        int durationMin = service.getDurationMin() != null ? service.getDurationMin().intValue() : 0;

        AddressResult address = resolveAddressForAppointment(appointment);

        String textBody = """
                Bonjour %s %s,

                Votre rendez-vous a bien été enregistré pour le %s.

                Service : %s
                Durée : %d minutes
                Adresse :
                %s

                Pour annuler votre rendez-vous :
                %s

                À bientôt,
                Les Photos de Mai
                """.formatted(firstName, lastName, formattedStart, serviceName,
                durationMin, address.text(), cancelUrl);

        String htmlBody = """
                <p>Bonjour %s %s,</p>
                <p>Votre rendez-vous a bien été enregistré pour le <strong>%s</strong>.</p>
                <ul>
                  <li><strong>Service :</strong> %s</li>
                  <li><strong>Durée :</strong> %d minutes</li>
                  <li><strong>Adresse :</strong><br>%s</li>
                </ul>
                <p>Pour annuler votre rendez-vous, cliquez sur le bouton ci-dessous :</p>
                <p style="margin:16px 0;">
                  <a href="%s" style="display:inline-block;padding:10px 18px;background-color:#0078d4;color:#fff;text-decoration:none;border-radius:4px;font-weight:bold;">
                    Annuler mon rendez-vous
                  </a>
                </p>
                <p style="font-size:12px;color:#666;">
                  Si le bouton ne fonctionne pas, copiez-collez ce lien :<br>
                  <span style="word-break:break-all;">%s</span>
                </p>
                <p>À bientôt,<br>Les Photos de Mai</p>
                """.formatted(
                html(firstName), html(lastName), html(formattedStart), html(serviceName),
                durationMin, address.html(), html(cancelUrl), html(cancelUrl)
        );

        String providerEmail = appointment.getProvider() != null
                ? trimToNull(appointment.getProvider().getEmail())
                : null;

        Instant now = Instant.now();
        EmailLog log = new EmailLog();
        log.setAppointment(appointment);
        log.setToEmail(user.getEmail());
        log.setTemplate("appointment_confirmation");
        log.setSentAt(now);

        if (mailer == null) {
            log.setStatus("failed");
            emailLogs.save(log);
            return;
        }

        try {
            MimeMessage message = mailer.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Confirmation de votre rendez-vous");
            helper.setText(textBody, htmlBody);

            if (providerEmail != null && providerEmail.contains("@")) {
                helper.setCc(providerEmail);
            }

            mailer.send(message);
            log.setStatus("sent");
            log.setMessageId(message.getMessageID());
        } catch (Exception e) {
            log.setStatus("failed");
            log.setMessageId(null);
        }

        emailLogs.save(log);
    }

    public AddressResult resolveAddressForAppointment(Appointment appointment) {
        String fallback = "Adresse : elle vous sera communiquée bientôt.";
        String fallbackHtml = html(fallback);

        if (appointment.getEvent() == null || appointment.getEvent().getShop() == null) {
            return new AddressResult(fallback, fallbackHtml);
        }

        Shop shop = appointment.getEvent().getShop();
        List<String> lines = new ArrayList<>();
        if (notBlank(shop.getName())) lines.add(shop.getName().trim());

        boolean hasAddress = false;
        if (notBlank(shop.getAddressLine1())) {
            lines.add(shop.getAddressLine1().trim());
            hasAddress = true;
        }
        if (notBlank(shop.getAddressLine2())) {
            lines.add(shop.getAddressLine2().trim());
            hasAddress = true;
        }

        String zipCity = (value(shop.getPostalCode()) + " " + value(shop.getCity())).trim();
        if (!zipCity.isBlank()) {
            lines.add(zipCity);
            hasAddress = true;
        }

        if (notBlank(shop.getCountry())) {
            lines.add(shop.getCountry().trim());
            hasAddress = true;
        }

        if (!hasAddress) return new AddressResult(fallback, fallbackHtml);

        String text = String.join("\n", lines);
        return new AddressResult(text, html(text).replace("\n", "<br>"));
    }

    public record AddressResult(String text, String html) {}

    private static String html(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isBlank() ? null : v;
    }

    private static String stripSlash(String value) {
        if (value == null || value.isBlank()) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
