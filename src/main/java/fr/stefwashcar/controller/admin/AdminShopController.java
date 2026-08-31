package fr.stefwashcar.controller.admin;

import fr.stefwashcar.service.admin.AdminShopService;
import lombok.RequiredArgsConstructor;
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
public class AdminShopController {

    private final AdminShopService adminShopService;

    @PostMapping("/shops/crt")
    public ResponseEntity<?> createShop(@RequestBody Map<String, Object> body) {
        return adminShopService.createShop(body);
    }

    @PostMapping("/events/crt")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body) {
        return adminShopService.createEvent(body);
    }

    @PatchMapping("/shops/{id}/pt")
    public ResponseEntity<?> patchShop(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return adminShopService.patchShop(id, body);
    }

    @DeleteMapping("/shops/{id}/pt/dl")
    public ResponseEntity<?> deleteShop(@PathVariable Long id) {
        return adminShopService.deleteShop(id);
    }

    @PatchMapping("/events/{id}/pt")
    public ResponseEntity<?> patchEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return adminShopService.patchEvent(id, body);
    }

    @DeleteMapping("/events/{id}/pt/dl")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        return adminShopService.deleteEvent(id);
    }

    @PatchMapping("/shops/{id}/notes")
    public ResponseEntity<?> patchShopNotes(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return adminShopService.patchShopNotes(id, body);
    }
}
