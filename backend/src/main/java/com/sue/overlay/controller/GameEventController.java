package com.sue.overlay.controller;

import com.sue.overlay.persistence.entity.GameEvent;
import com.sue.overlay.repository.GameEventRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/events")
public class GameEventController {

    private final GameEventRepository repository;

    public GameEventController(GameEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/types")
    public List<String> types() {
        return repository.findAllEventTypes();
    }

    @GetMapping("/latest")
    public List<GameEvent> latest(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int n) {
        return repository.findLatest(PageRequest.of(0, n));
    }

    @GetMapping("/{type}")
    public List<GameEvent> byType(
            @PathVariable String type,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return repository.findByEventTypeOrderByGameTimeDesc(type, PageRequest.of(0, limit));
    }

    @GetMapping("/after/{gameTime}")
    public List<GameEvent> after(@PathVariable int gameTime) {
        return repository.findAfterGameTime(gameTime);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return repository.countByEventType().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }
}