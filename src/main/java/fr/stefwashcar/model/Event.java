package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @Column(name = "end_at_utc", nullable = false)
    private Instant endAtUtc;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 50, nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToMany(mappedBy = "event")
    private List<Appointment> appointments = new ArrayList<>();

    @Column(name = "public_id", length = 36, nullable = false, unique = true, updatable = false)
    private String publicId;

    @PrePersist
    private void initPublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
        }
    }

    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointment.setEvent(this);
        }
    }

    public void removeAppointment(Appointment appointment) {
        if (appointments.remove(appointment) && appointment.getEvent() == this) {
            appointment.setEvent(null);
        }
    }
}
