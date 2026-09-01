package fr.stefwashcar.service.booking;

import fr.stefwashcar.dto.booking.AppointmentResponse;
import fr.stefwashcar.dto.booking.CreateReservationRequest;
import fr.stefwashcar.dto.booking.ReservationResponse;
import fr.stefwashcar.dto.booking.ReservationUserResponse;
import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.User;
import fr.stefwashcar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationService {
    private static final Pattern NAME = Pattern.compile("^[a-z]{2,60}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ULID = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");

    private final AppointmentRepository appointments;
    private final ServiceRepository services;
    private final FormuleRepository formules;
    private final UserRepository users;
    private final EventRepository events;
    private final AppointmentConfirmationMailer confirmationMailer;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ResponseEntity<?> createReservation(CreateReservationRequest data, String remoteIp) {
        Long serviceId = data.serviceId();
        Instant startAtUtc = data.startsAt();
        String lastName = str(data.lastName());
        String firstName = str(data.firstName());
        String email = str(data.email());
        String phone = str(data.phone());
        boolean gdpr = Boolean.TRUE.equals(data.gdprConsent());
        boolean manualOverride = Boolean.TRUE.equals(data.isManualOverride());
        String idempotencyKey = str(data.idempotencyKey());
        String eventPublicId = str(data.eventPublicId());
        String formulePublicId = str(data.formulePublicId());

        List<String> errors = validate(lastName, firstName, email, gdpr);
        if (idempotencyKey == null) errors.add("idempotencyKey_required");
        if (startAtUtc == null) errors.add("startsAt_required");

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","validation_error","errors",errors
            ));
        }

        var existing = appointments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return ResponseEntity.ok(response(existing.get(), false));
        }

        var service = services.findById(serviceId != null ? serviceId : -1L).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error","service_not_found"));
        }

        var event = eventPublicId != null ? events.findByPublicId(eventPublicId).orElse(null) : null;
        if (eventPublicId != null && event == null) {
            return ResponseEntity.status(404).body(Map.of("error","event_not_found"));
        }

        var formule = formulePublicId != null ? formules.findByPublicId(formulePublicId).orElse(null) : null;
        if (formulePublicId != null) {
            if (!ULID.matcher(formulePublicId).matches()) {
                return ResponseEntity.badRequest().body(Map.of("error","invalid_formule_public_id"));
            }
            if (formule == null) {
                return ResponseEntity.status(404).body(Map.of("error","formule_not_found"));
            }
            if (formule.getService() == null || !formule.getService().getId().equals(service.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error","formule_service_mismatch"));
            }
        }

        int duration = service.getDurationMin() != null ? service.getDurationMin().intValue() : 20;
        Instant endAtUtc = startAtUtc.plus(Duration.ofMinutes(duration));

        if (appointments.findBlockingConflict(service, startAtUtc, endAtUtc).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error","slot_already_booked"));
        }

        User user = users.findByEmail(email).orElse(null);
        Instant now = Instant.now();

        if (user == null) {
            user = new User();
            user.setLastname(lastName);
            user.setFirstname(firstName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setGdprConsent(gdpr);
            user.setLastip(remoteIp);
            user.setCreatedAt(now);
            user = users.save(user);
        } else {
            if (phone != null) user.setPhone(phone);
            if (gdpr) user.setGdprConsent(true);
        }

        Appointment a = new Appointment();
        a.setUser(user);
        a.setService(service);
        a.setFormule(formule);
        a.setStartAtUtc(startAtUtc);
        a.setEndAtUtc(endAtUtc);
        a.setStatus(AppointmentStatus.confirmed);
        a.setCreatedAt(now);
        a.setCancelToken(newToken());
        a.setCancelTokenExpiresAt(startAtUtc);
        a.setIsManualOverride(manualOverride);
        a.setEvent(event);
        a.setIdempotencyKey(idempotencyKey);

        a = appointments.save(a);
        confirmationMailer.send(a);

        return ResponseEntity.status(201).body(response(a, false));
    }

    @Transactional
    public ResponseEntity<?> overrideReservation(Map<String,Object> data, String remoteIp) {
        List<String> required = List.of(
                "serviceId","startsAt","lastName","firstName","email","gdprConsent","idempotencyKey"
        );
        List<String> missing = required.stream().filter(k -> !data.containsKey(k)).toList();

        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","missing_fields","missing",missing
            ));
        }

        Long serviceId = asLong(data.get("serviceId"));
        String startsAtRaw = str(data.get("startsAt"));
        String lastName = str(data.get("lastName"));
        String firstName = str(data.get("firstName"));
        String email = str(data.get("email"));
        String phone = str(data.get("phone"));
        boolean gdpr = bool(data.get("gdprConsent"));
        String idempotencyKey = str(data.get("idempotencyKey"));
        String eventPublicId = str(data.get("eventPublicId"));

        List<String> errors = validate(lastName, firstName, email, gdpr);
        if (idempotencyKey == null) errors.add("idempotencyKey_required");
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","validation_error","errors",errors
            ));
        }

        var existing = appointments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return ResponseEntity.ok(response(existing.get(), true));
        }

        var service = services.findById(serviceId != null ? serviceId : -1L).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error","service_not_found"));
        }

        Instant startAtUtc;
        try {
            startAtUtc = Instant.parse(startsAtRaw);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_startsAt"));
        }

        var event = eventPublicId != null ? events.findByPublicId(eventPublicId).orElse(null) : null;
        if (eventPublicId != null && event == null) {
            return ResponseEntity.status(404).body(Map.of("error","event_not_found"));
        }

        if (event != null) {
            if (event.getStartAtUtc() != null && startAtUtc.isBefore(event.getStartAtUtc())) {
                return ResponseEntity.badRequest().body(Map.of("error","startsAt_outside_event"));
            }
            if (event.getEndAtUtc() != null && !startAtUtc.isBefore(event.getEndAtUtc())) {
                return ResponseEntity.badRequest().body(Map.of("error","startsAt_outside_event"));
            }
        }

        int duration = service.getDurationMin() != null ? service.getDurationMin().intValue() : 20;
        Instant endAtUtc = startAtUtc.plus(Duration.ofMinutes(duration));

        if (appointments.findBlockingConflict(service, startAtUtc, endAtUtc).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error","slot_already_booked"));
        }

        User user = users.findByEmail(email).orElse(null);
        Instant now = Instant.now();

        if (user == null) {
            user = new User();
            user.setLastname(lastName);
            user.setFirstname(firstName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setGdprConsent(gdpr);
            user.setLastip(remoteIp);
            user.setCreatedAt(now);
            user = users.save(user);
        } else {
            if (phone != null) user.setPhone(phone);
            if (gdpr) user.setGdprConsent(true);
        }

        Appointment a = new Appointment();
        a.setUser(user);
        a.setService(service);
        a.setStartAtUtc(startAtUtc);
        a.setEndAtUtc(endAtUtc);
        a.setStatus(AppointmentStatus.confirmed);
        a.setCreatedAt(now);
        a.setCancelToken(newToken());
        a.setCancelTokenExpiresAt(startAtUtc);
        a.setEvent(event);
        a.setIdempotencyKey(idempotencyKey);
        a.setIsManualOverride(true);

        a = appointments.save(a);
        confirmationMailer.send(a);
        return ResponseEntity.status(201).body(response(a, true));
    }

    private List<String> validate(String lastName, String firstName, String email, boolean gdpr) {
        List<String> errors = new ArrayList<>();
        if (!gdpr) errors.add("gdpr_consent_required");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) errors.add("email_invalid");
        if (lastName == null || !NAME.matcher(lastName).matches()) errors.add("lastName_invalid");
        if (firstName == null || !NAME.matcher(firstName).matches()) errors.add("firstName_invalid");
        return errors;
    }

    private ReservationResponse response(Appointment appointment, boolean override) {
        var service = appointment.getService();
        var formule = appointment.getFormule();
        var user = appointment.getUser();

        AppointmentResponse appointmentResponse = new AppointmentResponse(
                appointment.getId(),
                service != null ? service.getId() : null,
                formule != null ? formule.getPublicId() : null,
                appointment.getStartAtUtc(),
                appointment.getEndAtUtc(),
                appointment.getStatus(),
                override ? true : null
        );

        ReservationUserResponse userResponse = new ReservationUserResponse(
                user != null ? user.getId() : null,
                user != null ? user.getLastname() : null,
                user != null ? user.getFirstname() : null,
                user != null ? user.getEmail() : null
        );

        return new ReservationResponse(true, appointmentResponse, userResponse);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return v != null && Boolean.parseBoolean(v.toString());
    }
}
