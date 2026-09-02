# Stef Wash Car — backend

Backend Spring Boot 4 / Java 21 avec PostgreSQL, documentation OpenAPI et données de démonstration.

## Démarrage local

1. Copier `.env.example` vers `.env` et remplacer les mots de passe.
2. Démarrer PostgreSQL : `docker compose up -d`.
3. Démarrer l'application avec le profil de développement :

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

Le profil `dev` insère de façon idempotente des services, une formule, des horaires, un prestataire, une boutique, un évènement, un client et un rendez-vous fictifs.

Les e-mails sont désactivés par défaut dans tous les profils. Aucun appel SMTP ni journal d'envoi n'est produit tant que cette option reste désactivée. Pour réactiver toute la plomberie existante, configure simplement `MAIL_ENABLED=true` ainsi que les variables `MAIL_*` du fichier `.env`, puis redémarre l'application.

Swagger UI est disponible sur <http://localhost:8080/swagger-ui.html>. Les routes d'administration utilisent HTTP Basic ; le bouton **Authorize** accepte `ADMIN_USERNAME` et `ADMIN_PASSWORD`. En profil `dev`, le mot de passe de secours est `change-me-in-dev` et doit rester strictement local.

En dehors du profil `dev`, Swagger est désactivé par défaut (`OPENAPI_ENABLED=false`) et `ADMIN_USERNAME` / `ADMIN_PASSWORD` sont obligatoires. Utilise une adresse e-mail comme identifiant admin : elle sert aussi à rattacher les actions au prestataire connecté.

## Tests

```powershell
.\mvnw.cmd clean test
```

Les tests utilisent une base H2 éphémère en mode PostgreSQL et ne nécessitent ni Docker ni une base locale déjà remplie.
