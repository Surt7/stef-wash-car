package fr.stefwashcar.repository;

import fr.stefwashcar.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByCode(String code);

    Optional<Provider> findByEmail(String email);

    List<Provider> findByIsActiveTrueOrderByDisplayNameAsc();
}
