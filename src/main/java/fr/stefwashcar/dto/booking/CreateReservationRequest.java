package fr.stefwashcar.dto.booking;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateReservationRequest(
        @NotNull Long serviceId,
        @NotNull Instant startsAt,
        @NotBlank @Pattern(regexp = "^[a-zA-Z]{2,60}$") String lastName,
        @NotBlank @Pattern(regexp = "^[a-zA-Z]{2,60}$") String firstName,
        @NotBlank @Email String email,
        String phone,
        @NotNull @AssertTrue Boolean gdprConsent,
        @NotNull Boolean isManualOverride,
        @NotBlank @Size(max = 36) String idempotencyKey,
        String eventPublicId,
        String formulePublicId
) {
    public CreateReservationRequest {
        lastName = trimToNull(lastName);
        firstName = trimToNull(firstName);
        email = trimToNull(email);
        phone = trimToNull(phone);
        idempotencyKey = trimToNull(idempotencyKey);
        eventPublicId = trimToNull(eventPublicId);
        formulePublicId = trimToNull(formulePublicId);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
