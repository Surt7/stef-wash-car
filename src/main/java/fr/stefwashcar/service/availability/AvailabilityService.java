package fr.stefwashcar.service.availability;

import fr.stefwashcar.model.*;
import fr.stefwashcar.repository.*;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AvailabilityService {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final SpecialAvailabilityRepository specialRepository;
    private final BlackoutRepository blackoutRepository;

    public Map<String,Object> getDailyAvailability(Service service, String dateParam) {
        return getDailyAvailability(service, dateParam, null);
    }

    public Map<String,Object> getDailyAvailability(Service service, String dateParam, String timezone) {
        String tzName = notBlank(service.getTimezone())
                ? service.getTimezone()
                : (notBlank(timezone) ? timezone : "Europe/Paris");

        ZoneId zone;
        try {
            zone = ZoneId.of(tzName);
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Paris");
        }

        LocalDate day;
        if (notBlank(dateParam)) {
            try {
                day = LocalDate.parse(dateParam);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("invalid_date", e);
            }
        } else {
            day = LocalDate.now(zone);
        }

        List<RuleWindow> rules = loadRules(service, day);
        if (rules.isEmpty()) {
            Map<String,Object> empty = new LinkedHashMap<>();
            empty.put("serviceId", service.getId());
            empty.put("date", day.toString());
            empty.put("timezone", zone.getId());
            empty.put("slots", List.of());
            return empty;
        }

        ZonedDateTime dayStartLocal = day.atStartOfDay(zone);
        ZonedDateTime dayEndLocal = dayStartLocal.plusDays(1);
        Instant dayStartUtc = dayStartLocal.toInstant();
        Instant dayEndUtc = dayEndLocal.toInstant();

        List<Appointment> appointments = appointmentRepository.findBlockingForAvailability(
                service.getId() != null && service.getId() == 2L,
                dayStartUtc, dayEndUtc
        );

        List<Blackout> blackouts = blackoutRepository.findForAvailabilityDay(
                service, day, dayStartUtc, dayEndUtc
        );

        List<LocalInterval> blackoutIntervals = new ArrayList<>();
        for (Blackout b : blackouts) {
            if (b.getDate() != null || (b.getDateFrom() != null && b.getDateTo() != null)) {
                blackoutIntervals.add(new LocalInterval(dayStartLocal, dayEndLocal));
                continue;
            }

            if (b.getStartAtUtc() != null && b.getEndAtUtc() != null) {
                blackoutIntervals.add(new LocalInterval(
                        b.getStartAtUtc().atZone(zone),
                        b.getEndAtUtc().atZone(zone)
                ));
            }
        }

        Map<String,Boolean> occupied = new HashMap<>();
        for (Appointment a : appointments) {
            occupied.put(HH_MM.format(a.getStartAtUtc().atZone(zone).toLocalTime()), true);
        }

        boolean hasWeddingOnDay = appointmentRepository.hasWeddingOnDay(dayStartUtc, dayEndUtc);
        ZonedDateTime nowLocal = ZonedDateTime.now(zone);
        boolean isToday = day.equals(nowLocal.toLocalDate());

        Integer effectiveStepMin = null;
        Map<String,Map<String,Object>> slotIndex = new LinkedHashMap<>();

        for (RuleWindow rule : rules) {
            int step = rule.stepMin() != null
                    ? rule.stepMin()
                    : service.getDurationMin() != null
                    ? service.getDurationMin().intValue()
                    : 20;

            if (effectiveStepMin == null) effectiveStepMin = step;

            ZonedDateTime current = ZonedDateTime.of(day, rule.startTime(), zone);
            ZonedDateTime end = ZonedDateTime.of(day, rule.endTime(), zone);
            if (!end.isAfter(current)) end = end.plusDays(1);

            while (current.isBefore(end)) {
                if (isToday && !current.isAfter(nowLocal)) {
                    current = current.plusMinutes(step);
                    continue;
                }

                String hhmm = HH_MM.format(current.toLocalTime());
                boolean isOccupied = Boolean.TRUE.equals(occupied.get(hhmm));

                if (hasWeddingOnDay) isOccupied = true;
                if (isInBlackout(current, blackoutIntervals)) isOccupied = true;

                Map<String,Object> candidate = new LinkedHashMap<>();
                candidate.put("time", hhmm);
                candidate.put("startsAt", current.toOffsetDateTime().toString());
                candidate.put("startsAtUtc", current.toInstant().toString());
                candidate.put("available", !isOccupied);

                Map<String,Object> existing = slotIndex.get(hhmm);
                if (existing != null) {
                    existing.put("available",
                            Boolean.TRUE.equals(existing.get("available"))
                                    && Boolean.TRUE.equals(candidate.get("available")));
                } else {
                    slotIndex.put(hhmm, candidate);
                }

                current = current.plusMinutes(step);
            }
        }

        List<Map<String,Object>> slots = new ArrayList<>(slotIndex.values());
        slots.sort(Comparator.comparing(s -> s.get("time").toString()));

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("serviceId", service.getId());
        result.put("date", day.toString());
        result.put("timezone", zone.getId());
        result.put("stepMin", effectiveStepMin != null ? effectiveStepMin : 20);
        result.put("slots", slots);
        return result;
    }

    private List<RuleWindow> loadRules(Service service, LocalDate day) {
        List<SpecialAvailability> special = specialRepository.findForDay(service, day);
        if (!special.isEmpty()) {
            return special.stream().map(r -> new RuleWindow(
                    r.getStartTime(), r.getEndTime(),
                    r.getStepMin() != null ? r.getStepMin().intValue() : null
            )).toList();
        }

        short weekday = (short) day.getDayOfWeek().getValue();
        return ruleRepository.findByServiceAndWeekdayOrderByStartTimeAsc(service, weekday)
                .stream().map(r -> new RuleWindow(
                        r.getStartTime(), r.getEndTime(),
                        r.getStepMin() != null ? r.getStepMin().intValue() : null
                )).toList();
    }

    private boolean isInBlackout(ZonedDateTime slot, List<LocalInterval> intervals) {
        for (LocalInterval i : intervals) {
            if (!slot.isBefore(i.start()) && slot.isBefore(i.end())) return true;
        }
        return false;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record RuleWindow(LocalTime startTime, LocalTime endTime, Integer stepMin) {}
    private record LocalInterval(ZonedDateTime start, ZonedDateTime end) {}
}
