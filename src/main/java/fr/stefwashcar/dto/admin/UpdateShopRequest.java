package fr.stefwashcar.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Champs modifiables d'une boutique ; tous sont facultatifs")
public record UpdateShopRequest(
        @Size(max = 80) @Schema(example = "Studio République") String name,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 16) @Schema(example = "75011") String postalCode,
        @Size(max = 80) @Schema(example = "Paris") String city,
        @Size(max = 60) String region,
        @Size(max = 4) @Schema(example = "FR") String country,
        @Size(max = 32) @Schema(example = "+33102030405") String phone,
        @Email @Size(max = 190) @Schema(example = "studio@example.com") String email
) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "name", name);
        put(values, "addressLine1", addressLine1);
        put(values, "addressLine2", addressLine2);
        put(values, "postalCode", postalCode);
        put(values, "city", city);
        put(values, "region", region);
        put(values, "country", country);
        put(values, "phone", phone);
        put(values, "email", email);
        return values;
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null) values.put(key, value);
    }
}
