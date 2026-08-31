package fr.stefwashcar.model;

import fr.stefwashcar.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "appointment",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_slot",
        columnNames = {"service_id", "start_at_utc"}
    ),
    indexes = @Index(name = "idx_start_at", columnList = "start_at_utc")
)
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @Column(name = "end_at_utc", nullable = false)
    private Instant endAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.confirmed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formule_id")
    private Formule formule;

    @Column(length = 255)
    private String notes;

    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "appointment")
    private List<EmailLog> emailLogs = new ArrayList<>();

    @Column(name = "cancel_token", length = 128)
    private String cancelToken;

    @Column(name = "cancel_token_expires_at")
    private Instant cancelTokenExpiresAt;

    @OneToOne(mappedBy = "appointment", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "is_manual_override", nullable = false)
    private Boolean isManualOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "price_cents_at_booking")
    private Integer priceCentsAtBooking;

    @Column(name = "formule_name_at_booking", length = 120)
    private String formuleNameAtBooking;

    @Column(name = "formule_color_value_at_booking", length = 40)
    private String formuleColorValueAtBooking;

    public void addEmailLog(EmailLog emailLog) {
        if (!emailLogs.contains(emailLog)) {
            emailLogs.add(emailLog);
            emailLog.setAppointment(this);
        }
    }

    public void removeEmailLog(EmailLog emailLog) {
        if (emailLogs.remove(emailLog) && emailLog.getAppointment() == this) {
            emailLog.setAppointment(null);
        }
    }

    public void setOrder(Order order) {
        if (order != null && order.getAppointment() != this) {
            order.setAppointment(this);
        }
        this.order = order;
    }

    public static Appointment fromLocalSlot(Service service, LocalDateTime localSlot) {
        String timezone = service.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            timezone = "Europe/Paris";
        }

        ZoneId serviceZone = ZoneId.of(timezone);
        Instant startUtc = localSlot.atZone(serviceZone).toInstant();

        int durationMinutes = service.getDurationMin() != null
            ? service.getDurationMin().intValue()
            : 20;

        Instant endUtc = startUtc.plus(Duration.ofMinutes(durationMinutes));

        Appointment appointment = new Appointment();
        appointment.setService(service);
        appointment.setStartAtUtc(startUtc);
        appointment.setEndAtUtc(endUtc);
        appointment.setStatus(AppointmentStatus.confirmed);
        return appointment;
    }
}
