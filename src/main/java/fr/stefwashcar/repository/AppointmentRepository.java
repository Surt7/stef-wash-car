package fr.stefwashcar.repository;

import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.Event;
import fr.stefwashcar.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Set<AppointmentStatus> BLOCKING_STATUSES =
            EnumSet.of(AppointmentStatus.confirmed, AppointmentStatus.pending);

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);

    Optional<Appointment> findByCancelToken(String cancelToken);

    Optional<Appointment> findFirstByServiceAndStatusInAndStartAtUtcLessThanAndEndAtUtcGreaterThan(
            Service service,
            Set<AppointmentStatus> statuses,
            Instant endUtc,
            Instant startUtc
    );

    default Optional<Appointment> findBlockingConflict(
            Service service,
            Instant startUtc,
            Instant endUtc
    ) {
        return findFirstByServiceAndStatusInAndStartAtUtcLessThanAndEndAtUtcGreaterThan(
                service,
                BLOCKING_STATUSES,
                endUtc,
                startUtc
        );
    }

    List<Appointment> findByServiceAndStatusAndStartAtUtcGreaterThanEqualAndStartAtUtcLessThanOrderByStartAtUtcAsc(
            Service service,
            AppointmentStatus status,
            Instant fromUtc,
            Instant toUtc
    );

    default List<Appointment> findForServiceBetween(
            Service service,
            Instant fromUtc,
            Instant toUtc
    ) {
        return findByServiceAndStatusAndStartAtUtcGreaterThanEqualAndStartAtUtcLessThanOrderByStartAtUtcAsc(
                service,
                AppointmentStatus.confirmed,
                fromUtc,
                toUtc
        );
    }

    default List<Appointment> findConfirmedForPeriod(
            Service service,
            Instant fromUtc,
            Instant toUtc
    ) {
        return findForServiceBetween(service, fromUtc, toUtc);
    }

    @Query("""
        select a
        from Appointment a
        left join fetch a.user u
        where a.startAtUtc >= :fromUtc
          and a.startAtUtc < :toUtc
          and a.status = :status
          and (:serviceId is null or a.service.id = :serviceId)
        order by a.startAtUtc asc
        """)
    List<Appointment> findConfirmedBetween(
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc,
            @Param("serviceId") Long serviceId,
            @Param("status") AppointmentStatus status
    );

    default List<Appointment> findConfirmedBetween(
            Instant fromUtc,
            Instant toUtc,
            Long serviceId
    ) {
        return findConfirmedBetween(
                fromUtc,
                toUtc,
                serviceId,
                AppointmentStatus.confirmed
        );
    }

    @Query("""
        select a
        from Appointment a
        where a.service = :service
          and a.startAtUtc < :toUtc
          and a.endAtUtc > :fromUtc
          and a.status in :statuses
        """)
    List<Appointment> findActiveForServiceBetween(
            @Param("service") Service service,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc,
            @Param("statuses") Set<AppointmentStatus> statuses
    );

    default List<Appointment> findActiveForServiceBetween(
            Service service,
            Instant fromUtc,
            Instant toUtc
    ) {
        return findActiveForServiceBetween(
                service,
                fromUtc,
                toUtc,
                BLOCKING_STATUSES
        );
    }

    @Query("""
        select count(a)
        from Appointment a
        where a.provider.id = :providerId
          and a.startAtUtc < :endUtc
          and a.endAtUtc > :startUtc
        """)
    long countProviderOverlaps(
            @Param("providerId") Long providerId,
            @Param("startUtc") Instant startUtc,
            @Param("endUtc") Instant endUtc
    );

    default boolean hasOverlap(
            long providerId,
            Instant startUtc,
            Instant endUtc
    ) {
        return countProviderOverlaps(providerId, startUtc, endUtc) > 0;
    }

    @Query("""
        select count(a)
        from Appointment a
        where a.service = :service
          and a.startAtUtc < :endUtc
          and a.endAtUtc > :startUtc
          and (:excludeAppointmentId is null or a.id <> :excludeAppointmentId)
        """)
    long countOverlapsForService(
            @Param("service") Service service,
            @Param("startUtc") Instant startUtc,
            @Param("endUtc") Instant endUtc,
            @Param("excludeAppointmentId") Long excludeAppointmentId
    );

    long countByEvent(Event event);

    default long countForEvent(Event event) {
        return countByEvent(event);
    }

    @Query("""
        select count(a)
        from Appointment a
        where a.status in :statuses
          and a.service.id = 2
          and a.startAtUtc < :dayEndUtc
          and a.endAtUtc > :dayStartUtc
        """)
    long countWeddingOnDay(
            @Param("statuses") Set<AppointmentStatus> statuses,
            @Param("dayStartUtc") Instant dayStartUtc,
            @Param("dayEndUtc") Instant dayEndUtc
    );

    default boolean hasWeddingOnDay(
            Instant dayStartUtc,
            Instant dayEndUtc
    ) {
        return countWeddingOnDay(
                BLOCKING_STATUSES,
                dayStartUtc,
                dayEndUtc
        ) > 0;
    }

    default Map<String, Object> findAdminViewByDateParam(
            String dateParam,
            long serviceId,
            String timezone
    ) {
        ZoneId zone = ZoneId.of(timezone);

        ZonedDateTime fromLocal;
        ZonedDateTime toLocal;

        if (dateParam.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate day = LocalDate.parse(dateParam);
            fromLocal = day.atStartOfDay(zone);
            toLocal = fromLocal.plusDays(1);
        } else if (dateParam.matches("\\d{4}-\\d{2}")) {
            YearMonth month = YearMonth.parse(dateParam);
            fromLocal = month.atDay(1).atStartOfDay(zone);
            toLocal = fromLocal.plusMonths(1);
        } else {
            throw new IllegalArgumentException("Invalid date format");
        }

        List<Appointment> appointments = findConfirmedBetween(
                fromLocal.toInstant(),
                toLocal.toInstant(),
                serviceId
        );

        List<Map<String, Object>> rows = appointments.stream()
                .map(appointment -> {
                    ZonedDateTime localStart = appointment.getStartAtUtc().atZone(zone);
                    var user = appointment.getUser();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("horaire", localStart.toLocalTime().toString().substring(0, 5));
                    row.put("prénom", user != null ? user.getFirstname() : null);
                    row.put("nom", user != null ? user.getLastname() : null);
                    row.put("email", user != null ? user.getEmail() : null);
                    row.put("phone", user != null ? user.getPhone() : null);
                    return row;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dateSelectionnee", dateParam);
        result.put("nbRendezVous", rows.size());
        result.put("rendezVous", rows);
        return result;
    }

    default Map<String, Object> normalizeOverrideAppointment(Appointment appointment) {
        Event event = appointment.getEvent();
        var shop = event != null ? event.getShop() : null;

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("hasEvent", event != null);
        context.put("hasShop", shop != null);

        if (event != null) {
            Map<String, Object> eventData = new LinkedHashMap<>();
            eventData.put("id", event.getId());
            eventData.put("name", event.getName());
            context.put("event", eventData);
        } else {
            context.put("event", null);
        }

        if (shop != null) {
            Map<String, Object> shopData = new LinkedHashMap<>();
            shopData.put("id", shop.getId());
            shopData.put("name", shop.getName());
            shopData.put("city", shop.getCity());
            context.put("shop", shopData);
        } else {
            context.put("shop", null);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", appointment.getId());
        result.put("serviceId", appointment.getService() != null ? appointment.getService().getId() : null);
        result.put("providerId", appointment.getProvider() != null ? appointment.getProvider().getId() : null);
        result.put("userId", appointment.getUser() != null ? appointment.getUser().getId() : null);
        result.put("startAtUtc", appointment.getStartAtUtc());
        result.put("endAtUtc", appointment.getEndAtUtc());
        result.put("status", appointment.getStatus() != null ? appointment.getStatus().name() : null);
        result.put("notes", appointment.getNotes());
        result.put("override", true);
        result.put("idempotencyKey", appointment.getIdempotencyKey());
        result.put("context", context);

        return result;
    }

    @Query("""
        select a from Appointment a
        where (
              (:wedding = true and a.service.id = 2)
              or
              (:wedding = false and a.service.id <> 2)
        )
          and a.startAtUtc < :dayEndUtc
          and a.endAtUtc > :dayStartUtc
          and a.status in :statuses
        """)
    List<Appointment> findBlockingForAvailability(
            @Param("wedding") boolean wedding,
            @Param("dayStartUtc") Instant dayStartUtc,
            @Param("dayEndUtc") Instant dayEndUtc,
            @Param("statuses") Set<AppointmentStatus> statuses
    );

    default List<Appointment> findBlockingForAvailability(
            boolean wedding, Instant dayStartUtc, Instant dayEndUtc) {
        return findBlockingForAvailability(
                wedding, dayStartUtc, dayEndUtc, BLOCKING_STATUSES
        );
    }

}
