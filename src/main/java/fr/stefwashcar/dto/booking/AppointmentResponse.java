package fr.stefwashcar.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.stefwashcar.enums.AppointmentStatus;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long serviceId,
        String formulePublicId,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean override
) {
}
