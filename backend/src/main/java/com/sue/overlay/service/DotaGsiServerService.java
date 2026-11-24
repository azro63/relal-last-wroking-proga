package com.sue.overlay.service;

// Новые импорты для версии 3.0.0
import com.github.mrbean355.dota2.gamestate.GameState;
import com.github.mrbean355.dota2.server.GameStateServer;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class DotaGsiServerService {

    private final GameStatePersistenceService persistenceService;

    public DotaGsiServerService(GameStatePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    public void startGsiServer() {
        GameStateServer.create(44444)
                .setSpectatingListener(gameState -> {
                    persistenceService.processGameState(gameState);
                })
                .startAsync();
    }
}