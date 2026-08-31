package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "`user`",
    indexes = @Index(name = "user_email_idx", columnList = "email")
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false)
    private String lastname;

    @Column(length = 80, nullable = false)
    private String firstname;

    @Column(length = 190, nullable = false, unique = true)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 45)
    private String lastip;

    @Column(name = "gdpr_consent", nullable = false)
    private Boolean gdprConsent = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "user")
    private List<Appointment> appointments = new ArrayList<>();

    public String getFullName() {
        String first = firstname == null ? "" : firstname.trim();
        String last = lastname == null ? "" : lastname.trim();
        return (first + " " + last).trim();
    }

    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointment.setUser(this);
        }
    }

    public void removeAppointment(Appointment appointment) {
        if (appointments.remove(appointment) && appointment.getUser() == this) {
            appointment.setUser(null);
        }
    }
}
