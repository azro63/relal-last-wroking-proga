package com.sue.overlay.controller;

import com.sue.overlay.service.GameStatePersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Принимает Dota 2 Game State Integration.
 * В gamestate_integration_*.cfg укажите: "uri" "http://localhost:8080/gsi"
 */
@RestController
public class GsiController {

    private final GameStatePersistenceService service;

    public GsiController(GameStatePersistenceService service) {
        this.service = service;
    }

    @PostMapping({"/gsi", "/gsi/"})
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> body) {
        service.saveSnapshot(body);
        service.saveEvents(body);
        return ResponseEntity.ok().build();
    }
}
