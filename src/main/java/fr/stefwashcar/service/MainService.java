package fr.stefwashcar.service;

import fr.stefwashcar.model.Blackout;
import fr.stefwashcar.repository.BlackoutRepository;
import fr.stefwashcar.repository.ServiceRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MainService {
    private final ServiceRepository services;
    private final BlackoutRepository blackouts;
    private final JavaMailSender mailer;

    @Value("${app.mail.from:booking@lesphotosdemai.fr}")
    private String mailFrom;

    @Value("${app.mail.test-to:philippemai@yahoo.fr}")
    private String testMailTo;

    @Transactional
    public ResponseEntity<?> createBlackout(Map<String,Object> body) {
        Long serviceId = asLong(body.get("serviceId"));
        String date = str(body.get("date"));
        String start = str(body.get("start"));
        String end = str(body.get("end"));
        String name = str(body.get("name"));

        if (serviceId == null || date == null || start == null || end == null) {
            return ResponseEntity.badRequest().body(Map.of("error","missing_fields"));
        }

        var service = services.findById(serviceId).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error","service_not_found"));
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(service.getTimezone() != null ? service.getTimezone() : "Europe/Paris");
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime s = LocalTime.parse(start);
            LocalTime e = LocalTime.parse(end);

            LocalDateTime startLocal = d.atTime(s);
            LocalDateTime endLocal = d.atTime(e);

            if (!endLocal.isAfter(startLocal)) {
                return ResponseEntity.badRequest().body(Map.of("error","end_before_start"));
            }

            Blackout blackout = new Blackout();
            blackout.setService(service);
            blackout.setDate(d);
            blackout.setStartAtUtc(startLocal.atZone(zone).toInstant());
            blackout.setEndAtUtc(endLocal.atZone(zone).toInstant());
            blackout.setBlackoutName(name != null ? name : "");
            blackout = blackouts.save(blackout);

            Map<String,Object> result = new LinkedHashMap<>();
            result.put("id", blackout.getId());
            result.put("serviceId", service.getId());
            result.put("timezone", zone.getId());
            result.put("start_local", startLocal.atZone(zone).toString());
            result.put("end_local", endLocal.atZone(zone).toString());
            result.put("start_utc", blackout.getStartAtUtc());
            result.put("end_utc", blackout.getEndAtUtc());
            result.put("name", blackout.getBlackoutName());
            return ResponseEntity.status(201).body(result);
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_datetime_format"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> listServices() {
        return ResponseEntity.ok(services.findAll().stream().map(s -> {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("name", s.getName());
            row.put("duration_min", s.getDurationMin());
            row.put("capacity", s.getCapacity());
            row.put("timezone", s.getTimezone());
            row.put("rules_count", s.getAvailabilityRules() != null ? s.getAvailabilityRules().size() : 0);
            return row;
        }).toList());
    }

    public ResponseEntity<?> testMail() {
        try {
            MimeMessage message = mailer.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(testMailTo);
            helper.setSubject("Test mail");
            helper.setText("Hello, ceci est un test de Spring Mail.");
            mailer.send(message);
            return ResponseEntity.ok(Map.of("status","ok"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status","error","message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private static Long asLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && s.matches("\\d+")) return Long.parseLong(s);
        return null;
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isBlank() ? null : s;
    }
}
