package fr.stefwashcar.controller;

import fr.stefwashcar.service.booking.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CreateReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        return reservationService.createReservation(body, request.getRemoteAddr());
    }

    @PostMapping("/admin/reservations/override")
    public ResponseEntity<?> overrideReservation(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        // TODO security: @PreAuthorize("hasRole('ADMIN')") once Spring Security is wired.
        return reservationService.overrideReservation(body, request.getRemoteAddr());
    }
}
