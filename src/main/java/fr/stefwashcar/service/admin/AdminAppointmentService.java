package fr.stefwashcar.service.admin;

import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.model.*;
import fr.stefwashcar.repository.*;
import fr.stefwashcar.service.AdminAppointmentSlotChecker;
import fr.stefwashcar.service.MailBodyRenderer;
import fr.stefwashcar.service.ProviderResolver;
import fr.stefwashcar.service.availability.AvailabilityService;
import fr.stefwashcar.service.booking.AppointmentConfirmationMailer;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class AdminAppointmentService {
    private static final BigDecimal ORDER_AMOUNT_MAX = new BigDecimal("999999.99");
    private static final int ORDER_PHOTO_COUNT_MAX = 100000;
    private static final String SELF_PROVIDER_CODE = "_SELF";
    private static final Pattern PROVIDER_CODE = Pattern.compile("^[A-Z]{2,8}$");

    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final OrderRepository orders;
    private final ServiceRepository services;
    private final ShopRepository shops;
    private final EventRepository events;
    private final UserRepository users;
    private final EmailLogRepository emailLogs;
    private final AvailabilityService availabilityService;
    private final AdminAppointmentSlotChecker slotChecker;
    private final ProviderResolver providerResolver;
    private final MailBodyRenderer renderer;
    private final AppointmentConfirmationMailer confirmationMailer;
    private final JavaMailSender mailer;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.mail.from:booking@lesphotosdemai.fr}")
    private String mailFrom;

    @Value("${app.mail.archive-bcc:}")
    private String archiveBcc;

    public ResponseEntity<?> listAppointments(String date, Long serviceId) {
        if (date == null || date.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing ?date=YYYY-MM-DD or ?date=YYYY-MM or ?date=YYYY")
            );
        }

        ZoneId zone = ZoneId.of("Europe/Paris");
        Instant fromUtc;
        Instant toUtc;

        try {
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate d = LocalDate.parse(date);
                fromUtc = d.atStartOfDay(zone).toInstant();
                toUtc = d.plusDays(1).atStartOfDay(zone).toInstant();
            } else if (date.matches("\\d{4}-\\d{2}")) {
                YearMonth m = YearMonth.parse(date);
                fromUtc = m.atDay(1).atStartOfDay(zone).toInstant();
                toUtc = m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
            } else if (date.matches("\\d{4}")) {
                int y = Integer.parseInt(date);
                fromUtc = LocalDate.of(y,1,1).atStartOfDay(zone).toInstant();
                toUtc = LocalDate.of(y+1,1,1).atStartOfDay(zone).toInstant();
            } else {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid date format, use YYYY-MM-DD or YYYY-MM or YYYY")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","Invalid date format"));
        }

        List<Appointment> found = appointments.findConfirmedBetween(
                fromUtc,
                toUtc,
                serviceId != null && serviceId > 0 ? serviceId : null
        );

        List<Map<String,Object>> rows = found.stream()
                .map(a -> normalizeAppointment(a, zone))
                .toList();

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("date selectionné", date);
        result.put("nombre de rendez-vous", rows.size());
        result.put("rendez-vous", rows);
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> listProviders() {
        return ResponseEntity.ok(
                providers.findByIsActiveTrueOrderByDisplayNameAsc()
                        .stream().map(this::normalizeProvider).toList()
        );
    }

    public ResponseEntity<?> upsertOrder(Map<String,Object> data) {
        Long appointmentId = positiveLong(data.get("appointmentId"));
        Long orderId = optionalPositiveLong(data.get("orderId"));

        if (appointmentId == null) {
            return validation("appointmentId", "Doit être un entier positif.");
        }

        BigDecimal amount = parseAmount(data.get("amount"));
        if (data.containsKey("amount") && data.get("amount") != null && amount == null) {
            return validation("amount", "Doit être un nombre compris entre 0 et 999999.99.");
        }

        Integer photoCount = parsePhotoCount(data.get("photoCount"));
        if (data.containsKey("photoCount") && data.get("photoCount") != null && photoCount == null) {
            return validation("photoCount", "Doit être un entier compris entre 0 et 100000.");
        }

        Long providerId = optionalPositiveLong(data.get("providerId"));
        String providerCode = upper(data.get("providerCode"));
        String providerName = str(data.get("providerDisplayName"));
        String providerEmail = str(data.get("providerEmail"));

        if (providerCode != null && !SELF_PROVIDER_CODE.equals(providerCode)
                && !PROVIDER_CODE.matcher(providerCode).matches()) {
            return validation("providerCode", "Format invalide (ex: PM, AA, ABCD).");
        }

        if (providerEmail != null && !validEmail(providerEmail)) {
            return validation("providerEmail", "Adresse e-mail invalide.");
        }

        Appointment appointment = appointments.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error","appointment_not_found",
                    "message","Rendez-vous introuvable."
            ));
        }

        Provider provider = null;
        boolean isSelf = SELF_PROVIDER_CODE.equals(providerCode);

        if (providerId != null) provider = providers.findById(providerId).orElse(null);
        if (provider == null && providerCode != null && !isSelf) {
            provider = providers.findByCode(providerCode).orElse(null);
        }
        if (provider == null && providerEmail != null && !isSelf) {
            provider = providers.findByEmail(providerEmail).orElse(null);
        }

        if (provider == null && providerCode != null && !isSelf) {
            provider = new Provider();
            provider.setCode(providerCode);
            provider.setDisplayName(providerName != null ? providerName : providerCode);
            provider.setEmail(providerEmail);
            provider.setIsActive(true);
            provider = providers.save(provider);
        }

        Order order;
        if (orderId != null) {
            order = orders.findById(orderId).orElse(null);
            if (order == null || order.getAppointment() == null
                    || !order.getAppointment().getId().equals(appointment.getId())) {
                return ResponseEntity.status(404).body(Map.of(
                        "error","order_not_found",
                        "message","Commande introuvable pour ce rendez-vous."
                ));
            }
        } else {
            order = orders.findByAppointment(appointment).orElse(null);
        }

        if (order == null) {
            order = new Order();
            order.setAppointment(appointment);
            order.setReference(truncate(generateOrderReference(appointment, provider), 64));
        }

        order.setAmount(amount);
        order.setPhotoCount(photoCount);

        if (provider != null) {
            order.setProvider(provider);
            order.setProviderCode(provider.getCode());
        } else {
            order.setProvider(null);
            order.setProviderCode(null);
        }

        order = orders.save(order);
        return ResponseEntity.ok(normalizeOrder(order));
    }

    public ResponseEntity<?> sendOrderEmail(Map<String,Object> data) {
        Long appointmentId = positiveLong(data.get("appointmentId"));
        Long orderId = positiveLong(data.get("orderId"));
        boolean forceResend = bool(data.get("forceResend"));
        String reason = str(data.get("reason"));

        if (appointmentId == null || orderId == null) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed"));
        }

        Appointment appointment = appointments.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return ResponseEntity.status(404).body(Map.of("error","appointment_not_found"));
        }

        Order order = orders.findById(orderId).orElse(null);
        if (order == null || order.getAppointment() == null
                || !order.getAppointment().getId().equals(appointment.getId())) {
            return ResponseEntity.status(404).body(Map.of("error","order_not_found"));
        }

        if (order.getAmount() == null || order.getPhotoCount() == null) {
            return ResponseEntity.badRequest().body(Map.of("error","missing_order_data"));
        }

        boolean alreadySent = order.getEmailFirstSentAt() != null || order.getEmailLastSentAt() != null;
        if (alreadySent && !forceResend) {
            return ResponseEntity.status(409).body(Map.of("error","already_sent"));
        }

        String to = appointment.getUser() != null ? str(appointment.getUser().getEmail()) : null;
        if (!validEmail(to)) {
            return ResponseEntity.badRequest().body(Map.of("error","email_invalid"));
        }

        ZonedDateTime local = appointment.getStartAtUtc().atZone(ZoneId.of("Europe/Paris"));
        String dateLabel = local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeLabel = local.format(DateTimeFormatter.ofPattern("HH:mm"));

        Map<String,String> note = renderer.render(appointment.getNotes());
        Map<String,String> reasonRendered = renderer.render(reason);

        String textBody = """
                Bonjour %s %s,

                Voici le récapitulatif de votre séance photo du %s à %s.

                Montant : %s €
                Nombre de photos : %d photo(s)
                Référence de commande : %s

                %s%sBien cordialement,
                Les Photos de Mai
                """.formatted(
                value(appointment.getUser().getFirstname()),
                value(appointment.getUser().getLastname()),
                dateLabel, timeLabel,
                order.getAmount().toPlainString(),
                order.getPhotoCount(),
                value(order.getReference()),
                textBlock("Note", note.get("text")),
                textBlock("Motif", reasonRendered.get("text"))
        );

        String htmlBody = """
                <div style="font-family:Segoe UI,Arial,sans-serif;font-size:14px;line-height:1.5;">
                  <p>Bonjour %s %s,</p>
                  <p>Voici le récapitulatif de votre séance photo du <strong>%s</strong> à <strong>%s</strong>.</p>
                  <ul>
                    <li><strong>Montant :</strong> %s €</li>
                    <li><strong>Nombre de photos :</strong> %d photo(s)</li>
                    <li><strong>Référence :</strong> %s</li>
                  </ul>
                  %s%s
                  <p>Bien cordialement,<br>Les Photos de Mai</p>
                </div>
                """.formatted(
                html(value(appointment.getUser().getFirstname())),
                html(value(appointment.getUser().getLastname())),
                html(dateLabel), html(timeLabel),
                html(order.getAmount().toPlainString()),
                order.getPhotoCount(),
                html(value(order.getReference())),
                htmlBlock("Note", note.get("html")),
                htmlBlock("Motif", reasonRendered.get("html"))
        );

        Instant now = Instant.now();
        try {
            sendMime(to,
                    "Récapitulatif de votre séance photo du " + dateLabel + " à " + timeLabel,
                    textBody, htmlBody, providerEmail(appointment, order));

            if (order.getEmailFirstSentAt() == null) order.setEmailFirstSentAt(now);
            order.setEmailLastSentAt(now);
            saveLog(appointment, to, "appointment_order_summary", "sent", now);
        } catch (Exception e) {
            saveLog(appointment, to, "appointment_order_summary", "failed", now);
        }

        return ResponseEntity.ok(normalizeOrder(order));
    }

    public ResponseEntity<?> overrideSlot(Map<String,Object> data, String remoteIp) {
        Long serviceId = positiveLong(data.get("serviceId"));
        Long providerId = optionalPositiveLong(data.get("providerId"));
        String startRaw = str(data.get("startAtUtc"));
        String endRaw = str(data.get("endAtUtc"));
        String idempotencyKey = str(data.get("idempotencyKey"));
        Long shopId = optionalPositiveLong(data.get("shopId"));
        Long eventId = optionalPositiveLong(data.get("eventId"));

        if (idempotencyKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error","idempotency_key_required"));
        }

        var existing = appointments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return ResponseEntity.ok(appointments.normalizeOverrideAppointment(existing.get()));
        }

        if (serviceId == null || startRaw == null || endRaw == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","Champs requis : serviceId, startAtUtc, endAtUtc"
            ));
        }

        Instant start;
        Instant end;
        try {
            start = Instant.parse(startRaw);
            end = Instant.parse(endRaw);
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error","Format de date invalide"));
        }

        if (!end.isAfter(start)) {
            return ResponseEntity.badRequest().body(Map.of("error","endAtUtc doit être après startAtUtc"));
        }

        var service = services.findById(serviceId).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error","Service introuvable"));
        }

        Provider provider;
        try {
            provider = providerResolver.resolveProvider(
                    providerId,
                    SecurityContextHolder.getContext().getAuthentication()
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Utilisateur non authentifié"
            ));
        }

        if (appointments.hasOverlap(provider.getId(), start, end)) {
            return ResponseEntity.status(409).body(Map.of(
                    "error","Ce créneau est déjà occupé pour ce prestataire"
            ));
        }

        Object clientRaw = data.get("client");
        if (!(clientRaw instanceof Map<?,?> client)) {
            return ResponseEntity.badRequest().body(Map.of("error","Données client incomplètes"));
        }

        String email = str(client.get("email"));
        String firstName = str(client.get("firstName"));
        String lastName = str(client.get("lastName"));
        String phone = str(client.get("phone"));
        String notes = str(client.get("notes"));

        if (email == null || firstName == null || lastName == null) {
            return ResponseEntity.badRequest().body(Map.of("error","Données client incomplètes"));
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setPhone(phone);
            user.setGdprConsent(true);
            user.setLastip(remoteIp);
            user.setCreatedAt(Instant.now());
            user = users.save(user);
        }

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setService(service);
        appointment.setProvider(provider);
        appointment.setStartAtUtc(start);
        appointment.setEndAtUtc(end);
        appointment.setStatus(AppointmentStatus.confirmed);
        appointment.setCancelToken(randomToken());
        appointment.setCancelTokenExpiresAt(start);
        appointment.setNotes(notes != null ? truncate(notes,255) : null);
        appointment.setCreatedAt(Instant.now());
        appointment.setIsManualOverride(true);
        appointment.setIdempotencyKey(idempotencyKey);

        if (eventId != null) {
            Event event = events.findById(eventId).orElse(null);
            if (event == null) {
                return ResponseEntity.status(404).body(Map.of("error","event_not_found"));
            }

            if (shopId != null) {
                Shop eventShop = event.getShop();
                if (eventShop == null || !eventShop.getId().equals(shopId)) {
                    return ResponseEntity.badRequest().body(Map.of("error","shop_event_mismatch"));
                }
            }
            appointment.setEvent(event);
        } else if (shopId != null) {
            return ResponseEntity.badRequest().body(Map.of("error","event_required_with_shop"));
        }

        appointment = appointments.save(appointment);
        return ResponseEntity.status(201).body(
                appointments.normalizeOverrideAppointment(appointment)
        );
    }

    public ResponseEntity<?> listSlots(Long serviceId, String date) {
        if (serviceId == null || date == null || date.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","missing_parameters",
                    "required",List.of("serviceId","date (YYYY-MM-DD)")
            ));
        }

        var service = services.findById(serviceId).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error","service_not_found"));
        }

        Map<String,Object> daily = availabilityService.getDailyAvailability(service,date);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> official =
                daily.get("slots") instanceof List<?> list
                        ? (List<Map<String,Object>>) list
                        : List.of();

        int step = daily.get("stepMin") instanceof Number n
                ? n.intValue()
                : service.getDurationMin() != null
                ? service.getDurationMin().intValue()
                : 20;

        ZoneId zone = ZoneId.of(
                daily.get("timezone") != null ? daily.get("timezone").toString() : "Europe/Paris"
        );

        LocalDate day;
        try { day = LocalDate.parse(date); }
        catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_date_format"));
        }

        Map<String,Map<String,Object>> index = new LinkedHashMap<>();
        for (Map<String,Object> s : official) {
            if (s.get("time") != null) index.put(s.get("time").toString(),s);
        }

        LocalTime start = official.isEmpty()
                ? LocalTime.of(8,0)
                : LocalTime.parse(official.getFirst().get("time").toString());

        LocalTime end = official.isEmpty()
                ? LocalTime.of(20,0)
                : LocalTime.parse(official.getLast().get("time").toString()).plusMinutes(step);

        List<Map<String,Object>> slots = new ArrayList<>();

        for (ZonedDateTime cursor = day.atTime(start).atZone(zone);
             cursor.isBefore(day.atTime(end).atZone(zone));
             cursor = cursor.plusMinutes(step)) {

            String hhmm = cursor.format(DateTimeFormatter.ofPattern("HH:mm"));
            Map<String,Object> source = index.get(hhmm);
            String status = source == null
                    ? "outOfRule"
                    : Boolean.TRUE.equals(source.get("available")) ? "free" : "busy";

            Map<String,Object> row = new LinkedHashMap<>();
            row.put("time",hhmm);
            row.put("status",status);
            row.put("startsAt",cursor.toOffsetDateTime().toString());
            row.put("startsAtUtc",cursor.toInstant().toString());
            slots.add(row);
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("serviceId",service.getId());
        result.put("date",day.toString());
        result.put("timezone",zone.getId());
        result.put("slots",slots);
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> listShops(boolean activeOnly) {
        List<Shop> source = activeOnly
                ? shops.findByIsActiveTrueOrderByNameAsc()
                : shops.findAllByOrderByNameAsc();

        return ResponseEntity.ok(source.stream().map(this::normalizeShop).toList());
    }

    public ResponseEntity<?> listEvents(Long shopId, boolean futureOnly) {
        if (shopId == null || shopId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error","missing_or_invalid_shopId"));
        }

        Shop shop = shops.findById(shopId).orElse(null);
        if (shop == null) {
            return ResponseEntity.status(404).body(Map.of("error","shop_not_found"));
        }

        List<Event> source = futureOnly
                ? events.findByShopAndEndAtUtcGreaterThanEqualOrderByStartAtUtcAsc(shop,Instant.now())
                : events.findByShopOrderByStartAtUtcAsc(shop);

        return ResponseEntity.ok(source.stream().map(e -> {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id",e.getId());
            row.put("name",e.getName());
            row.put("startAtUtc",e.getStartAtUtc());
            row.put("endAtUtc",e.getEndAtUtc());
            row.put("timezone",e.getTimezone());
            row.put("shopId",e.getShop() != null ? e.getShop().getId() : null);
            row.put("status",e.getStatus());
            row.put("isActive","active".equals(e.getStatus()));
            return row;
        }).toList());
    }

    public ResponseEntity<?> updateQuickNote(Map<String,Object> data) {
        Long appointmentId = positiveLong(data.get("appointmentId"));
        Object raw = data.get("notes");

        if (appointmentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_appointment_id"));
        }

        if (raw != null && !(raw instanceof String)) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_note"));
        }

        Appointment a = appointments.findById(appointmentId).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("error","appointment_not_found"));
        }

        String note = raw == null ? null : raw.toString()
                .replace("\r\n","\n").replace("\r","\n")
                .replaceAll("[\\p{Cc}&&[^\\n\\t]]","")
                .trim();

        if (note != null && note.length() > 4000) note = note.substring(0,4000);
        if (note != null && note.isBlank()) note = null;
        a.setNotes(note);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("id",a.getId());
        result.put("notes",a.getNotes());
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> sendCustomAppointmentEmail(Map<String,Object> data) {
        Long appointmentId = positiveLong(data.get("appointmentId"));
        String subject = str(data.get("subject"));
        String messageMd = str(data.get("message"));
        String reason = str(data.get("reason"));

        if (appointmentId == null || subject == null || messageMd == null
                || subject.length() > 150 || messageMd.length() > 8000
                || subject.contains("\r") || subject.contains("\n")) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed"));
        }

        Appointment a = appointments.findById(appointmentId).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("error","appointment_not_found"));
        }

        String to = a.getUser() != null ? str(a.getUser().getEmail()) : null;
        if (!validEmail(to)) {
            return ResponseEntity.badRequest().body(Map.of("error","missing_email"));
        }

        Map<String,String> msg = renderer.render(messageMd);
        Map<String,String> rsn = renderer.render(reason);
        Map<String,String> note = renderer.render(a.getNotes());

        String textBody = msg.get("text")
                + textBlock("Motif",rsn.get("text"))
                + textBlock("Note",note.get("text"));

        String htmlBody = """
                <div style="font-family:Segoe UI,Arial,sans-serif;font-size:14px;line-height:1.5;">
                  <p>Bonjour %s %s,</p>
                  %s%s%s
                  <p>Bien cordialement,<br>Les Photos de Mai</p>
                </div>
                """.formatted(
                html(value(a.getUser().getFirstname())),
                html(value(a.getUser().getLastname())),
                htmlBlock("Motif",rsn.get("html")),
                htmlBlock("Note",note.get("html")),
                msg.get("html")
        );

        try {
            sendMime(to,subject,textBody,htmlBody,
                    a.getProvider() != null ? a.getProvider().getEmail() : null);
            return ResponseEntity.ok(Map.of("ok",true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error","mail_send_failed"));
        }
    }

    public ResponseEntity<?> resendConfirmationEmail(Map<String,Object> data) {
        Long appointmentId = positiveLong(data.get("appointmentId"));
        boolean force = bool(data.get("forceResend"));

        if (appointmentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed"));
        }

        Appointment a = appointments.findById(appointmentId).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("error","appointment_not_found"));
        }

        if (!force && emailLogs
                .findFirstByAppointmentAndTemplateAndStatusOrderBySentAtDesc(
                        a,"appointment_confirmation","sent"
                ).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error","already_sent"));
        }

        confirmationMailer.send(a);
        return ResponseEntity.ok(Map.of("ok",true));
    }

    public ResponseEntity<?> setAppointmentEvent(Long id, Map<String,Object> data) {
        Appointment a = appointments.findById(id).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("error","appointment_not_found"));
        }

        if (!data.containsKey("eventId")) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed"));
        }

        Object raw = data.get("eventId");
        if (raw == null) {
            a.setEvent(null);
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("ok",true);
            result.put("appointmentId",a.getId());
            result.put("eventId",null);
            return ResponseEntity.ok(result);
        }

        Long eventId = positiveLong(raw);
        if (eventId == null) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed"));
        }

        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.status(404).body(Map.of("error","event_not_found"));
        }

        a.setEvent(event);
        return ResponseEntity.ok(Map.of(
                "ok",true,
                "appointmentId",a.getId(),
                "eventId",event.getId()
        ));
    }

    public ResponseEntity<?> patchAppointment(Long id, Map<String,Object> data) {
        Appointment a = appointments.findById(id).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("success",false,"code","not_found"));
        }

        String status = str(data.get("status"));
        String date = str(data.get("date"));
        String horaire = str(data.get("horaire"));
        boolean force = bool(data.get("force"));

        if ("canceled".equals(status)) {
            a.setStatus(AppointmentStatus.cancelled);
            return ResponseEntity.ok(Map.of("success",true,"code","canceled"));
        }

        boolean hasShift = date != null || horaire != null;
        if (hasShift) {
            if (date == null || horaire == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success",false,"code","missing_fields",
                        "message","Pour décaler, fournir date + horaire."
                ));
            }

            Map<String,Object> check = slotChecker.check(
                    a.getService(),date,horaire,a.getId()
            );

            String slotStatus = value(check.get("status"));

            if (!force && "busy".equals(slotStatus)) {
                return ResponseEntity.status(409).body(Map.of(
                        "success",false,"code","slot_busy",
                        "message","Créneau déjà pris.","data",check
                ));
            }

            if (!force && "outOfRule".equals(slotStatus)) {
                return ResponseEntity.status(409).body(Map.of(
                        "success",false,"code","slot_out_of_rule",
                        "message","Créneau hors disponibilité.","data",check
                ));
            }

            ZoneId zone = ZoneId.of(
                    a.getService().getTimezone() != null
                            ? a.getService().getTimezone()
                            : "Europe/Paris"
            );

            ZonedDateTime startLocal = LocalDate.parse(date)
                    .atTime(LocalTime.parse(horaire))
                    .atZone(zone);

            int duration = a.getService().getDurationMin() != null
                    ? a.getService().getDurationMin().intValue()
                    : 20;

            a.setStartAtUtc(startLocal.toInstant());
            a.setEndAtUtc(startLocal.plusMinutes(duration).toInstant());

            return ResponseEntity.ok(Map.of("success",true,"code","shifted"));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success",false,"code","no_op",
                "message","Aucune modification fournie."
        ));
    }

    private Map<String,Object> normalizeAppointment(Appointment a, ZoneId zone) {
        ZonedDateTime local = a.getStartAtUtc().atZone(zone);
        User u = a.getUser();
        Order order = a.getOrder();
        Event event = a.getEvent();
        Shop shop = event != null ? event.getShop() : null;

        var sent = emailLogs.findFirstByAppointmentAndTemplateAndStatusOrderBySentAtDesc(
                a,"appointment_confirmation","sent"
        );

        Map<String,Object> context = new LinkedHashMap<>();
        context.put("hasEvent",event != null);
        context.put("hasShop",shop != null);
        context.put("emailAlreadySent",sent.isPresent());
        context.put("emailAlreadySentAt",sent.map(EmailLog::getSentAt).orElse(null));

        if (event != null) {
            Map<String,Object> e = new LinkedHashMap<>();
            e.put("id",event.getId()); e.put("name",event.getName());
            e.put("startAt",event.getStartAtUtc()); e.put("endAt",event.getEndAtUtc());
            context.put("event",e);
        } else context.put("event",null);

        if (shop != null) {
            Map<String,Object> s = new LinkedHashMap<>();
            s.put("id",shop.getId()); s.put("name",shop.getName());
            s.put("city",shop.getCity()); s.put("address",shop.getAddressLine1());
            context.put("shop",s);
        } else context.put("shop",null);

        Map<String,Object> row = new LinkedHashMap<>();
        row.put("id",a.getId());
        row.put("date",local.toLocalDate().toString());
        row.put("horaire",local.format(DateTimeFormatter.ofPattern("HH:mm")));
        row.put("prénom",u != null ? u.getFirstname() : null);
        row.put("nom",u != null ? u.getLastname() : null);
        row.put("email",u != null ? u.getEmail() : null);
        row.put("phone",u != null ? u.getPhone() : null);
        row.put("notes",a.getNotes());
        row.put("createdAt",a.getCreatedAt());
        row.put("nomComplet",u != null ? u.getFullName() : "");
        row.put("context",context);
        row.put("order",order != null ? normalizeOrder(order) : null);
        return row;
    }

    private Map<String,Object> normalizeProvider(Provider p) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",p.getId()); r.put("code",p.getCode());
        r.put("displayName",p.getDisplayName()); r.put("email",p.getEmail());
        r.put("phone",p.getPhone()); r.put("color",p.getColor());
        r.put("isActive",p.getIsActive());
        return r;
    }

    private Map<String,Object> normalizeOrder(Order o) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",o.getId());
        r.put("appointmentId",o.getAppointment() != null ? o.getAppointment().getId() : null);
        r.put("amount",o.getAmount()); r.put("photoCount",o.getPhotoCount());
        r.put("providerCode",o.getProviderCode());
        r.put("provider",o.getProvider() != null ? normalizeProvider(o.getProvider()) : null);
        r.put("reference",o.getReference());
        r.put("createdAt",o.getCreatedAt()); r.put("updatedAt",o.getUpdatedAt());
        r.put("emailFirstSentAt",o.getEmailFirstSentAt());
        r.put("emailLastSentAt",o.getEmailLastSentAt());
        return r;
    }

    private Map<String,Object> normalizeShop(Shop s) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",s.getId()); r.put("name",s.getName());
        r.put("code",s.getPostalCode()); r.put("addressLine1",s.getAddressLine1());
        r.put("addressLine2",s.getAddressLine2()); r.put("postalCode",s.getPostalCode());
        r.put("city",s.getCity()); r.put("notes",s.getNotes());
        r.put("notesHtml",s.getNotesHtml() != null ? s.getNotesHtml() : "");
        r.put("country",s.getCountry()); r.put("region",s.getRegion());
        r.put("phone",s.getPhone()); r.put("email",s.getEmail());
        r.put("isActive",s.getIsActive());

        r.put("events",s.getEvents() == null ? List.of() : s.getEvents().stream().map(e -> {
            Map<String,Object> x = new LinkedHashMap<>();
            x.put("id",e.getId()); x.put("name",e.getName());
            x.put("startsAt",e.getStartAtUtc()); x.put("endsAt",e.getEndAtUtc());
            return x;
        }).toList());

        return r;
    }

    private String generateOrderReference(Appointment a, Provider p) {
        String code = p != null ? p.getCode() : "XX";
        String date = a.getStartAtUtc() != null
                ? a.getStartAtUtc().atZone(ZoneId.of("Europe/Paris"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : "00000000";

        byte[] bytes = new byte[2];
        random.nextBytes(bytes);
        String randomPart = HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);

        return "%s-%s-%d-%s".formatted(code,date,a.getId(),randomPart);
    }

    private void sendMime(String to, String subject, String text, String html, String providerEmail)
            throws Exception {
        MimeMessage message = mailer.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message,false,StandardCharsets.UTF_8.name());

        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text,html);

        String provider = str(providerEmail);
        if (validEmail(provider) && !provider.equalsIgnoreCase(to)) helper.setBcc(provider);
        if (validEmail(archiveBcc) && !archiveBcc.equalsIgnoreCase(to)) helper.addBcc(archiveBcc);

        mailer.send(message);
    }

    private void saveLog(Appointment a, String to, String template, String status, Instant at) {
        EmailLog log = new EmailLog();
        log.setAppointment(a); log.setToEmail(to); log.setTemplate(template);
        log.setStatus(status); log.setSentAt(at); log.setMessageId(null);
        emailLogs.save(log);
    }

    private String providerEmail(Appointment a, Order o) {
        if (a.getProvider() != null && validEmail(a.getProvider().getEmail())) {
            return a.getProvider().getEmail();
        }
        if (o.getProvider() != null && validEmail(o.getProvider().getEmail())) {
            return o.getProvider().getEmail();
        }
        return null;
    }

    private ResponseEntity<?> validation(String field, String message) {
        return ResponseEntity.badRequest().body(Map.of(
                "error","validation_failed",
                "details",Map.of(field,List.of(message))
        ));
    }

    private static BigDecimal parseAmount(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            BigDecimal n = new BigDecimal(v.toString().replace(',','.'));
            return n.compareTo(BigDecimal.ZERO) >= 0 && n.compareTo(ORDER_AMOUNT_MAX) <= 0 ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static Integer parsePhotoCount(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            int n = Integer.parseInt(v.toString());
            return n >= 0 && n <= ORDER_PHOTO_COUNT_MAX ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static Long positiveLong(Object v) {
        Long n = optionalPositiveLong(v);
        return n != null && n > 0 ? n : null;
    }

    private static Long optionalPositiveLong(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            long n = Long.parseLong(v.toString());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isBlank() ? null : s;
    }

    private static String upper(Object v) {
        String s = str(v);
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return v != null && Boolean.parseBoolean(v.toString());
    }

    private static boolean validEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                && !s.contains("\r") && !s.contains("\n");
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0,max);
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().replace("-","")
                + UUID.randomUUID().toString().replace("-","");
    }

    private static String textBlock(String title, String text) {
        return text == null || text.isBlank() ? "" : title + " :\n" + text + "\n\n";
    }

    private static String htmlBlock(String title, String html) {
        return html == null || html.isBlank() ? "" :
                "<div style=\"margin:14px 0;padding:12px 14px;background:#f5f8ff;border-left:4px solid #106ebe;\">"
                + "<div style=\"font-weight:700;margin-bottom:6px;\">"
                + HtmlUtils.htmlEscape(title) + " :</div><div>" + html + "</div></div>";
    }

    private static String html(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static String value(Object v) {
        return v == null ? "" : v.toString();
    }
}
