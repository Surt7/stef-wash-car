package fr.stefwashcar.service;

import fr.stefwashcar.dto.booking.CreateReservationRequest;
import fr.stefwashcar.dto.booking.ReservationResponse;
import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.Service;
import fr.stefwashcar.model.User;
import fr.stefwashcar.repository.AppointmentRepository;
import fr.stefwashcar.repository.EventRepository;
import fr.stefwashcar.repository.FormuleRepository;
import fr.stefwashcar.repository.ServiceRepository;
import fr.stefwashcar.repository.UserRepository;
import fr.stefwashcar.service.booking.AppointmentConfirmationMailer;
import fr.stefwashcar.service.booking.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    private static final long SERVICE_ID = 1L;
    private static final String CUSTOMER_EMAIL = "jean.dupont@example.com";

    @Mock
    private AppointmentRepository appointments;

    @Mock
    private ServiceRepository services;

    @Mock
    private FormuleRepository formules;

    @Mock
    private UserRepository users;

    @Mock
    private EventRepository events;

    @Mock
    private AppointmentConfirmationMailer confirmationMailer;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldReturnExistingAppointmentWhenIdempotencyKeyWasAlreadyUsed() {
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440000";

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(42L);
        existingAppointment.setIdempotencyKey(idempotencyKey);

        CreateReservationRequest request = validReservationRequest(
                idempotencyKey,
                Instant.parse("2026-09-10T08:00:00Z")
        );

        when(appointments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingAppointment));

        var response = reservationService.createReservation(
                request,
                "127.0.0.1"
        );

        assertEquals(200, response.getStatusCode().value());
        ReservationResponse body =
                assertInstanceOf(ReservationResponse.class, response.getBody());
        assertEquals(42L, body.appointment().id());

        verify(appointments).findByIdempotencyKey(idempotencyKey);
        verify(appointments, never()).save(any(Appointment.class));
        verify(users, never()).save(any(User.class));
        verify(confirmationMailer, never()).send(any(Appointment.class));
    }

    @Test
    void shouldCreateAndReturnAppointmentWhenSlotIsAvailable() {
        // Arrange
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440001";
        Instant startAtUtc = Instant.parse("2026-09-10T08:00:00Z");
        Instant endAtUtc = startAtUtc.plus(Duration.ofMinutes(45));

        Service service = serviceWithDuration(SERVICE_ID, 45);
        CreateReservationRequest request =
                validReservationRequest(idempotencyKey, startAtUtc);

        when(appointments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(services.findById(SERVICE_ID))
                .thenReturn(Optional.of(service));
        when(appointments.findBlockingConflict(
                service,
                startAtUtc,
                endAtUtc))
                .thenReturn(Optional.empty());
        when(users.findByEmail(CUSTOMER_EMAIL))
                .thenReturn(Optional.empty());
        repositoryAssignsIdToSavedUser(7L);
        repositoryAssignsIdToSavedAppointment(42L);

        // Act
        var response = reservationService.createReservation(
                request,
                "127.0.0.1"
        );

        // Assert: réponse retournée par le SUT
        assertEquals(201, response.getStatusCode().value());

        ReservationResponse body =
                assertInstanceOf(ReservationResponse.class, response.getBody());
        assertEquals(true, body.ok());
        assertEquals(42L, body.appointment().id());
        assertEquals(startAtUtc, body.appointment().startsAt());
        assertEquals(endAtUtc, body.appointment().endsAt());
        assertEquals(AppointmentStatus.confirmed, body.appointment().status());

        // Assert: objet construit et envoyé au repository
        ArgumentCaptor<Appointment> appointmentCaptor =
                ArgumentCaptor.forClass(Appointment.class);
        verify(appointments).save(appointmentCaptor.capture());

        Appointment createdAppointment = appointmentCaptor.getValue();
        assertSame(service, createdAppointment.getService());
        assertEquals(startAtUtc, createdAppointment.getStartAtUtc());
        assertEquals(endAtUtc, createdAppointment.getEndAtUtc());
        assertEquals(AppointmentStatus.confirmed, createdAppointment.getStatus());
        assertEquals(idempotencyKey, createdAppointment.getIdempotencyKey());
        assertFalse(createdAppointment.getIsManualOverride());
        assertNotNull(createdAppointment.getCreatedAt());
        assertNotNull(createdAppointment.getCancelToken());

        verify(confirmationMailer).send(createdAppointment);
    }

    private CreateReservationRequest validReservationRequest(
            String idempotencyKey,
            Instant startAtUtc
    ) {
        return new CreateReservationRequest(
                SERVICE_ID,
                startAtUtc,
                "Dupont",
                "Jean",
                CUSTOMER_EMAIL,
                null,
                true,
                false,
                idempotencyKey,
                null,
                null
        );
    }

    private Service serviceWithDuration(long id, int durationMinutes) {
        Service service = new Service();
        service.setId(id);
        service.setDurationMin((short) durationMinutes);
        return service;
    }

    private void repositoryAssignsIdToSavedUser(long id) {
        when(users.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(id);
                    return user;
                });
    }

    private void repositoryAssignsIdToSavedAppointment(long id) {
        when(appointments.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment appointment = invocation.getArgument(0);
                    appointment.setId(id);
                    return appointment;
                });
    }
}
