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
import java.util.HashMap;
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
        // 1. Отправляем обработанные данные для стандартного оверлея
        OverlayState overlayState = buildOverlayState(state);
        messagingTemplate.convertAndSend("/topic/overlay", overlayState);

        // 2. Отправляем ПОЛНЫЕ сырые данные для кастомных оверлеев
        // Spring сам превратит этот объект в JSON со всеми полями (items, abilities, draft и т.д.)
        messagingTemplate.convertAndSend("/topic/raw", state);
    }

    private void saveSnapshot(SpectatingGameState state) {
        var map = state.getMap();
        if (map == null) return;

        GameSnapshot snapshot = new GameSnapshot(
                null,
                map.getClockTime(),
                map.getGameTime(),
                map.isDaytime(),
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

        Map<String, List<SpectatedPlayer>> playersMap = state.getPlayers();
        Map<String, SpectatedHero> heroesMap = state.getHeroes();

        // Собираем всех игроков в мапу по слоту для быстрого поиска
        Map<Integer, SpectatedPlayer> playerBySlot = new HashMap<>();
        if (playersMap != null) {
            for (List<SpectatedPlayer> teamPlayers : playersMap.values()) {
                for (SpectatedPlayer p : teamPlayers) {
                    playerBySlot.put(p.getPlayerSlot(), p);
                }
            }
        }

        // Итерируемся по героям (ключи: player0 ... player9)
        if (heroesMap != null) {
            for (Map.Entry<String, SpectatedHero> entry : heroesMap.entrySet()) {
                String key = entry.getKey();
                SpectatedHero hero = entry.getValue();
                if (hero == null) continue;

                // Извлекаем индекс 0..9 из ключа "playerN"
                int index = -1;
                if (key.startsWith("player")) {
                    try {
                        index = Integer.parseInt(key.substring(6));
                    } catch (NumberFormatException ignored) {}
                }
                if (index < 0) continue;

                boolean isRadiant = index < 5;
                // ИСПРАВЛЕНИЕ: правильный слот для Dire — 128 + (index - 5)
                int playerSlot = isRadiant ? index : (index - 5 + 128);

                SpectatedPlayer player = playerBySlot.get(playerSlot);
                String playerName = (player != null && player.getName() != null)
                        ? player.getName()
                        : "Player " + index;
                double netWorth = player != null ? player.getNetWorth() : 0;

                HeroState heroState = mapHero(playerName, hero);

                if (isRadiant) {
                    radiantHeroes.add(heroState);
                    radiantEco += netWorth;
                } else {
                    direHeroes.add(heroState);
                    direEco += netWorth;
                }
            }
        }

        var map = state.getMap();
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

    private HeroState mapHero(String playerName, SpectatedHero hero) {
        int hpPercent = 0;
        int mpPercent = 0;
        int level = 0;
        boolean alive = false;
        int respawnTimer = 0;

        // ИСПРАВЛЕНИЕ: получаем внутреннее имя героя для картинки
        String heroName = hero.getName(); // например, "npc_dota_hero_axe"

        if (hero != null) {
            hpPercent = hero.getHealthPercent();
            mpPercent = hero.getManaPercent();
            level = hero.getLevel();
            alive = hero.isAlive();
            respawnTimer = hero.getRespawnTimer();
        }

        return new HeroState(
                playerName,
                heroName, // Передаем имя героя в модель
                level,
                hpPercent,
                mpPercent,
                alive,
                respawnTimer
        );
    }
}