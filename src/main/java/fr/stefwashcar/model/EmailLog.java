package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_log")
@Getter
@Setter
@NoArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "to_email", length = 190, nullable = false)
    private String toEmail;

    @Column(length = 80, nullable = false)
    private String template;

    @Column(length = 10, nullable = false)
    private String status;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "message_id", length = 190)
    private String messageId;
}
