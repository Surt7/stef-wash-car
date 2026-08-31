package fr.stefwashcar.repository;

import fr.stefwashcar.model.AvailabilityRule;
import fr.stefwashcar.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    List<AvailabilityRule> findByServiceAndWeekdayOrderByStartTimeAsc(
            Service service,
            Short weekday
    );

    List<AvailabilityRule> findByServiceOrderByWeekdayAscStartTimeAsc(Service service);
}
