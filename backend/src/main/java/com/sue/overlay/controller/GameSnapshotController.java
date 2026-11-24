package com.sue.overlay.controller;

import com.sue.overlay.model.GameSnapshot;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/snapshots")
public class GameSnapshotController {

    private final GameSnapshotRepository repository;

    public GameSnapshotController(GameSnapshotRepository repository) {
        this.repository = repository;
    }

    /** GET /api/snapshots/latest?n=10 */
    @GetMapping("/latest")
    public List<GameSnapshot> latest(@RequestParam(defaultValue = "10") int n) {
        return repository.findLatest(n);
    }

    /** GET /api/snapshots/after/600 */
    @GetMapping("/after/{clockTime}")
    public List<GameSnapshot> after(@PathVariable int clockTime) {
        return repository.findAfterClockTime(clockTime);
    }

    /** GET /api/snapshots/between?from=...&to=... */
    @GetMapping("/between")
    public List<GameSnapshot> between(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return repository.findByCapturedAtBetween(from, to);
    }

    /** GET /api/snapshots/night — только ночные */
    @GetMapping("/night")
    public List<GameSnapshot> night() {
        return repository.findNight();
    }

    /**
     * GET /api/snapshots/stats — сводная статистика.
     * Используем LinkedHashMap вместо Map.of(), т.к. Map.of() не принимает null
     * (а MAX/AVG на пустой таблице возвращают null).
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", repository.count());
        result.put("maxClockTime", repository.findMaxClockTime());
        result.put("avgForRadiant", repository.findAverageClockTimeByWinTeam("radiant"));
        result.put("avgForDire", repository.findAverageClockTimeByWinTeam("dire"));
        return result;
    }

    /** GET /api/snapshots/by-team?team=radiant */
    @GetMapping("/by-team")
    public List<GameSnapshot> byTeamNative(@RequestParam String team) {
        return repository.findByWinTeam(team);
    }
}