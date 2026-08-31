package fr.stefwashcar.controller;

import fr.stefwashcar.repository.ServiceRepository;
import fr.stefwashcar.service.availability.AvailabilityMonthService;
import fr.stefwashcar.service.availability.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceAvailabilityController {

    private final ServiceRepository services;
    private final AvailabilityService availabilityService;
    private final AvailabilityMonthService availabilityMonthService;

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> day(
            @PathVariable Long id,
            @RequestParam(required = false) String date
    ) {
        var service = services.findById(id).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error", "service_not_found"));
        }

        try {
            return ResponseEntity.ok(availabilityService.getDailyAvailability(service, date));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_date"));
        }
    }

    @GetMapping("/{id}/availability/month")
    public ResponseEntity<?> month(
            @PathVariable Long id,
            @RequestParam(required = false) String date
    ) {
        final YearMonth ym;
        try {
            ym = YearMonth.parse(date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing/invalid ?date=YYYY-MM"));
        }

        var fromUtc = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var toUtc = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var days = availabilityMonthService.getMonthAvailability(id, fromUtc, toUtc);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("month", date);
        body.put("serviceId", id);
        body.put("days", days);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/availability/range")
    public ResponseEntity<?> range(
            @PathVariable Long id,
            @RequestParam(required = false, name = "from") String from,
            @RequestParam(defaultValue = "12") int months
    ) {
        final YearMonth ym;
        try {
            ym = YearMonth.parse(from);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing/invalid ?from=YYYY-MM"));
        }

        if (months <= 0 || months > 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid months (1..12)"));
        }

        var fromUtc = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var toUtc = ym.plusMonths(months).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var days = availabilityMonthService.getMonthAvailability(id, fromUtc, toUtc);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", ym.atDay(1).toString());
        body.put("to", ym.plusMonths(months).atDay(1).toString());
        body.put("serviceId", id);
        body.put("days", days);
        return ResponseEntity.ok(body);
    }
}
