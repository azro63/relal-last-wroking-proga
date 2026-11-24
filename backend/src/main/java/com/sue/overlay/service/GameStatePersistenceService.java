package com.sue.overlay.service;

import com.sue.overlay.model.GameEvent;
import com.sue.overlay.model.GameSnapshot;
import com.sue.overlay.repository.GameEventRepository;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class GameStatePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GameStatePersistenceService.class);

    private final GameSnapshotRepository snapshotRepository;
    private final GameEventRepository eventRepository;

    public GameStatePersistenceService(GameSnapshotRepository snapshotRepository, GameEventRepository eventRepository) {
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
    }

    /** body — JSON от Dota 2 GSI: { "map": { "clock_time": .., "game_time": .., "daytime": .., "win_team": .. }, "events": [..] } */
    public void saveSnapshot(Map<String, Object> body) {
        if (!(body.get("map") instanceof Map<?, ?> map)) return;
        if (!(map.get("clock_time") instanceof Number clock)) return;

        Integer gameTime = map.get("game_time") instanceof Number gt ? gt.intValue() : null;
        Boolean daytime = map.get("daytime") instanceof Boolean day ? day : null;
        String winTeam = map.get("win_team") instanceof String win && !win.isBlank() && !"none".equals(win) ? win : null;

        GameSnapshot snapshot = new GameSnapshot(null, clock.intValue(), gameTime, daytime, winTeam,
                LocalDateTime.now(ZoneOffset.UTC));
        snapshotRepository.save(snapshot);
    }

    public void saveEvents(Map<String, Object> body) {
        if (!(body.get("events") instanceof List<?> events) || events.isEmpty()) return;

        for (Object o : events) {
            if (!(o instanceof Map<?, ?> ev)) continue;
            if (!(ev.get("event_type") instanceof String type)) continue;

            int gameTime = ev.get("game_time") instanceof Number n ? n.intValue() : 0;
            GameEvent entity = eventRepository.save(
                    new GameEvent(null, type, gameTime, toJson(ev), LocalDateTime.now(ZoneOffset.UTC)));

            log.info("Сохранено событие: {} на {} сек.", entity.eventType(), entity.gameTime());
        }
    }

    /** Минимальный JSON-сериализатор (Map / Collection / String / Number / Boolean / null). */
    static String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (v instanceof Collection<?> c) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : c) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(x));
            }
            return sb.append(']').toString();
        }
        return quote(v.toString());
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.append('"').toString();
    }
}
