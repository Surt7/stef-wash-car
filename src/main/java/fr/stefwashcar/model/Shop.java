package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop")
@Getter
@Setter
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(length = 80)
    private String slug;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(length = 60)
    private String region;

    @Column(length = 80)
    private String city;

    @Column(length = 4)
    private String country;

    @Column(length = 32)
    private String phone;

    @Column(length = 190)
    private String email;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "shop")
    private List<Event> events = new ArrayList<>();

    @Column(name = "notes_html", columnDefinition = "text")
    private String notesHtml;

    @PrePersist
    private void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addEvent(Event event) {
        if (!events.contains(event)) {
            events.add(event);
            event.setShop(this);
        }
    }

    public void removeEvent(Event event) {
        if (events.remove(event) && event.getShop() == this) {
            event.setShop(null);
        }
    }
}
