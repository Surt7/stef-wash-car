package fr.stefwashcar.dto.availability;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailabilitySlotResponse(
        @Schema(example = "10:20") String time,
        @Schema(example = "2026-09-10T10:20:00+02:00") String startsAt,
        @Schema(example = "2026-09-10T08:20:00Z") String startsAtUtc,
        boolean available
) {}
