package com.sue.overlay;

import com.github.mrbean355.dota2.server.GameStateServer;
import com.sue.overlay.service.GameStatePersistenceService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TimeChecker {

    private static final Logger log = LoggerFactory.getLogger(TimeChecker.class);
    private static final int DOTA_GSI_PORT = 44444;

    private final GameStatePersistenceService persistenceService;

    public TimeChecker(GameStatePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    public void start() {
        log.info("Запуск Dota 2 GSI сервера на порту {}", DOTA_GSI_PORT);

        GameStateServer.create(DOTA_GSI_PORT)
                .setSpectatingListener(gameState -> {
                    if (gameState.getMap() != null) {
                        int clockTime = gameState.getMap().getClockTime();
                        log.debug("Игровое время: {} сек.", clockTime);
                        persistenceService.saveSnapshot(gameState);
                    } else {
                        log.debug("Данные карты недоступны (игра не идёт?)");
                    }
                })
                .startAsync(); // сервер работает в фоне, не блокируя Spring
    }
}