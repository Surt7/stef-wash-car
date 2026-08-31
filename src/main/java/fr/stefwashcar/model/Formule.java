package fr.stefwashcar.model;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "formule")
@Getter
@Setter
@NoArgsConstructor
public class Formule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private int priceCents = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @OneToMany(mappedBy = "formule")
    private List<Appointment> appointments = new ArrayList<>();

    @Column(name = "public_id", length = 26, nullable = false, unique = true, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "start_day", nullable = false)
    private LocalTime startDay;

    @Column(name = "end_day", nullable = false)
    private LocalTime endDay;

    // Source Doctrine: type=time. Conservé tel quel pour une migration rapide.
    @Column(name = "duration", nullable = false)
    private LocalTime duration;

    @Column(name = "slot_type", length = 20, nullable = false)
    private String slotType;

    // Source Doctrine: type=time. À revoir plus tard si ceci représente une durée.
    @Column(name = "pause_time", nullable = false)
    private LocalTime pauseTime;

    @PrePersist
    private void initPublicId() {
        if (publicId == null) {
            publicId = UlidCreator.getUlid().toString();
        }
    }

    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointment.setFormule(this);
        }
    }

    public void removeAppointment(Appointment appointment) {
        if (appointments.remove(appointment) && appointment.getFormule() == this) {
            appointment.setFormule(null);
        }
    }
}
