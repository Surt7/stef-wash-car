package fr.stefwashcar.repository;

import fr.stefwashcar.model.Formule;
import fr.stefwashcar.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormuleRepository extends JpaRepository<Formule, Long> {
    Optional<Formule> findByPublicId(String publicId);
    List<Formule> findByIsActiveTrueOrderBySortOrderAsc();
    List<Formule> findByIsActiveTrueOrderBySortOrderAscIdAsc();
    List<Formule> findByServiceAndIsActiveTrueOrderBySortOrderAsc(Service service);
}
