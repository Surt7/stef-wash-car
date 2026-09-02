package fr.stefwashcar.controller.admin;

import fr.stefwashcar.config.OpenApiConfig;
import fr.stefwashcar.service.admin.AdminAppointmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration - rendez-vous")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class AdminAppointmentController {

    private final AdminAppointmentService adminAppointmentService;

    @GetMapping("/appointments")
    public ResponseEntity<?> listAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") Long serviceId
    ) {
        return adminAppointmentService.listAppointments(date, serviceId);
    }

    @GetMapping("/providers")
    public ResponseEntity<?> listProviders() {
        return adminAppointmentService.listProviders();
    }

    @PostMapping("/orders")
    public ResponseEntity<?> upsertOrder(@RequestBody Map<String, Object> body) {
        return adminAppointmentService.upsertOrder(body);
    }

    @PostMapping("/orders/send-email")
    public ResponseEntity<?> sendOrderEmail(@RequestBody Map<String, Object> body) {
        return adminAppointmentService.sendOrderEmail(body);
    }

    @PostMapping("/override")
    public ResponseEntity<?> overrideSlot(
            @RequestBody Map<String, Object> body,
            @NonNull HttpServletRequest request
    ) {
        return adminAppointmentService.overrideSlot(body, request.getRemoteAddr());
    }

    @GetMapping("/slots")
    public ResponseEntity<?> listSlots(
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String date
    ) {
        return adminAppointmentService.listSlots(serviceId, date);
    }

    @GetMapping("/shops")
    public ResponseEntity<?> listShops(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return adminAppointmentService.listShops(activeOnly);
    }

    @GetMapping("/events")
    public ResponseEntity<?> listEvents(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "true") boolean futureOnly
    ) {
        return adminAppointmentService.listEvents(shopId, futureOnly);
    }

    @PatchMapping("/note")
    public ResponseEntity<?> updateQuickNote(@RequestBody Map<String, Object> body) {
        return adminAppointmentService.updateQuickNote(body);
    }

    @PostMapping("/appointments/send-custom-email")
    public ResponseEntity<?> sendCustomAppointmentEmail(@RequestBody Map<String, Object> body) {
        return adminAppointmentService.sendCustomAppointmentEmail(body);
    }

    @PostMapping("/appointments/resend-confirmation-email")
    public ResponseEntity<?> resendConfirmationEmail(@RequestBody Map<String, Object> body) {
        return adminAppointmentService.resendConfirmationEmail(body);
    }

    @PatchMapping("/appointments/{id}/set-event")
    public ResponseEntity<?> setAppointmentEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return adminAppointmentService.setAppointmentEvent(id, body);
    }

    @PatchMapping("/appointments/{id}/ptc/shift")
    public ResponseEntity<?> patchAppointment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return adminAppointmentService.patchAppointment(id, body);
    }
}
