package fr.stefwashcar.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

@Schema(description = "Notes Markdown d'une boutique ; null efface les notes")
public record UpdateShopNotesRequest(
        @Schema(example = "Accès par le parking **niveau -1**.", nullable = true) String notes
) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        values.put("notes", notes);
        return values;
    }
}
