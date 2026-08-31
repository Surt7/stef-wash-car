package fr.stefwashcar.controller.publicapi;

import fr.stefwashcar.repository.FormuleRepository;
import fr.stefwashcar.service.publicapi.PublicCatalogCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/formules")
@RequiredArgsConstructor
public class PublicFormuleController {

    private final FormuleRepository formules;
    private final PublicCatalogCache cache;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(cache.getActiveFormules());
    }

    @GetMapping("/{formulePublicId:[0-9A-HJKMNP-TV-Z]{26}}")
    public ResponseEntity<?> getOne(@PathVariable String formulePublicId) {
        var formule = formules.findByPublicId(formulePublicId).orElse(null);
        if (formule == null) {
            return ResponseEntity.status(404).body(Map.of("error", "formule_not_found"));
        }

        var service = formule.getService();

        Map<String, Object> color = null;
        if (formule.getColor() != null) {
            color = new LinkedHashMap<>();
            color.put("value", formule.getColor().getValue());
            color.put("cssClass", formule.getColor().getCssClass());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", formule.getId());
        body.put("publicId", formule.getPublicId());
        body.put("name", formule.getName());
        body.put("priceCents", formule.getPriceCents());
        body.put("serviceId", service.getId());
        body.put("servicePublicId", service.getPublicId());
        body.put("isActive", formule.isActive());
        body.put("description", formule.getDescription());
        body.put("sortOrder", formule.getSortOrder());
        body.put("code", formule.getCode());
        body.put("imagePath", formule.getImagePath());
        body.put("startDay", formatTime(formule.getStartDay()));
        body.put("endDay", formatTime(formule.getEndDay()));
        body.put("duration", formatTime(formule.getDuration()));
        body.put("slotType", formule.getSlotType());
        body.put("pauseTime", formatTime(formule.getPauseTime()));
        body.put("color", color);

        return ResponseEntity.ok(body);
    }

    private String formatTime(java.time.LocalTime value) {
        return value == null ? null : value.withNano(0).toString();
    }
}
