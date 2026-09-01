package fr.stefwashcar.dto.booking;

public record ReservationResponse(
        boolean ok,
        AppointmentResponse appointment,
        ReservationUserResponse user
) {
}
