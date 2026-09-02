package fr.stefwashcar.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Données nécessaires à la création d'un évènement")
public record CreateEventRequest(
        @NotNull @Positive @Schema(example = "1") Long shopId,
        @NotBlank @Size(max = 120) @Schema(example = "Journée lavage premium") String name,
        @Schema(example = "Europe/Paris", defaultValue = "Europe/Paris") String timezone,
        @NotBlank @Schema(example = "2026-09-15T09:00:00+02:00") String startAt,
        @NotBlank @Schema(example = "2026-09-15T18:00:00+02:00") String endAt,
        @Positive @Schema(example = "20") Integer maxCapacity
) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "shopId", shopId);
        put(values, "name", name);
        put(values, "timezone", timezone);
        put(values, "startAt", startAt);
        put(values, "endAt", endAt);
        put(values, "maxCapacity", maxCapacity);
        return values;
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null) values.put(key, value);
    }
}
