package fr.stefwashcar.repository;

import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findFirstByAppointmentAndTemplateAndStatusOrderBySentAtDesc(
            Appointment appointment,
            String template,
            String status
    );
}
