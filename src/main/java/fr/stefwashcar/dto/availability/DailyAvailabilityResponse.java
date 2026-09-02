package fr.stefwashcar.dto.availability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Disponibilités d'un service pour une journée")
public record DailyAvailabilityResponse(
        Long serviceId,
        @Schema(example = "2026-09-10") String date,
        @Schema(example = "Europe/Paris") String timezone,
        Integer stepMin,
        List<AvailabilitySlotResponse> slots
) {}
