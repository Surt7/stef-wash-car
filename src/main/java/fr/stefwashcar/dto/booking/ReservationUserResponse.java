package fr.stefwashcar.dto.booking;

public record ReservationUserResponse(
        Long id,
        String lastName,
        String firstName,
        String email
) {
}
