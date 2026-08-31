package fr.stefwashcar.service.availability;

import fr.stefwashcar.model.Service;
import fr.stefwashcar.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AvailabilityMonthService {
    private final ServiceRepository services;
    private final AvailabilityService availabilityService;

    public List<Map<String,Object>> getMonthAvailability(long serviceId, Instant fromUtc, Instant toUtc) {
        Service service = services.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("service_not_found"));

        ZoneId zone;
        try {
            zone = ZoneId.of(service.getTimezone() != null ? service.getTimezone() : "Europe/Paris");
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        List<Map<String,Object>> days = new ArrayList<>();

        for (Instant cursor = fromUtc; cursor.isBefore(toUtc); cursor = cursor.plus(1, ChronoUnit.DAYS)) {
            String dayKey = cursor.atZone(zone).toLocalDate().toString();
            Map<String,Object> daily = availabilityService.getDailyAvailability(service, dayKey, zone.getId());

            boolean hasAvailability = false;
            if (daily.get("slots") instanceof List<?> slots) {
                for (Object item : slots) {
                    if (item instanceof Map<?,?> slot && Boolean.TRUE.equals(slot.get("available"))) {
                        hasAvailability = true;
                        break;
                    }
                }
            }

            Map<String,Object> row = new LinkedHashMap<>();
            row.put("date", dayKey);
            row.put("hasAvailability", hasAvailability);
            days.add(row);
        }

        return days;
    }
}
