package com.sue.overlay.repository;

import com.sue.overlay.model.GameEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Структура в Redis:
 *   event:seq              — счётчик id
 *   event:{id}             — hash с полями события
 *   events:all             — zset, score = id
 *   events:time            — zset, score = game_time
 *   events:type:{type}     — zset, score = game_time
 *   events:types           — set типов событий
 *   events:counts          — hash type -> количество
 */
@Repository
public class GameEventRepository {

    private static final String ALL = "events:all";
    private static final String TIME = "events:time";
    private static final String TYPES = "events:types";
    private static final String COUNTS = "events:counts";

    private final StringRedisTemplate redis;

    public GameEventRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public GameEvent save(GameEvent e) {
        long id = Objects.requireNonNull(redis.opsForValue().increment("event:seq"));
        String sid = String.valueOf(id);

        Map<String, String> h = new HashMap<>();
        h.put("eventType", e.eventType());
        h.put("gameTime", String.valueOf(e.gameTime()));
        h.put("capturedAt", e.capturedAt().toString());
        if (e.payload() != null) h.put("payload", e.payload());
        redis.opsForHash().putAll("event:" + id, h);

        redis.opsForZSet().add(ALL, sid, id);
        redis.opsForZSet().add(TIME, sid, e.gameTime());
        redis.opsForZSet().add("events:type:" + e.eventType(), sid, e.gameTime());
        redis.opsForSet().add(TYPES, e.eventType());
        redis.opsForHash().increment(COUNTS, e.eventType(), 1);

        return new GameEvent(id, e.eventType(), e.gameTime(), e.payload(), e.capturedAt());
    }

    public List<GameEvent> findByTypeOrderByGameTimeDesc(String type, int limit) {
        return load(redis.opsForZSet().reverseRange("events:type:" + type, 0, limit - 1));
    }

    public List<GameEvent> findLatest(int n) {
        return load(redis.opsForZSet().reverseRange(ALL, 0, n - 1));
    }

    public List<GameEvent> findAfterGameTime(int gameTime) {
        return load(redis.opsForZSet().rangeByScore(TIME, gameTime + 1.0, Double.POSITIVE_INFINITY));
    }

    public List<String> findAllEventTypes() {
        Set<String> types = redis.opsForSet().members(TYPES);
        return types == null ? List.of() : types.stream().sorted().toList();
    }

    /** Тип события -> количество, по убыванию. */
    public Map<String, Long> countByEventType() {
        Map<Object, Object> raw = redis.opsForHash().entries(COUNTS);
        Map<String, Long> result = new LinkedHashMap<>();
        raw.entrySet().stream()
                .sorted((a, b) -> Long.compare(Long.parseLong((String) b.getValue()), Long.parseLong((String) a.getValue())))
                .forEach(en -> result.put((String) en.getKey(), Long.valueOf((String) en.getValue())));
        return result;
    }

    private List<GameEvent> load(Collection<String> ids) {
        List<GameEvent> result = new ArrayList<>();
        if (ids == null) return result;
        for (String id : ids) {
            Map<Object, Object> h = redis.opsForHash().entries("event:" + id);
            if (h.isEmpty()) continue;
            result.add(new GameEvent(
                    Long.valueOf(id),
                    (String) h.get("eventType"),
                    Integer.parseInt((String) h.get("gameTime")),
                    (String) h.get("payload"),
                    LocalDateTime.parse((String) h.get("capturedAt"))));
        }
        return result;
    }
}
