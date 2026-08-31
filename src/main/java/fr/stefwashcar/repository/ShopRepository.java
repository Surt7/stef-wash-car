package fr.stefwashcar.repository;

import fr.stefwashcar.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findBySlug(String slug);

    List<Shop> findByIsActiveTrueOrderByNameAsc();

    List<Shop> findAllByOrderByNameAsc();
}
