package fr.stefwashcar.repository;

import fr.stefwashcar.model.Event;
import fr.stefwashcar.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByPublicId(String publicId);

    List<Event> findByShopOrderByStartAtUtcAsc(Shop shop);

    List<Event> findByShopAndEndAtUtcGreaterThanEqualOrderByStartAtUtcAsc(
            Shop shop,
            Instant now
    );

    long countByShop(Shop shop);
}
