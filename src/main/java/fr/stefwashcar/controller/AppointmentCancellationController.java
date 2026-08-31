package fr.stefwashcar.controller;

import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AppointmentCancellationController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping("/appointments/cancel/{token}/confirm")
    public ModelAndView confirm(@PathVariable String token) {
        var appointment = appointmentRepository.findByCancelToken(token).orElse(null);

        if (appointment == null) {
            return errorView("Ce lien d’annulation est invalide ou a déjà été utilisé.", HttpStatus.NOT_FOUND);
        }

        var now = Instant.now();

        if (appointment.getCancelTokenExpiresAt() != null
                && appointment.getCancelTokenExpiresAt().isBefore(now)) {
            return errorView("Ce lien d’annulation a expiré.", HttpStatus.GONE);
        }

        if (appointment.getStatus() != AppointmentStatus.confirmed
                && appointment.getStatus() != AppointmentStatus.pending) {
            return errorView("Ce rendez-vous ne peut plus être annulé.", HttpStatus.BAD_REQUEST);
        }

        if (!appointment.getStartAtUtc().isAfter(now)) {
            return errorView("Le rendez-vous est déjà en cours ou passé.", HttpStatus.BAD_REQUEST);
        }

        return new ModelAndView("booking/cancel_confirm", Map.of("appointment", appointment));
    }

    @PostMapping("/appointments/cancel/{token}")
    @Transactional
    public String cancel(@PathVariable String token) {
        var appointment = appointmentRepository.findByCancelToken(token).orElse(null);

        if (appointment == null) {
            return "redirect:/appointments/cancel/error?reason=invalid_token";
        }

        var now = Instant.now();

        if (appointment.getCancelTokenExpiresAt() != null
                && appointment.getCancelTokenExpiresAt().isBefore(now)) {
            return "redirect:/appointments/cancel/error?reason=token_expired";
        }

        if (appointment.getStatus() != AppointmentStatus.confirmed
                && appointment.getStatus() != AppointmentStatus.pending) {
            return "redirect:/appointments/cancel/error?reason=not_cancellable";
        }

        if (!appointment.getStartAtUtc().isAfter(now)) {
            return "redirect:/appointments/cancel/error?reason=too_late";
        }

        appointment.setStatus(AppointmentStatus.cancelled);
        appointment.setCancelToken(null);
        appointment.setCancelTokenExpiresAt(null);

        return "redirect:/appointments/cancel/success/" + appointment.getId();
    }

    @GetMapping("/appointments/cancel/success/{id}")
    public ModelAndView success(@PathVariable Long id) {
        var appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return errorView("Impossible de retrouver le rendez-vous annulé.", HttpStatus.NOT_FOUND);
        }
        return new ModelAndView("booking/cancel_success", Map.of("appointment", appointment));
    }

    @GetMapping("/appointments/cancel/error")
    public ModelAndView error(@RequestParam(defaultValue = "unknown") String reason) {
        var messages = Map.of(
                "invalid_token", "Ce lien d’annulation est invalide ou a déjà été utilisé.",
                "token_expired", "Ce lien d’annulation a expiré.",
                "not_cancellable", "Ce rendez-vous ne peut plus être annulé.",
                "too_late", "Le rendez-vous est déjà en cours ou passé.",
                "unknown", "Une erreur est survenue lors de la tentative d’annulation."
        );
        return errorView(messages.getOrDefault(reason, messages.get("unknown")), HttpStatus.OK);
    }

    private ModelAndView errorView(String message, HttpStatus status) {
        var mav = new ModelAndView("booking/cancel_error", Map.of("message", message));
        mav.setStatus(status);
        return mav;
    }
}
