package fr.stefwashcar.dto.publicapi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Service réservable visible publiquement")
public record PublicServiceResponse(
        Long id,
        @Schema(example = "01JABCDEF0123456789ABCDEFG") String publicId,
        @Schema(example = "Lavage complet") String name,
        @Schema(example = "45") Short durationMin,
        @Schema(example = "1") Short capacity,
        @Schema(example = "Europe/Paris") String timezone
) {}
