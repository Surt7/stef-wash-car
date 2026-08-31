package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provider")
@Getter
@Setter
@NoArgsConstructor
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 8, nullable = false)
    private String code;

    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    @Column(length = 255)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 7)
    private String color;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "provider")
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "provider")
    private List<Order> orders = new ArrayList<>();

    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointment.setProvider(this);
        }
    }

    public void removeAppointment(Appointment appointment) {
        if (appointments.remove(appointment) && appointment.getProvider() == this) {
            appointment.setProvider(null);
        }
    }

    public void addOrder(Order order) {
        if (!orders.contains(order)) {
            orders.add(order);
            order.setProvider(this);
        }
    }

    public void removeOrder(Order order) {
        if (orders.remove(order) && order.getProvider() == this) {
            order.setProvider(null);
        }
    }
}
