package fr.stefwashcar.service;

import fr.stefwashcar.model.Service;
import fr.stefwashcar.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.util.*;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminSlotProvider {
    private final AppointmentRepository appointments;

    public List<Map<String,Object>> getSlotsForDay(Service service, LocalDate day) {
        ZoneId zone;
        try {
            zone = ZoneId.of(service.getTimezone() != null ? service.getTimezone() : "Europe/Paris");
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        ZonedDateTime startLocal = day.atTime(8,0).atZone(zone);
        ZonedDateTime endLocal = day.atTime(20,0).atZone(zone);

        var existing = appointments.findForServiceBetween(service, startLocal.toInstant(), endLocal.toInstant());
        List<Map<String,Object>> slots = new ArrayList<>();

        for (ZonedDateTime cursor = startLocal; cursor.isBefore(endLocal); cursor = cursor.plusMinutes(20)) {
            Instant slotStart = cursor.toInstant();
            Instant slotEnd = cursor.plusMinutes(20).toInstant();

            boolean busy = existing.stream().anyMatch(a ->
                    a.getStartAtUtc().isBefore(slotEnd) && a.getEndAtUtc().isAfter(slotStart)
            );

            Map<String,Object> slot = new LinkedHashMap<>();
            slot.put("time", cursor.toLocalTime().withSecond(0).withNano(0).toString());
            slot.put("status", busy ? "busy" : "free");
            slots.add(slot);
        }

        return slots;
    }
}
