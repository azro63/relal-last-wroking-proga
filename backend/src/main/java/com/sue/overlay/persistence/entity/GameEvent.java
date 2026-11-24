package com.sue.overlay.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "game_time", nullable = false)
    private int gameTime;

    // В реальном проекте лучше использовать @JdbcTypeCode(SqlTypes.JSON)
    // Для простоты храним как строку или мапу через конвертер
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    // --- Геттеры / Сеттеры ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getGameTime() { return gameTime; }
    public void setGameTime(int gameTime) { this.gameTime = gameTime; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}