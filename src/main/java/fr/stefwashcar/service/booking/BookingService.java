package fr.stefwashcar.service.booking;

import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.Service;
import fr.stefwashcar.model.User;
import fr.stefwashcar.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingService {
    private final AppointmentRepository appointments;

    public Appointment book(Service service, User user, Instant startsAtUtc) {
        throw new UnsupportedOperationException(
                "BookingService.book is not implemented in the PHP source yet."
        );
    }
}
