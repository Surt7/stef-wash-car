package fr.stefwashcar.controller.publicapi;

import fr.stefwashcar.service.publicapi.PublicCatalogCache;
import fr.stefwashcar.dto.publicapi.PublicFormuleResponse;
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
@RequestMapping("/api/public/formules")
@RequiredArgsConstructor
public class PublicFormuleController {

    private final PublicCatalogCache cache;

    @GetMapping
    @Operation(summary = "Lister les formules publiques actives")
    public ResponseEntity<List<PublicFormuleResponse>> list() {
        return ResponseEntity.ok(cache.getActiveFormules());
    }

    @GetMapping("/{formulePublicId:[0-9A-HJKMNP-TV-Z]{26}}")
    @Operation(summary = "Consulter une formule publique")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PublicFormuleResponse.class)))
    public ResponseEntity<?> getOne(@PathVariable String formulePublicId) {
        var formule = cache.getFormule(formulePublicId);
        if (formule.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "formule_not_found"));
        }

        return ResponseEntity.ok(formule.get());
    }
}
