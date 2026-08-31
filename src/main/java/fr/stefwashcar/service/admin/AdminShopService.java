package fr.stefwashcar.service.admin;

import fr.stefwashcar.model.Event;
import fr.stefwashcar.model.Shop;
import fr.stefwashcar.repository.AppointmentRepository;
import fr.stefwashcar.repository.EventRepository;
import fr.stefwashcar.repository.ShopRepository;
import fr.stefwashcar.service.MarkdownRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class AdminShopService {
    private static final Pattern POSTAL = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\- ]{1,11}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 .\\-()]{6,20}$");

    private final ShopRepository shops;
    private final EventRepository events;
    private final AppointmentRepository appointments;
    private final MarkdownRenderer markdownRenderer;

    public ResponseEntity<?> createShop(Map<String,Object> data) {
        String name = clean(data.get("name"));
        String address1 = clean(data.get("addressLine1"));
        String address2 = clean(data.get("addressLine2"));
        String postalCode = clean(data.get("postalCode"));
        String city = clean(data.get("city"));
        String region = clean(data.get("region"));
        String country = clean(data.get("country"));
        String phone = clean(data.get("phone"));
        String email = clean(data.get("email"));
        String notes = clean(data.get("notes"));
        boolean active = !data.containsKey("isActive") || bool(data.get("isActive"));

        Map<String,Object> errors = new LinkedHashMap<>();
        if (name == null) errors.put("name", "Nom requis.");
        if (address1 == null) errors.put("addressLine1", "Adresse requise.");
        if (postalCode == null) errors.put("postalCode", "Code postal requis.");
        if (city == null) errors.put("city", "Ville requise.");
        if (email != null && !validEmail(email)) errors.put("email", "Adresse e-mail invalide.");

        if (!errors.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("ok",false,"errors",errors));
        }

        Shop shop = new Shop();
        shop.setName(name);
        shop.setAddressLine1(address1);
        shop.setAddressLine2(address2);
        shop.setPostalCode(postalCode);
        shop.setCity(city);
        shop.setRegion(region);
        shop.setCountry(country != null ? country.toUpperCase() : "FR");
        shop.setPhone(phone);
        shop.setEmail(email);
        shop.setNotes(notes);
        shop.setIsActive(active);
        shop = shops.save(shop);

        return ResponseEntity.status(201).body(Map.of("ok",true,"shop",normalizeShop(shop)));
    }

    public ResponseEntity<?> createEvent(Map<String,Object> data) {
        Long shopId = positiveLong(data.get("shopId"));
        String name = clean(data.get("name"));
        String timezone = clean(data.get("timezone"));
        String startAt = clean(data.get("startAt"));
        String endAt = clean(data.get("endAt"));
        Integer maxCapacity = optionalInt(data.get("maxCapacity"));

        if (timezone == null) timezone = "Europe/Paris";

        Map<String,Object> errors = new LinkedHashMap<>();
        if (shopId == null) errors.put("shopId","Doit être un entier positif.");
        if (name == null) errors.put("name","Nom requis.");
        if (name != null && name.length() > 120) errors.put("name","Max 120 caractères.");
        if (startAt == null) errors.put("startAt","startAt requis.");
        if (endAt == null) errors.put("endAt","endAt requis.");
        if (maxCapacity != null && maxCapacity <= 0) errors.put("maxCapacity","Doit être > 0.");

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error","validation_failed","details",errors));
        }

        Shop shop = shops.findById(shopId).orElse(null);
        if (shop == null) {
            return ResponseEntity.status(404).body(Map.of("error","shop_not_found"));
        }

        Instant startUtc;
        Instant endUtc;
        try {
            ZoneId zone = ZoneId.of(timezone);
            startUtc = parseInstant(startAt, zone);
            endUtc = parseInstant(endAt, zone);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid_dates"));
        }

        if (!endUtc.isAfter(startUtc)) {
            return ResponseEntity.badRequest().body(Map.of("error","end_before_start"));
        }

        Event event = new Event();
        event.setShop(shop);
        event.setName(name);
        event.setStartAtUtc(startUtc);
        event.setEndAtUtc(endUtc);
        event.setTimezone(timezone);
        event.setMaxCapacity(maxCapacity);
        event.setStatus("active");
        event.setCreatedAt(Instant.now());
        event = events.save(event);

        return ResponseEntity.status(201).body(Map.of("ok",true,"event",normalizeEvent(event)));
    }

    public ResponseEntity<?> patchShop(Long id, Map<String,Object> data) {
        Shop shop = shops.findById(id).orElse(null);
        if (shop == null) return ResponseEntity.status(404).body(Map.of("success",false));

        if (data.containsKey("name")) {
            String name = clean(data.get("name"));
            if (name == null) return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_name"));
            shop.setName(name);
        }

        if (data.containsKey("addressLine1")) shop.setAddressLine1(clean(data.get("addressLine1")));
        if (data.containsKey("addressLine2")) shop.setAddressLine2(clean(data.get("addressLine2")));
        if (data.containsKey("city")) shop.setCity(clean(data.get("city")));
        if (data.containsKey("region")) shop.setRegion(clean(data.get("region")));
        if (data.containsKey("country")) shop.setCountry(clean(data.get("country")));

        if (data.containsKey("postalCode")) {
            String pc = clean(data.get("postalCode"));
            if (pc != null && !POSTAL.matcher(pc).matches()) {
                return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_postal_code"));
            }
            shop.setPostalCode(pc);
        }

        if (data.containsKey("phone")) {
            String phone = clean(data.get("phone"));
            if (phone != null && !PHONE.matcher(phone).matches()) {
                return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_phone"));
            }
            shop.setPhone(phone);
        }

        if (data.containsKey("email")) {
            String email = clean(data.get("email"));
            if (email != null && !validEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_email"));
            }
            shop.setEmail(email);
        }

        return ResponseEntity.ok(Map.of("ok",true));
    }

    public ResponseEntity<?> deleteShop(Long id) {
        Shop shop = shops.findById(id).orElse(null);
        if (shop == null) return ResponseEntity.status(404).body(Map.of("success",false));

        if (events.countByShop(shop) > 0) {
            return ResponseEntity.status(409).body(Map.of(
                    "success",false,"code","has_events",
                    "message","Ce magasin contient des évènements."
            ));
        }

        shops.delete(shop);
        return ResponseEntity.ok(Map.of("success",true));
    }

    public ResponseEntity<?> patchEvent(Long id, Map<String,Object> data) {
        Event event = events.findById(id).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("ok",false,"error","not_found"));

        if (data.containsKey("name")) {
            String name = clean(data.get("name"));
            if (name == null) return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_name"));
            event.setName(name);
        }

        if (data.containsKey("timezone")) event.setTimezone(clean(data.get("timezone")));

        if (data.containsKey("startsAt")) {
            Object raw = data.get("startsAt");
            if (raw == null || raw.toString().isBlank()) event.setStartAtUtc(null);
            else {
                try { event.setStartAtUtc(Instant.parse(raw.toString())); }
                catch (DateTimeException e) {
                    return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_startsAt"));
                }
            }
        }

        if (data.containsKey("endsAt")) {
            Object raw = data.get("endsAt");
            if (raw == null || raw.toString().isBlank()) event.setEndAtUtc(null);
            else {
                try { event.setEndAtUtc(Instant.parse(raw.toString())); }
                catch (DateTimeException e) {
                    return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_endsAt"));
                }
            }
        }

        if (event.getStartAtUtc() != null && event.getEndAtUtc() != null
                && !event.getEndAtUtc().isAfter(event.getStartAtUtc())) {
            return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_period"));
        }

        if (data.containsKey("isActive")) {
            event.setStatus(bool(data.get("isActive")) ? "active" : "inactive");
        }

        return ResponseEntity.ok(Map.of("ok",true));
    }

    public ResponseEntity<?> deleteEvent(Long id) {
        Event event = events.findById(id).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("ok",false,"error","not_found"));

        // Preserves the order in the PHP source.
        event.setStatus("deleted");

        long count = appointments.countForEvent(event);
        if (count > 0) {
            return ResponseEntity.status(409).body(Map.of(
                    "ok",false,"error","has_appointments",
                    "message","Impossible de supprimer : des rendez-vous sont rattachés à cet évènement.",
                    "count",count
            ));
        }

        events.delete(event);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> patchShopNotes(Long id, Map<String,Object> data) {
        Shop shop = shops.findById(id).orElse(null);
        if (shop == null) return ResponseEntity.status(404).body(Map.of("ok",false,"error","not_found"));

        if (!data.containsKey("notes")) {
            return ResponseEntity.badRequest().body(Map.of("ok",false,"error","missing_notes"));
        }

        Object raw = data.get("notes");
        if (raw != null && !(raw instanceof String)) {
            return ResponseEntity.badRequest().body(Map.of("ok",false,"error","invalid_notes_type"));
        }

        String notes = raw == null ? null : raw.toString().trim();
        if (notes != null && notes.isBlank()) notes = null;

        shop.setNotes(notes);
        String html = notes != null ? markdownRenderer.toHtml(notes) : null;
        shop.setNotesHtml(html);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("ok",true);
        result.put("notes",notes);
        result.put("notesHtml",html != null ? html : "");
        result.put("notesText",markdownRenderer.toText(notes));
        return ResponseEntity.ok(result);
    }

    private Map<String,Object> normalizeShop(Shop s) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",s.getId()); r.put("name",s.getName());
        r.put("addressLine1",s.getAddressLine1()); r.put("addressLine2",s.getAddressLine2());
        r.put("postalCode",s.getPostalCode()); r.put("city",s.getCity());
        r.put("region",s.getRegion()); r.put("country",s.getCountry());
        r.put("phone",s.getPhone()); r.put("email",s.getEmail());
        r.put("notes",s.getNotes()); r.put("isActive",s.getIsActive());
        return r;
    }

    private Map<String,Object> normalizeEvent(Event e) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",e.getId()); r.put("shopId",e.getShop() != null ? e.getShop().getId() : null);
        r.put("name",e.getName()); r.put("startAtUtc",e.getStartAtUtc());
        r.put("endAtUtc",e.getEndAtUtc()); r.put("timezone",e.getTimezone());
        r.put("maxCapacity",e.getMaxCapacity()); r.put("status",e.getStatus());
        return r;
    }

    private static Instant parseInstant(String value, ZoneId zone) {
        try { return Instant.parse(value); } catch (DateTimeException ignored) {}
        try { return OffsetDateTime.parse(value).toInstant(); } catch (DateTimeException ignored) {}
        return LocalDateTime.parse(value).atZone(zone).toInstant();
    }

    private static String clean(Object v) {
        if (v == null) return null;
        String s = v.toString().replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]","").trim();
        return s.isBlank() ? null : s;
    }

    private static Long positiveLong(Object v) {
        if (v == null) return null;
        try {
            long n = Long.parseLong(v.toString());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static Integer optionalInt(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try { return Integer.parseInt(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean bool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return v != null && Boolean.parseBoolean(v.toString());
    }

    private static boolean validEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
