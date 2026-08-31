package fr.stefwashcar.model;

import fr.stefwashcar.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AppointmentTests {
    @Test
    void shouldConvertParisWinterTimeToUtc() {
        // Arrange
        Service service = new Service();
        service.setTimezone("Europe/Paris");

        LocalDateTime localSlot =
                LocalDateTime.of(2020, 1, 15, 14, 0);

        Instant expectedStartUtc =
                Instant.parse("2020-01-15T13:00:00Z");
        // Act
        Appointment appointment = Appointment.fromLocalSlot(service, localSlot);

        // Assert
        assertEquals(expectedStartUtc, appointment.getStartAtUtc());
    }

    @Test
    void shouldConvertParisSummerTimeToUtc() {
        Service service = serviceInParisWithDuration(45);

        Appointment appointment = Appointment.fromLocalSlot(
                service,
                LocalDateTime.of(2026, 7, 15, 14, 0)
        );

        assertEquals(
                Instant.parse("2026-07-15T12:00:00Z"),
                appointment.getStartAtUtc()
        );
    }

    @Test
    void shouldCalculateEndFromServiceDuration() {
        Service service = serviceInParisWithDuration(45);

        Appointment appointment = Appointment.fromLocalSlot(
                service,
                LocalDateTime.of(2026, 1, 15, 14, 0)
        );

        assertEquals(
                Duration.ofMinutes(45),
                Duration.between(
                        appointment.getStartAtUtc(),
                        appointment.getEndAtUtc()
                )
        );
    }

    @Test
    void shouldUseParisAndTwentyMinutesByDefault() {
        Service service = new Service();
        service.setTimezone(" ");
        service.setDurationMin(null);

        Appointment appointment = Appointment.fromLocalSlot(
                service,
                LocalDateTime.of(2026, 1, 15, 14, 0)
        );

        assertEquals(
                Instant.parse("2026-01-15T13:00:00Z"),
                appointment.getStartAtUtc()
        );
        assertEquals(
                Instant.parse("2026-01-15T13:20:00Z"),
                appointment.getEndAtUtc()
        );
    }

    @Test
    void shouldCreateConfirmedAppointmentForService() {
        Service service = serviceInParisWithDuration(45);

        Appointment appointment = Appointment.fromLocalSlot(
                service,
                LocalDateTime.of(2026, 1, 15, 14, 0)
        );

        assertSame(service, appointment.getService());
        assertEquals(AppointmentStatus.confirmed, appointment.getStatus());
    }

    private Service serviceInParisWithDuration(int durationMinutes) {
        Service service = new Service();
        service.setTimezone("Europe/Paris");
        service.setDurationMin((short) durationMinutes);
        return service;
    }
}
