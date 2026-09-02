package fr.stefwashcar.service.publicapi;

import fr.stefwashcar.model.Formule;
import fr.stefwashcar.model.Service;
import fr.stefwashcar.dto.publicapi.PublicFormuleResponse;
import fr.stefwashcar.dto.publicapi.PublicServiceResponse;
import fr.stefwashcar.repository.FormuleRepository;
import fr.stefwashcar.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PublicCatalogCache {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final FormuleRepository formules;
    private final ServiceRepository services;

    @Cacheable("public-formules-active-v1")
    @Transactional(readOnly = true)
    public List<PublicFormuleResponse> getActiveFormules() {
        return formules.findByIsActiveTrueOrderBySortOrderAscIdAsc()
                .stream().map(this::toFormuleResponse).toList();
    }

    @Cacheable("public-services-list-v1")
    @Transactional(readOnly = true)
    public List<PublicServiceResponse> getServices() {
        return services.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().map(this::toServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PublicFormuleResponse> getFormule(String publicId) {
        return formules.findByPublicId(publicId).map(this::toFormuleResponse);
    }

    public PublicFormuleResponse toFormuleResponse(Formule f) {
        var service = f.getService();
        var color = f.getColor() == null ? null : new PublicFormuleResponse.ColorResponse(
                f.getColor().getValue(), f.getColor().getCssClass());
        return new PublicFormuleResponse(
                f.getPublicId(), f.getName(), f.getPriceCents(), f.isActive(),
                f.getSortOrder(), f.getDescription(), f.getCode(),
                service != null ? service.getId() : null,
                service != null ? service.getPublicId() : null,
                f.getImagePath(), format(f.getStartDay()), format(f.getEndDay()),
                format(f.getDuration()), f.getSlotType(), format(f.getPauseTime()),
                f.getAppointments() != null ? f.getAppointments().size() : 0, color
        );
    }

    public PublicServiceResponse toServiceResponse(Service s) {
        return new PublicServiceResponse(
                s.getId(), s.getPublicId(), s.getName(), s.getDurationMin(),
                s.getCapacity(), s.getTimezone()
        );
    }

    private String format(java.time.LocalTime value) {
        return value == null ? null : value.format(TIME);
    }
}
