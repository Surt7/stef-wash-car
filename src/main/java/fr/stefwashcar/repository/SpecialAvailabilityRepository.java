package fr.stefwashcar.repository;

import fr.stefwashcar.model.Service;
import fr.stefwashcar.model.SpecialAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public interface SpecialAvailabilityRepository extends JpaRepository<SpecialAvailability, Long> {

    @Query("""
        select s from SpecialAvailability s
        where s.service = :service
          and s.dateFrom <= :day
          and coalesce(s.dateTo, s.dateFrom) >= :day
        order by s.startTime asc
        """)
    List<SpecialAvailability> findForDay(
            @Param("service") Service service,
            @Param("day") LocalDate day
    );

    @Query("""
        select s from SpecialAvailability s
        where s.service = :service
          and s.dateFrom <= :toDateInclusive
          and (s.dateTo is null or s.dateTo >= :fromDate)
        order by s.dateFrom asc, s.startTime asc
        """)
    List<SpecialAvailability> findForDatePeriod(
            @Param("service") Service service,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDateInclusive") LocalDate toDateInclusive
    );

    default List<SpecialAvailability> findForPeriod(
            Service service, Instant fromUtc, Instant toUtc) {
        LocalDate fromDate = fromUtc.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDateInclusive = toUtc.atZone(ZoneOffset.UTC).toLocalDate().minusDays(1);
        return findForDatePeriod(service, fromDate, toDateInclusive);
    }
}
