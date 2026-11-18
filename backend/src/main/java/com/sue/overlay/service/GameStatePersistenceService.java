package com.sue.overlay.service;

import com.github.mrbean355.dota2.gamestate.SpectatingGameState;
import com.sue.overlay.persistence.entity.GameSnapshot;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GameStatePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GameStatePersistenceService.class);

    private final GameSnapshotRepository repository;

    public GameStatePersistenceService(GameSnapshotRepository repository) {
        this.repository = repository;
    }

    /**
     * Сохраняет снимок состояния игры, если доступны данные карты.
     * Принимает SpectatingGameState, у которого есть getMap().
     */
    @Transactional
    public void saveSnapshot(SpectatingGameState gameState) {
        if (gameState == null || gameState.getMap() == null) {
            return;
        }

        var map = gameState.getMap();

        GameSnapshot snapshot = new GameSnapshot();
        snapshot.setClockTime(map.getClockTime());
        snapshot.setCapturedAt(LocalDateTime.now());

        // Опциональные поля — раскомментируйте, если методы существуют в вашей версии библиотеки.
        // try {
        //     snapshot.setDaytime(map.isDaytime());
        // } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        //     // Поле недоступно — пропускаем
        // }

        repository.save(snapshot);
        log.info("Снимок сохранён: clockTime={}s", snapshot.getClockTime());
    }
}