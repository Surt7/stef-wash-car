package fr.stefwashcar.controller.admin;

import fr.stefwashcar.config.OpenApiConfig;
import fr.stefwashcar.dto.admin.CreateEventRequest;
import fr.stefwashcar.dto.admin.CreateShopRequest;
import fr.stefwashcar.dto.admin.UpdateEventRequest;
import fr.stefwashcar.dto.admin.UpdateShopNotesRequest;
import fr.stefwashcar.dto.admin.UpdateShopRequest;
import fr.stefwashcar.service.admin.AdminShopService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration - boutiques et évènements")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class AdminShopController {

    private final AdminShopService adminShopService;

    @PostMapping("/shops/crt")
    public ResponseEntity<?> createShop(@Valid @RequestBody CreateShopRequest body) {
        return adminShopService.createShop(body.toMap());
    }

    @PostMapping("/events/crt")
    public ResponseEntity<?> createEvent(@Valid @RequestBody CreateEventRequest body) {
        return adminShopService.createEvent(body.toMap());
    }

    @PatchMapping("/shops/{id}/pt")
    public ResponseEntity<?> patchShop(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShopRequest body
    ) {
        return adminShopService.patchShop(id, body.toMap());
    }

    @DeleteMapping("/shops/{id}/pt/dl")
    public ResponseEntity<?> deleteShop(@PathVariable Long id) {
        return adminShopService.deleteShop(id);
    }

    @PatchMapping("/events/{id}/pt")
    public ResponseEntity<?> patchEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest body
    ) {
        return adminShopService.patchEvent(id, body.toMap());
    }

    @DeleteMapping("/events/{id}/pt/dl")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        return adminShopService.deleteEvent(id);
    }

    @PatchMapping("/shops/{id}/notes")
    public ResponseEntity<?> patchShopNotes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShopNotesRequest body
    ) {
        return adminShopService.patchShopNotes(id, body.toMap());
    }
}
