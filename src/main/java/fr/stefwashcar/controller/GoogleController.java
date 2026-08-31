package fr.stefwashcar.controller;

/*
 * Google Calendar est désactivé pour le moment.
 *
 * Code conservé ici pour une réactivation future rapide si besoin.
 *
 * @RestController
 * @RequestMapping("/api/google-calendar")
 * @RequiredArgsConstructor
 * public class GoogleController {
 *
 *     private final GoogleCalendarService googleCalendarService;
 *
 *     @GetMapping("/events")
 *     public ResponseEntity<?> events() {
 *         try {
 *             var events = googleCalendarService.getFutureEvents();
 *             Map<String, Object> body = new LinkedHashMap<>();
 *             body.put("count", events.size());
 *             body.put("items", events);
 *             return ResponseEntity.ok(body);
 *         } catch (Exception e) {
 *             Map<String, Object> body = new LinkedHashMap<>();
 *             body.put("error", true);
 *             body.put("message", e.getMessage());
 *             body.put("class", e.getClass().getName());
 *             return ResponseEntity.internalServerError().body(body);
 *         }
 *     }
 * }
 */
public class GoogleController {
    // Désactivé volontairement.
}
