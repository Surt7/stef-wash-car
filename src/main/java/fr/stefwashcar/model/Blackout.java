package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "blackout")
@Getter
@Setter
@NoArgsConstructor
public class Blackout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "start_at_utc")
    private Instant startAtUtc;

    @Column(name = "end_at_utc")
    private Instant endAtUtc;

    @Column(name = "blackout_name", length = 80, nullable = false)
    private String blackoutName;
}
