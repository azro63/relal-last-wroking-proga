package com.sue.overlay.service;

import com.github.mrbean355.dota2.gamestate.SpectatingGameState;
import com.sue.overlay.persistence.entity.GameEvent;
import com.sue.overlay.persistence.entity.GameSnapshot;
import com.sue.overlay.repository.GameEventRepository;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GameStatePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GameStatePersistenceService.class);

    private final GameSnapshotRepository snapshotRepository;
    private final GameEventRepository eventRepository;

    public GameStatePersistenceService(GameSnapshotRepository snapshotRepository, GameEventRepository eventRepository) {
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void saveSnapshot(SpectatingGameState gameState) {
        if (gameState == null || gameState.getMap() == null) return;

        var map = gameState.getMap();
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.setClockTime(map.getClockTime());
        snapshot.setCapturedAt(LocalDateTime.now());

        snapshotRepository.save(snapshot);
    }

    @Transactional
    public void saveEvents(SpectatingGameState gameState) {
        if (gameState == null || gameState.getEvents() == null || gameState.getEvents().isEmpty()) return;

        for (var dotaEvent : gameState.getEvents()) {
            GameEvent entity = new GameEvent();
            entity.setEventType(dotaEvent.getEventType());
            entity.setGameTime(dotaEvent.getGameTime());
            // Если payload сложный, лучше использовать Jackson ObjectMapper для сериализации в String
            entity.setPayload(dotaEvent.getPayload() != null ? dotaEvent.getPayload().toString() : null);
            entity.setCapturedAt(LocalDateTime.now());
            eventRepository.save(entity);

            log.info("Сохранено событие: {} на {} сек.", entity.getEventType(), entity.getGameTime());
        }
    }
}