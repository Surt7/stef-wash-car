package fr.stefwashcar.service;

import fr.stefwashcar.model.Provider;
import fr.stefwashcar.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProviderResolver {
    private final ProviderRepository providers;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Provider resolveProvider(Long providerId, Authentication authentication) {
        if (providerId != null) {
            return providers.findById(providerId)
                    .orElseThrow(() -> new AccessDeniedException("Prestataire introuvable."));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur SSO non authentifié.");
        }

        Object principal = authentication.getPrincipal();
        String email = firstNonBlank(
                invokeStringGetter(principal, "getEmail"),
                invokeStringGetter(principal, "getPreferredUsername"),
                principal instanceof UserDetails ud ? ud.getUsername() : null,
                authentication.getName()
        );

        if (email == null) {
            throw new AccessDeniedException("Impossible de déterminer le prestataire (aucun email SSO).");
        }

        var existing = providers.findByEmail(email);
        if (existing.isPresent()) return existing.get();

        String displayName = firstNonBlank(
                invokeStringGetter(principal, "getDisplayName"),
                invokeStringGetter(principal, "getFullName"),
                invokeStringGetter(principal, "getName"),
                email
        );

        Provider provider = new Provider();
        provider.setCode(generateProviderCodeFromEmail(email));
        provider.setDisplayName(displayName);
        provider.setEmail(email);
        provider.setPhone(null);
        provider.setColor(null);
        provider.setIsActive(true);
        return providers.save(provider);
    }

    private String generateProviderCodeFromEmail(String email) {
        String local = email == null ? "" : email.split("@", 2)[0];
        String cleaned = local.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() >= 2) return cleaned.substring(0, Math.min(8, cleaned.length()));
        return "P" + (1000 + random.nextInt(9000));
    }

    private static String invokeStringGetter(Object target, String name) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(name);
            Object value = method.invoke(target);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }
}
