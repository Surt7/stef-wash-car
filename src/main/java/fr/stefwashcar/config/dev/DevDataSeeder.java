package fr.stefwashcar.config.dev;

import fr.stefwashcar.enums.AppointmentStatus;
import fr.stefwashcar.model.Appointment;
import fr.stefwashcar.model.AvailabilityRule;
import fr.stefwashcar.model.Color;
import fr.stefwashcar.model.Event;
import fr.stefwashcar.model.Formule;
import fr.stefwashcar.model.Provider;
import fr.stefwashcar.model.Service;
import fr.stefwashcar.model.Shop;
import fr.stefwashcar.model.User;
import fr.stefwashcar.repository.AppointmentRepository;
import fr.stefwashcar.repository.AvailabilityRuleRepository;
import fr.stefwashcar.repository.ColorRepository;
import fr.stefwashcar.repository.EventRepository;
import fr.stefwashcar.repository.FormuleRepository;
import fr.stefwashcar.repository.ProviderRepository;
import fr.stefwashcar.repository.ServiceRepository;
import fr.stefwashcar.repository.ShopRepository;
import fr.stefwashcar.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Configuration
@Profile("dev")
public class DevDataSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(DevDataSeeder.class);

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Bean
    CommandLineRunner seedDevData(
            ServiceRepository services,
            ColorRepository colors,
            FormuleRepository formules,
            AvailabilityRuleRepository availabilityRules,
            ProviderRepository providers,
            ShopRepository shops,
            EventRepository events,
            UserRepository users,
            AppointmentRepository appointments
    ) {
        return args -> {
            log.info("DEV seed: starting");

            Color blue = seedColor(colors);

            Service standard = findServiceByName(services, "Séance photo");
            Service wedding = findServiceByName(services, "Mariage");

            /*
             * Important for the current port:
             * AvailabilityService still preserves the Symfony rule
             * where service id 2 is the wedding service.
             *
             * On a fresh DB, standard is inserted first and wedding second,
             * so wedding normally gets id = 2.
             */
            if (standard == null && wedding == null && services.count() == 0) {
                standard = createService(
                        services,
                        "Séance photo",
                        (short) 20,
                        (short) 1
                );

                wedding = createService(
                        services,
                        "Mariage",
                        (short) 60,
                        (short) 1
                );
            } else {
                if (standard == null) {
                    standard = createService(
                            services,
                            "Séance photo",
                            (short) 20,
                            (short) 1
                    );
                }

                if (wedding == null) {
                    wedding = createService(
                            services,
                            "Mariage",
                            (short) 60,
                            (short) 1
                    );
                }
            }

            if (wedding.getId() == null || wedding.getId() != 2L) {
                log.warn(
                        "DEV seed: 'Mariage' has id={} instead of 2. "
                                + "The current AvailabilityService still contains "
                                + "the legacy Symfony assumption serviceId=2.",
                        wedding.getId()
                );
            }

            seedStandardAvailability(availabilityRules, standard);
            seedWeddingAvailability(availabilityRules, wedding);

            Formule formule = seedFormule(
                    formules,
                    standard,
                    blue
            );

            Provider provider = seedProvider(providers);
            Shop shop = seedShop(shops);
            seedEvent(events, shop);
            User user = seedUser(users);

            seedAppointment(
                    appointments,
                    standard,
                    formule,
                    provider,
                    user
            );

            log.info("DEV seed: finished");
        };
    }

    private Color seedColor(ColorRepository repository) {
        return repository.findByName("Bleu dev")
                .orElseGet(() -> {
                    Color color = new Color();
                    color.setName("Bleu dev");
                    color.setValue("#0078D4");
                    color.setCssClass("dev-blue");
                    color.setScope("formule");
                    return repository.save(color);
                });
    }

    private Service createService(
            ServiceRepository repository,
            String name,
            short durationMin,
            short capacity
    ) {
        Service service = new Service();
        service.setName(name);
        service.setDurationMin(durationMin);
        service.setCapacity(capacity);
        service.setTimezone("Europe/Paris");
        return repository.save(service);
    }

    private Service findServiceByName(
            ServiceRepository repository,
            String name
    ) {
        return repository.findAll()
                .stream()
                .filter(service -> name.equals(service.getName()))
                .findFirst()
                .orElse(null);
    }

    private void seedStandardAvailability(
            AvailabilityRuleRepository repository,
            Service service
    ) {
        if (!repository
                .findByServiceOrderByWeekdayAscStartTimeAsc(service)
                .isEmpty()) {
            return;
        }

        // Monday -> Saturday, 09:00 -> 18:00, one slot every 20 minutes.
        for (short weekday = 1; weekday <= 6; weekday++) {
            AvailabilityRule rule = new AvailabilityRule();
            rule.setService(service);
            rule.setWeekday(weekday);
            rule.setStartTime(LocalTime.of(9, 0));
            rule.setEndTime(LocalTime.of(18, 0));
            rule.setStepMin((short) 20);
            rule.setRuleName("DEV - journée standard");
            repository.save(rule);
        }
    }

    private void seedWeddingAvailability(
            AvailabilityRuleRepository repository,
            Service service
    ) {
        if (!repository
                .findByServiceOrderByWeekdayAscStartTimeAsc(service)
                .isEmpty()) {
            return;
        }

        // Friday + Saturday, 10:00 -> 18:00, one-hour slots.
        for (short weekday : new short[]{5, 6}) {
            AvailabilityRule rule = new AvailabilityRule();
            rule.setService(service);
            rule.setWeekday(weekday);
            rule.setStartTime(LocalTime.of(10, 0));
            rule.setEndTime(LocalTime.of(18, 0));
            rule.setStepMin((short) 60);
            rule.setRuleName("DEV - mariage");
            repository.save(rule);
        }
    }

    private Formule seedFormule(
            FormuleRepository repository,
            Service service,
            Color color
    ) {
        Formule existing = repository.findAll()
                .stream()
                .filter(formule -> "DEV-STD".equals(formule.getCode()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        Formule formule = new Formule();
        formule.setCode("DEV-STD");
        formule.setName("Formule standard DEV");
        formule.setPriceCents(4900);
        formule.setSortOrder(1);
        formule.setService(service);
        formule.setColor(color);
        formule.setDescription("Formule de démonstration pour les tests locaux.");
        formule.setStartDay(LocalTime.of(9, 0));
        formule.setEndDay(LocalTime.of(18, 0));

        /*
         * The current entity preserves the original SQL TIME mapping
         * for duration/pauseTime. We therefore seed 00:20 and 00:00.
         */
        formule.setDuration(LocalTime.of(0, 20));
        formule.setPauseTime(LocalTime.of(0, 0));
        formule.setSlotType("standard");

        return repository.save(formule);
    }

    private Provider seedProvider(ProviderRepository repository) {
        return repository.findByCode("DEV")
                .orElseGet(() -> {
                    Provider provider = new Provider();
                    provider.setCode("DEV");
                    provider.setDisplayName("Prestataire DEV");
                    provider.setEmail("provider.dev@example.test");
                    provider.setPhone("+33100000000");
                    provider.setColor("#0078D4");
                    provider.setIsActive(true);
                    return repository.save(provider);
                });
    }

    private Shop seedShop(ShopRepository repository) {
        return repository.findBySlug("studio-dev")
                .orElseGet(() -> {
                    Shop shop = new Shop();
                    shop.setName("Studio DEV");
                    shop.setSlug("studio-dev");
                    shop.setAddressLine1("1 rue du Test");
                    shop.setPostalCode("75001");
                    shop.setCity("Paris");
                    shop.setCountry("FR");
                    shop.setPhone("+33100000001");
                    shop.setEmail("studio.dev@example.test");
                    shop.setNotes("Magasin créé automatiquement par le profil dev.");
                    shop.setIsActive(true);
                    return repository.save(shop);
                });
    }

    private void seedEvent(
            EventRepository repository,
            Shop shop
    ) {
        boolean exists = repository
                .findByShopOrderByStartAtUtcAsc(shop)
                .stream()
                .anyMatch(event -> "Évènement DEV".equals(event.getName()));

        if (exists) {
            return;
        }

        LocalDate day = nextWorkingDay(LocalDate.now(PARIS).plusDays(7));

        Event event = new Event();
        event.setShop(shop);
        event.setName("Évènement DEV");
        event.setDescription("Évènement fictif pour tester les parcours admin.");
        event.setStartAtUtc(day.atTime(9, 0).atZone(PARIS).toInstant());
        event.setEndAtUtc(day.atTime(18, 0).atZone(PARIS).toInstant());
        event.setMaxCapacity(20);
        event.setStatus("active");
        event.setTimezone("Europe/Paris");
        event.setCreatedAt(Instant.now());

        repository.save(event);
    }

    private User seedUser(UserRepository repository) {
        return repository.findByEmail("client.dev@example.test")
                .orElseGet(() -> {
                    User user = new User();
                    user.setFirstname("Jean");
                    user.setLastname("Test");
                    user.setEmail("client.dev@example.test");
                    user.setPhone("+33600000000");
                    user.setLastip("127.0.0.1");
                    user.setGdprConsent(true);
                    user.setCreatedAt(Instant.now());
                    return repository.save(user);
                });
    }

    private void seedAppointment(
            AppointmentRepository repository,
            Service service,
            Formule formule,
            Provider provider,
            User user
    ) {
        if (repository
                .findByIdempotencyKey("dev-seed-appointment-1")
                .isPresent()) {
            return;
        }

        LocalDate day = nextWorkingDay(LocalDate.now(PARIS).plusDays(1));

        Instant start = day
                .atTime(10, 0)
                .atZone(PARIS)
                .toInstant();

        int durationMin = service.getDurationMin() != null
                ? service.getDurationMin().intValue()
                : 20;

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setService(service);
        appointment.setFormule(formule);
        appointment.setProvider(provider);
        appointment.setStartAtUtc(start);
        appointment.setEndAtUtc(
                start.plus(Duration.ofMinutes(durationMin))
        );
        appointment.setStatus(AppointmentStatus.confirmed);
        appointment.setNotes("Rendez-vous DEV : ce créneau doit apparaître occupé.");
        appointment.setIdempotencyKey("dev-seed-appointment-1");
        appointment.setCreatedAt(Instant.now());
        appointment.setCancelToken("dev-cancel-token-1");
        appointment.setCancelTokenExpiresAt(start);
        appointment.setIsManualOverride(false);

        repository.save(appointment);
    }

    private LocalDate nextWorkingDay(LocalDate day) {
        LocalDate current = day;

        while (current.getDayOfWeek().getValue() == 7) {
            current = current.plusDays(1);
        }

        return current;
    }
}
