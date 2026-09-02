package fr.stefwashcar.dto.publicapi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Formule commerciale visible publiquement")
public record PublicFormuleResponse(
        String publicId,
        String name,
        int priceCents,
        boolean isActive,
        int sortOrder,
        String description,
        String code,
        Long serviceId,
        String servicePublicId,
        String imagePath,
        String startDay,
        String endDay,
        String duration,
        String slotType,
        String pauseTime,
        int appointmentsCount,
        ColorResponse color
) {
    public record ColorResponse(String value, String cssClass) {}
}
