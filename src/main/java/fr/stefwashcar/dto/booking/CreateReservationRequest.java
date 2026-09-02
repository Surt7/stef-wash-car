package fr.stefwashcar.dto.booking;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CreateReservationRequest(
        @NotNull Long serviceId,
        @NotNull Instant startsAt,
        @NotBlank @Size(max = 60)
        @Pattern(regexp = "^[\\p{L}][\\p{L} '\u2019-]*[\\p{L}]$") String lastName,
        @NotBlank @Size(max = 60)
        @Pattern(regexp = "^[\\p{L}][\\p{L} '\u2019-]*[\\p{L}]$") String firstName,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 30) String phone,
        @NotNull @AssertTrue Boolean gdprConsent,
        @Schema(description = "Ignoré pour une réservation publique", deprecated = true)
        Boolean isManualOverride,
        @NotBlank @Size(max = 36) String idempotencyKey,
        @Pattern(regexp = "^[0-9A-HJKMNP-TV-Z]{26}$") String eventPublicId,
        @Pattern(regexp = "^[0-9A-HJKMNP-TV-Z]{26}$") String formulePublicId
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
