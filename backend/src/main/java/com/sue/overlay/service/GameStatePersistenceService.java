package com.sue.overlay.service;

import com.github.mrbean355.dota2.gamestate.SpectatingGameState;
import com.github.mrbean355.dota2.player.SpectatedPlayer;
import com.github.mrbean355.dota2.hero.SpectatedHero;
import com.sue.overlay.model.GameSnapshot;
import com.sue.overlay.model.HeroState;
import com.sue.overlay.model.OverlayState;
import com.sue.overlay.repository.GameEventRepository;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GameStatePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GameStatePersistenceService.class);

    private final GameSnapshotRepository snapshotRepository;
    private final GameEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GameStatePersistenceService(GameSnapshotRepository snapshotRepository,
                                       GameEventRepository eventRepository,
                                       SimpMessagingTemplate messagingTemplate) {
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void processGameState(SpectatingGameState state) {
        if (state == null || state.getMap() == null) return;

        saveSnapshot(state);
        OverlayState overlayState = buildOverlayState(state);
        messagingTemplate.convertAndSend("/topic/overlay", overlayState);
    }

    private void saveSnapshot(SpectatingGameState state) {
        var map = state.getMap();
        if (map == null) return;

        GameSnapshot snapshot = new GameSnapshot(
                null,
                map.getClockTime(),
                map.getGameTime(),
                map.isDaytime(),
                // Исправлено: метод называется getWinningTeam(), а не getWinTeam()
                map.getWinningTeam() != null ? map.getWinningTeam().name() : null,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        snapshotRepository.save(snapshot);
    }

    private OverlayState buildOverlayState(SpectatingGameState state) {
        List<HeroState> radiantHeroes = new ArrayList<>();
        List<HeroState> direHeroes = new ArrayList<>();
        double radiantEco = 0;
        double direEco = 0;

        // getPlayers() возвращает Map<String, List<SpectatedPlayer>> (ключи "Radiant"/"Dire")
        Map<String, List<SpectatedPlayer>> playersMap = state.getPlayers();
        // getHeroes() возвращает Map<String, SpectatedHero> (ключ — ID игрока)
        Map<String, SpectatedHero> heroesMap = state.getHeroes();

        if (playersMap != null) {
            // Проходим по всем командам (Radiant, Dire и, возможно, другие)
            for (Map.Entry<String, List<SpectatedPlayer>> entry : playersMap.entrySet()) {
                String teamName = entry.getKey();
                List<SpectatedPlayer> teamPlayers = entry.getValue();
                if (teamPlayers == null) continue;

                for (SpectatedPlayer p : teamPlayers) {
                    double netWorth = p.getNetWorth();
                    // Получаем героя по ID игрока из отдельной Map
                    SpectatedHero hero = heroesMap != null ? heroesMap.get(p.getId()) : null;
                    HeroState heroState = mapHero(p, hero);

                    if ("Radiant".equals(teamName)) {
                        radiantEco += netWorth;
                        radiantHeroes.add(heroState);
                    } else if ("Dire".equals(teamName)) {
                        direEco += netWorth;
                        direHeroes.add(heroState);
                    }
                }
            }
        }

        var map = state.getMap();
        // Исправлено: getRadiantScore() и getDireScore() возвращают int (не Integer), проверка на null не нужна
        int radiantScore = map.getRadiantScore();
        int direScore = map.getDireScore();

        return new OverlayState(
                map.getClockTime(),
                map.isDaytime(),
                radiantScore,
                direScore,
                radiantEco,
                direEco,
                radiantHeroes,
                direHeroes
        );
    }

    private HeroState mapHero(SpectatedPlayer player, SpectatedHero hero) {
        int hpPercent = 0;
        int mpPercent = 0;
        int level = 0;
        boolean alive = false;
        int respawnTimer = 0;

        if (hero != null) {
            hpPercent = hero.getHealthPercent();
            mpPercent = hero.getManaPercent();
            level = hero.getLevel();
            alive = hero.isAlive();
            respawnTimer = hero.getRespawnTimer();
        }

        return new HeroState(
                player.getName(),
                level,
                hpPercent,
                mpPercent,
                alive,
                respawnTimer
        );
    }
}