package com.sue.overlay.repository;

import com.sue.overlay.model.GameSnapshot;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Структура в Redis:
 *   snapshot:seq              — счётчик id
 *   snapshot:{id}             — hash с полями снимка
 *   snapshots:all             — zset, score = id
 *   snapshots:clock           — zset, score = clock_time
 *   snapshots:captured        — zset, score = captured_at (epoch ms, UTC)
 *   snapshots:night           — set id ночных снимков
 *   snapshots:team:{team}     — set id снимков по победившей команде
 */
@Repository
public class GameSnapshotRepository {

    private static final String ALL = "snapshots:all";
    private static final String CLOCK = "snapshots:clock";
    private static final String CAPTURED = "snapshots:captured";
    private static final String NIGHT = "snapshots:night";

    private final StringRedisTemplate redis;

    public GameSnapshotRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public GameSnapshot save(GameSnapshot s) {
        long id = Objects.requireNonNull(redis.opsForValue().increment("snapshot:seq"));
        String sid = String.valueOf(id);

        Map<String, String> h = new HashMap<>();
        h.put("clockTime", String.valueOf(s.clockTime()));
        h.put("capturedAt", s.capturedAt().toString());
        if (s.gameTime() != null) h.put("gameTime", s.gameTime().toString());
        if (s.daytime() != null) h.put("daytime", s.daytime().toString());
        if (s.winTeam() != null) h.put("winTeam", s.winTeam());
        redis.opsForHash().putAll("snapshot:" + id, h);

        redis.opsForZSet().add(ALL, sid, id);
        redis.opsForZSet().add(CLOCK, sid, s.clockTime());
        redis.opsForZSet().add(CAPTURED, sid, toMillis(s.capturedAt()));
        if (Boolean.FALSE.equals(s.daytime())) redis.opsForSet().add(NIGHT, sid);
        if (s.winTeam() != null) redis.opsForSet().add("snapshots:team:" + s.winTeam(), sid);

        return new GameSnapshot(id, s.clockTime(), s.gameTime(), s.daytime(), s.winTeam(), s.capturedAt());
    }

    public long count() {
        Long n = redis.opsForZSet().zCard(ALL);
        return n == null ? 0 : n;
    }

    public List<GameSnapshot> findLatest(int n) {
        return load(redis.opsForZSet().reverseRange(ALL, 0, n - 1));
    }

    public List<GameSnapshot> findAfterClockTime(int clockTime) {
        return load(redis.opsForZSet().rangeByScore(CLOCK, clockTime + 1.0, Double.POSITIVE_INFINITY));
    }

    public List<GameSnapshot> findByCapturedAtBetween(LocalDateTime from, LocalDateTime to) {
        return load(redis.opsForZSet().rangeByScore(CAPTURED, toMillis(from), toMillis(to)));
    }

    public List<GameSnapshot> findNight() {
        return loadSorted(redis.opsForSet().members(NIGHT));
    }

    public List<GameSnapshot> findByWinTeam(String team) {
        return loadSorted(redis.opsForSet().members("snapshots:team:" + team));
    }

    public Integer findMaxClockTime() {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> top =
                redis.opsForZSet().reverseRangeWithScores(CLOCK, 0, 0);
        if (top == null || top.isEmpty()) return null;
        Double score = top.iterator().next().getScore();
        return score == null ? null : score.intValue();
    }

    public Double findAverageClockTimeByWinTeam(String team) {
        List<GameSnapshot> list = findByWinTeam(team);
        return list.isEmpty() ? null : list.stream().mapToInt(GameSnapshot::clockTime).average().orElse(0);
    }

    // ---------------------------------------------------------------

    private List<GameSnapshot> loadSorted(Set<String> ids) {
        if (ids == null) return List.of();
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort((a, b) -> Long.compare(Long.parseLong(b), Long.parseLong(a)));
        return load(sorted);
    }

    private List<GameSnapshot> load(Collection<String> ids) {
        List<GameSnapshot> result = new ArrayList<>();
        if (ids == null) return result;
        for (String id : ids) {
            Map<Object, Object> h = redis.opsForHash().entries("snapshot:" + id);
            if (h.isEmpty()) continue;
            result.add(new GameSnapshot(
                    Long.valueOf(id),
                    Integer.parseInt((String) h.get("clockTime")),
                    h.get("gameTime") != null ? Integer.valueOf((String) h.get("gameTime")) : null,
                    h.get("daytime") != null ? Boolean.valueOf((String) h.get("daytime")) : null,
                    (String) h.get("winTeam"),
                    LocalDateTime.parse((String) h.get("capturedAt"))));
        }
        return result;
    }

    private static double toMillis(LocalDateTime t) {
        return t.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
