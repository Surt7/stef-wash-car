package fr.stefwashcar.model;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service")
@Getter
@Setter
@NoArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(name = "duration_min", nullable = false)
    private Short durationMin = 20;

    @Column(nullable = false)
    private Short capacity = 1;

    @Column(length = 50, nullable = false)
    private String timezone = "Europe/Paris";

    @OneToMany(mappedBy = "service")
    private List<AvailabilityRule> availabilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "service")
    private List<Blackout> blackouts = new ArrayList<>();

    @OneToMany(mappedBy = "service")
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "service")
    private List<SpecialAvailability> specialAvailabilities = new ArrayList<>();

    @Column(name = "public_id", length = 26, nullable = false, unique = true, updatable = false)
    private String publicId;

    @OneToMany(mappedBy = "service")
    private List<Formule> formules = new ArrayList<>();

    @PrePersist
    private void initPublicId() {
        if (publicId == null) {
            publicId = UlidCreator.getUlid().toString();
        }
    }

    public void addAvailabilityRule(AvailabilityRule rule) {
        if (!availabilityRules.contains(rule)) {
            availabilityRules.add(rule);
            rule.setService(this);
        }
    }

    public void removeAvailabilityRule(AvailabilityRule rule) {
        if (availabilityRules.remove(rule) && rule.getService() == this) {
            rule.setService(null);
        }
    }

    public void addBlackout(Blackout blackout) {
        if (!blackouts.contains(blackout)) {
            blackouts.add(blackout);
            blackout.setService(this);
        }
    }

    public void removeBlackout(Blackout blackout) {
        if (blackouts.remove(blackout) && blackout.getService() == this) {
            blackout.setService(null);
        }
    }

    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointment.setService(this);
        }
    }

    public void removeAppointment(Appointment appointment) {
        if (appointments.remove(appointment) && appointment.getService() == this) {
            appointment.setService(null);
        }
    }

    public void addSpecialAvailability(SpecialAvailability availability) {
        if (!specialAvailabilities.contains(availability)) {
            specialAvailabilities.add(availability);
            availability.setService(this);
        }
    }

    public void removeSpecialAvailability(SpecialAvailability availability) {
        if (specialAvailabilities.remove(availability) && availability.getService() == this) {
            availability.setService(null);
        }
    }

    public void addFormule(Formule formule) {
        if (!formules.contains(formule)) {
            formules.add(formule);
            formule.setService(this);
        }
    }

    public void removeFormule(Formule formule) {
        if (formules.remove(formule) && formule.getService() == this) {
            formule.setService(null);
        }
    }
}
