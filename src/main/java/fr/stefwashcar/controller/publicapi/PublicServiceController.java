package fr.stefwashcar.controller.publicapi;

import fr.stefwashcar.repository.ServiceRepository;
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
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
public class PublicServiceController {

    private final PublicCatalogCache cache;
    private final ServiceRepository services;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(cache.getServices());
    }

    @GetMapping("/{servicePublicId:[0-9A-HJKMNP-TV-Z]{26}}")
    public ResponseEntity<?> getOne(@PathVariable String servicePublicId) {
        var service = services.findByPublicId(servicePublicId).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error", "service_not_found"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", service.getId());
        body.put("publicId", service.getPublicId());
        body.put("name", service.getName());
        body.put("durationMin", service.getDurationMin());
        body.put("capacity", service.getCapacity());
        body.put("timezone", service.getTimezone());
        return ResponseEntity.ok(body);
    }
}
