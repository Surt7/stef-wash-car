package fr.stefwashcar.service.publicapi;

import fr.stefwashcar.model.Formule;
import fr.stefwashcar.model.Service;
import fr.stefwashcar.repository.FormuleRepository;
import fr.stefwashcar.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PublicCatalogCache {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final FormuleRepository formules;
    private final ServiceRepository services;

    @Cacheable("public-formules-active-v1")
    @Transactional(readOnly = true)
    public List<Map<String,Object>> getActiveFormules() {
        return formules.findByIsActiveTrueOrderBySortOrderAscIdAsc()
                .stream().map(this::normalizeFormule).toList();
    }

    @Cacheable("public-services-list-v1")
    @Transactional(readOnly = true)
    public List<Map<String,Object>> getServices() {
        return services.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().map(this::normalizeService).toList();
    }

    private Map<String,Object> normalizeFormule(Formule f) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("publicId", f.getPublicId());
        r.put("name", f.getName());
        r.put("priceCents", f.getPriceCents());
        r.put("isActive", f.isActive());
        r.put("sortOrder", f.getSortOrder());
        r.put("description", f.getDescription());
        r.put("code", f.getCode());
        r.put("servicePublicId", f.getService() != null ? f.getService().getPublicId() : null);
        r.put("imagePath", f.getImagePath());
        r.put("startDay", f.getStartDay() != null ? f.getStartDay().format(TIME) : null);
        r.put("endDay", f.getEndDay() != null ? f.getEndDay().format(TIME) : null);
        r.put("duration", f.getDuration() != null ? f.getDuration().format(TIME) : null);
        r.put("slotType", f.getSlotType());
        r.put("pauseTime", f.getPauseTime() != null ? f.getPauseTime().format(TIME) : null);
        r.put("appointmentsCount", f.getAppointments() != null ? f.getAppointments().size() : 0);

        if (f.getColor() != null) {
            Map<String,Object> c = new LinkedHashMap<>();
            c.put("value", f.getColor().getValue());
            c.put("cssClass", f.getColor().getCssClass());
            r.put("color", c);
        } else {
            r.put("color", null);
        }
        return r;
    }

    private Map<String,Object> normalizeService(Service s) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("publicId", s.getPublicId());
        r.put("name", s.getName());
        r.put("durationMin", s.getDurationMin());
        r.put("capacity", s.getCapacity());
        r.put("timezone", s.getTimezone());
        return r;
    }
}
