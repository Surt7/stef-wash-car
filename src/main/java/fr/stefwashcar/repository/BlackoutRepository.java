package fr.stefwashcar.repository;

import fr.stefwashcar.model.Blackout;
import fr.stefwashcar.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface BlackoutRepository extends JpaRepository<Blackout, Long> {

    @Query("""
        select b from Blackout b
        where b.service = :service
          and (
                (b.date is not null and b.date = :dayDate)
                or
                (b.dateFrom is not null and b.dateFrom <= :dayDate
                 and b.dateTo is not null and b.dateTo >= :dayDate)
                or
                (b.date is null and b.dateFrom is null
                 and b.startAtUtc < :dayEndUtc and b.endAtUtc > :dayStartUtc)
              )
        """)
    List<Blackout> findForAvailabilityDay(
            @Param("service") Service service,
            @Param("dayDate") LocalDate dayDate,
            @Param("dayStartUtc") Instant dayStartUtc,
            @Param("dayEndUtc") Instant dayEndUtc
    );

    @Query("""
        select b from Blackout b
        where b.service = :service
          and (
                (b.date is not null and b.date = :dayDate)
                or
                (b.date is null and b.startAtUtc < :dayEndUtc and b.endAtUtc > :dayStartUtc)
              )
        """)
    List<Blackout> findForServiceAndDay(
            @Param("service") Service service,
            @Param("dayDate") LocalDate dayDate,
            @Param("dayStartUtc") Instant dayStartUtc,
            @Param("dayEndUtc") Instant dayEndUtc
    );

    @Query("""
        select b from Blackout b
        where b.service = :service
          and (
                (b.startAtUtc is not null and b.endAtUtc is not null
                 and b.startAtUtc < :toUtc and b.endAtUtc > :fromUtc)
                or
                (b.date is not null and b.date >= :fromDate and b.date < :toDate)
              )
        order by b.id asc
        """)
    List<Blackout> findForPeriod(
            @Param("service") Service service,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDateExclusive
    );

    default List<Blackout> findForPeriod(
            Service service, Instant fromUtc, Instant toUtc,
            String fromLocalDate, String toLocalDateExclusive) {
        return findForPeriod(
                service, fromUtc, toUtc,
                LocalDate.parse(fromLocalDate),
                LocalDate.parse(toLocalDateExclusive)
        );
    }
}
