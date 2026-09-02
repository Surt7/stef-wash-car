package fr.stefwashcar.controller;

import fr.stefwashcar.dto.booking.CreateReservationRequest;
import fr.stefwashcar.dto.booking.ReservationResponse;
import fr.stefwashcar.service.booking.ReservationService;
import fr.stefwashcar.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    @Operation(summary = "Créer une réservation")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Réservation créée",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Réservation idempotente déjà existante",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "404", description = "Ressource introuvable"),
            @ApiResponse(responseCode = "409", description = "Créneau déjà réservé")
    })
    public ResponseEntity<?> createReservation(
            @Valid @RequestBody CreateReservationRequest body,
            HttpServletRequest request
    ) {
        return reservationService.createReservation(body, request.getRemoteAddr());
    }

    @PostMapping("/admin/reservations/override")
    @Operation(summary = "Créer une réservation forcée (administration)")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    public ResponseEntity<?> overrideReservation(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        return reservationService.overrideReservation(body, request.getRemoteAddr());
    }
}
