package fr.stefwashcar.service;

import fr.stefwashcar.model.Service;
import fr.stefwashcar.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminAppointmentSlotChecker {
    private final AppointmentRepository appointments;

    public Map<String,Object> check(Service service, String dateYmd, String timeHm, Long excludeAppointmentId) {
        return check(service, dateYmd, timeHm, excludeAppointmentId, 20, "08:00", "20:00");
    }

    public Map<String,Object> check(Service service, String dateYmd, String timeHm,
                                    Long excludeAppointmentId, int stepMin,
                                    String dayStart, String dayEnd) {
        ZoneId zone;
        try {
            zone = ZoneId.of(service.getTimezone() != null ? service.getTimezone() : "Europe/Paris");
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        if (!isAligned(timeHm, stepMin)) {
            return result("outOfRule", timeHm, "not_aligned");
        }

        try {
            LocalTime time = LocalTime.parse(timeHm);
            LocalTime startRange = LocalTime.parse(dayStart);
            LocalTime endRange = LocalTime.parse(dayEnd);

            if (time.isBefore(startRange) || !time.isBefore(endRange)) {
                return result("outOfRule", timeHm, "outside_range");
            }

            LocalDate day = LocalDate.parse(dateYmd);
            Instant startUtc = day.atTime(time).atZone(zone).toInstant();
            Instant endUtc = day.atTime(time).plusMinutes(stepMin).atZone(zone).toInstant();

            long overlapCount = appointments.countOverlapsForService(
                    service, startUtc, endUtc, excludeAppointmentId
            );

            return result(overlapCount > 0 ? "busy" : "free", timeHm, null);
        } catch (DateTimeException e) {
            return result("outOfRule", timeHm, "invalid_datetime");
        }
    }

    private boolean isAligned(String timeHm, int stepMin) {
        if (timeHm == null || !timeHm.matches("\\d{2}:\\d{2}") || stepMin <= 0) return false;
        try {
            return LocalTime.parse(timeHm).getMinute() % stepMin == 0;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private Map<String,Object> result(String status, String time, String reason) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("time", time);
        if (reason != null) result.put("reason", reason);
        return result;
    }
}
