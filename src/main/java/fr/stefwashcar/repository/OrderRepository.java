package fr.stefwashcar.repository;

import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByAppointment(Appointment appointment);
}
