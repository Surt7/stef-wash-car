package fr.stefwashcar.repository;

import fr.stefwashcar.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findByPublicId(String publicId);

    List<Service> findAllByOrderByNameAsc();
}
