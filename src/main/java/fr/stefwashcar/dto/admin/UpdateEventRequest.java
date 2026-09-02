package fr.stefwashcar.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Champs modifiables d'un évènement ; tous sont facultatifs")
public record UpdateEventRequest(
        @Size(max = 120) String name,
        @Size(max = 50) @Schema(example = "Europe/Paris") String timezone,
        @Schema(example = "2026-09-15T07:00:00Z") Instant startsAt,
        @Schema(example = "2026-09-15T16:00:00Z") Instant endsAt,
        Boolean isActive
) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "name", name);
        put(values, "timezone", timezone);
        put(values, "startsAt", startsAt);
        put(values, "endsAt", endsAt);
        put(values, "isActive", isActive);
        return values;
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null) values.put(key, value);
    }
}
