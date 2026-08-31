package fr.stefwashcar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "color")
@Getter
@Setter
@NoArgsConstructor
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false, unique = true)
    private String name;

    @Column(length = 40, nullable = false)
    private String value;

    @Column(name = "css_class", length = 200)
    private String cssClass;

    @Column(length = 40)
    private String scope;

    @OneToMany(mappedBy = "color")
    private List<Formule> formules = new ArrayList<>();

    public void addFormule(Formule formule) {
        if (!formules.contains(formule)) {
            formules.add(formule);
            formule.setColor(this);
        }
    }

    public void removeFormule(Formule formule) {
        if (formules.remove(formule) && formule.getColor() == this) {
            formule.setColor(null);
        }
    }
}
