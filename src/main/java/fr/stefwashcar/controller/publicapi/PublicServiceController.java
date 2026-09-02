package fr.stefwashcar.controller.publicapi;

import fr.stefwashcar.repository.ServiceRepository;
import fr.stefwashcar.service.publicapi.PublicCatalogCache;
import fr.stefwashcar.dto.publicapi.PublicServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
public class PublicServiceController {

    private final PublicCatalogCache cache;
    private final ServiceRepository services;

    @GetMapping
    @Operation(summary = "Lister les services publics")
    public ResponseEntity<List<PublicServiceResponse>> list() {
        return ResponseEntity.ok(cache.getServices());
    }

    @GetMapping("/{servicePublicId:[0-9A-HJKMNP-TV-Z]{26}}")
    @Operation(summary = "Consulter un service public")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PublicServiceResponse.class)))
    public ResponseEntity<?> getOne(@PathVariable String servicePublicId) {
        var service = services.findByPublicId(servicePublicId).orElse(null);
        if (service == null) {
            return ResponseEntity.status(404).body(Map.of("error", "service_not_found"));
        }

        return ResponseEntity.ok(cache.toServiceResponse(service));
    }
}
