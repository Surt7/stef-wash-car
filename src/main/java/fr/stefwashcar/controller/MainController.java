package fr.stefwashcar.controller;

import fr.stefwashcar.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @PostMapping("/api/blackouts")
    public ResponseEntity<?> createBlackout(@RequestBody Map<String, Object> body) {
        return mainService.createBlackout(body);
    }

    @GetMapping("/test-mail")
    public ResponseEntity<?> testMail() {
        return mainService.testMail();
    }
}
